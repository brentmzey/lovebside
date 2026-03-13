#!/usr/bin/env ts-node
/**
 * Idempotent Database Migration Runner for PocketBase
 * 
 * Features:
 * - Checksum-based migration tracking
 * - Safe to run multiple times (idempotent)
 * - Works on fresh DB or existing production DB
 * - Environment-agnostic (dev, staging, prod)
 */

import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const POCKETBASE_URL = process.env.POCKETBASE_URL || 'http://127.0.0.1:8092';
const ADMIN_EMAIL = process.env.POCKETBASE_ADMIN_EMAIL || 'tester_admin@bside.love';
const ADMIN_PASSWORD = process.env.POCKETBASE_ADMIN_PASSWORD || 'password123';

interface Migration {
    version: string;
    name: string;
    path: string;
    checksum: string;
}

interface MigrationRecord {
    id?: string;
    migration: string;
    applied_at: string;
    batch: number;
    checksum: string;
}

class MigrationRunner {
    private token: string = '';
    private migrationsDir: string;

    constructor() {
        this.migrationsDir = path.join(__dirname, '..', 'migrations');
    }

    async run(command: string) {
        try {
            await this.authenticate();

            switch (command) {
                case 'up':
                    await this.migrateUp();
                    break;
                case 'down':
                    await this.migrateDown();
                    break;
                case 'status':
                    await this.showStatus();
                    break;
                case 'create':
                    await this.createMigration(process.argv[3]);
                    break;
                default:
                    console.log('Usage: ts-node migrate.ts [up|down|status|create <name>]');
            }
        } catch (error) {
            console.error('Migration failed:', error);
            process.exit(1);
        }
    }

    private async authenticate() {
        const response = await fetch(`${POCKETBASE_URL}/api/collections/_superusers/auth-with-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                identity: ADMIN_EMAIL,
                password: ADMIN_PASSWORD
            })
        });

        if (!response.ok) {
            throw new Error(`Authentication failed: ${response.statusText}`);
        }

        const data = await response.json() as { token: string };
        this.token = data.token;
        console.log('✓ Authenticated as admin');
    }

    private async ensureMigrationsTable() {
        // Check if pb_migrations collection exists
        const checkResponse = await fetch(`${POCKETBASE_URL}/api/collections/pb_migrations`, {
            headers: { 'Authorization': this.token }
        });

        if (checkResponse.status === 404) {
            console.log('Creating pb_migrations table...');

            const createResponse = await fetch(`${POCKETBASE_URL}/api/collections`, {
                method: 'POST',
                headers: {
                    'Authorization': this.token,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    name: 'pb_migrations',
                    type: 'base',
                    fields: [
                        {
                            name: 'migration',
                            type: 'text',
                            required: true
                        },
                        {
                            name: 'applied_at',
                            type: 'date',
                            required: true
                        },
                        {
                            name: 'batch',
                            type: 'number',
                            required: true
                        },
                        {
                            name: 'checksum',
                            type: 'text',
                            required: true
                        }
                    ]
                })
            });

            if (!createResponse.ok) {
                throw new Error('Failed to create migrations table');
            }
            console.log('✓ Created pb_migrations table');
        }
    }

    private async getAppliedMigrations(): Promise<MigrationRecord[]> {
        const response = await fetch(`${POCKETBASE_URL}/api/collections/pb_migrations/records?perPage=500`, {
            headers: { 'Authorization': this.token }
        });

        if (!response.ok) {
            return [];
        }

        const data = await response.json() as { items: MigrationRecord[] };
        return data.items || [];
    }

    private async getPendingMigrations(): Promise<Migration[]> {
        const applied = await this.getAppliedMigrations();
        const appliedNames = new Set(applied.map(m => m.migration));

        if (!fs.existsSync(this.migrationsDir)) {
            fs.mkdirSync(this.migrationsDir, { recursive: true });
            return [];
        }

        const files = fs.readdirSync(this.migrationsDir)
            .filter(f => f.endsWith('.js') || f.endsWith('.ts'))
            .sort();

        const pending: Migration[] = [];

        for (const file of files) {
            const version = file.split('_')[0];
            const name = file.replace(/\.(js|ts)$/, '');

            if (!appliedNames.has(name)) {
                const filePath = path.join(this.migrationsDir, file);
                const content = fs.readFileSync(filePath, 'utf8');
                const checksum = crypto.createHash('md5').update(content).digest('hex');

                pending.push({ version, name, path: filePath, checksum });
            }
        }

        return pending;
    }

    private async migrateUp() {
        console.log('Running migrations...\n');

        await this.ensureMigrationsTable();
        const pending = await this.getPendingMigrations();

        if (pending.length === 0) {
            console.log('✓ No pending migrations');
            return;
        }

        const applied = await this.getAppliedMigrations();
        const nextBatch = applied.length > 0
            ? Math.max(...applied.map(m => m.batch)) + 1
            : 1;

        for (const migration of pending) {
            console.log(`Applying: ${migration.name}...`);

            try {
                // Execute migration
                const migrationFn = await import(migration.path);
                if (typeof migrationFn.up === 'function') {
                    await migrationFn.up(POCKETBASE_URL, this.token);
                }

                // Record migration
                await fetch(`${POCKETBASE_URL}/api/collections/pb_migrations/records`, {
                    method: 'POST',
                    headers: {
                        'Authorization': this.token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        migration: migration.name,
                        applied_at: new Date().toISOString(),
                        batch: nextBatch,
                        checksum: migration.checksum
                    })
                });

                console.log(`✓ Applied: ${migration.name}`);
            } catch (error) {
                console.error(`✗ Failed: ${migration.name}`, error);
                throw error;
            }
        }

        console.log(`\n✓ Applied ${pending.length} migration(s)`);
    }

    private async migrateDown() {
        console.log('Rolling back last batch...\n');

        const applied = await this.getAppliedMigrations();
        if (applied.length === 0) {
            console.log('✓ No migrations to rollback');
            return;
        }

        const lastBatch = Math.max(...applied.map(m => m.batch));
        const toRollback = applied
            .filter(m => m.batch === lastBatch)
            .reverse();

        for (const record of toRollback) {
            console.log(`Rolling back: ${record.migration}...`);

            try {
                const migrationPath = path.join(this.migrationsDir, `${record.migration}.ts`);
                const migrationFn = await import(migrationPath);

                if (typeof migrationFn.down === 'function') {
                    await migrationFn.down(POCKETBASE_URL, this.token);
                }

                // Delete migration record
                await fetch(`${POCKETBASE_URL}/api/collections/pb_migrations/records/${record.id}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': this.token }
                });

                console.log(`✓ Rolled back: ${record.migration}`);
            } catch (error) {
                console.error(`✗ Failed to rollback: ${record.migration}`, error);
                throw error;
            }
        }

        console.log(`\n✓ Rolled back ${toRollback.length} migration(s)`);
    }

    private async showStatus() {
        await this.ensureMigrationsTable();

        const applied = await this.getAppliedMigrations();
        const pending = await this.getPendingMigrations();

        console.log('\n=== Migration Status ===\n');
        console.log(`Applied: ${applied.length}`);
        console.log(`Pending: ${pending.length}\n`);

        if (applied.length > 0) {
            console.log('Applied Migrations:');
            applied.forEach(m => {
                console.log(`  ✓ ${m.migration} (batch ${m.batch})`);
            });
        }

        if (pending.length > 0) {
            console.log('\nPending Migrations:');
            pending.forEach(m => {
                console.log(`  ⋯ ${m.name}`);
            });
        }

        console.log('');
    }

    private async createMigration(name: string) {
        if (!name) {
            console.error('Please provide a migration name');
            process.exit(1);
        }

        const timestamp = new Date().toISOString().replace(/[-:]/g, '').split('.')[0].replace('T', '_');
        const filename = `${timestamp}_${name.replace(/\s+/g, '_')}.ts`;
        const filepath = path.join(this.migrationsDir, filename);

        const template = `/**
 * Migration: ${name}
 * Created: ${new Date().toISOString()}
 */

export async function up(pbUrl: string, token: string) {
  // TODO: Implement migration
  console.log('Running migration: ${name}');
}

export async function down(pbUrl: string, token: string) {
  // TODO: Implement rollback
  console.log('Rolling back migration: ${name}');
}
`;

        fs.writeFileSync(filepath, template);
        console.log(`✓ Created migration: ${filename}`);
    }
}

// Run CLI
const runner = new MigrationRunner();
const command = process.argv[2] || 'status';
runner.run(command);
