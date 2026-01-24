#!/usr/bin/env ts-node
/**
 * Schema Comparison & Validation Tool
 * 
 * Compares PocketBase schema against canonical production snapshot
 * Usage: ts-node pocketbase/migrations-manager/schema-compare.ts [snapshot-file]
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const POCKETBASE_URL = process.env.POCKETBASE_URL || 'http://127.0.0.1:8091';
const ADMIN_EMAIL = process.env.POCKETBASE_ADMIN_EMAIL || 'tester_admin@bside.love';
const ADMIN_PASSWORD = process.env.POCKETBASE_ADMIN_PASSWORD || 'password123';

interface Field {
    name: string;
    type: string;
    required: boolean;
    unique: boolean;
    options?: unknown;
    [key: string]: unknown;
}

interface Collection {
    id: string;
    name: string;
    type: string;
    fields: Field[];
    indexes?: string[];
    listRule?: string | null;
    viewRule?: string | null;
    createRule?: string | null;
    updateRule?: string | null;
    deleteRule?: string | null;
    [key: string]: unknown;
}

interface SchemaDiff {
    missing: string[];
    extra: string[];
    different: { name: string; differences: string[] }[];
}

async function authenticate(): Promise<string> {
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
    return data.token;
}

async function getCurrentSchema(token: string): Promise<Collection[]> {
    const response = await fetch(`${POCKETBASE_URL}/api/collections?perPage=500`, {
        headers: { 'Authorization': token }
    });

    if (!response.ok) {
        throw new Error(`Failed to fetch current schema: ${response.statusText}`);
    }

    const data = await response.json() as { items: Collection[] };
    return data.items || [];
}

function compareCollections(expected: Collection, actual: Collection): string[] {
    const differences: string[] = [];

    // Compare type
    if (expected.type !== actual.type) {
        differences.push(`Type: expected "${expected.type}", got "${actual.type}"`);
    }

    // Compare fields count
    if (expected.fields.length !== actual.fields.length) {
        differences.push(`Field count: expected ${expected.fields.length}, got ${actual.fields.length}`);
    }

    // Compare each field
    for (const expectedField of expected.fields) {
        const actualField = actual.fields.find(f => f.name === expectedField.name);
        if (!actualField) {
            differences.push(`Missing field: "${expectedField.name}"`);
            continue;
        }

        if (expectedField.type !== actualField.type) {
            differences.push(`Field "${expectedField.name}": type "${expectedField.type}" vs "${actualField.type}"`);
        }
    }

    // Check for extra fields
    for (const actualField of actual.fields) {
        const expectedField = expected.fields.find(f => f.name === actualField.name);
        if (!expectedField) {
            differences.push(`Extra field: "${actualField.name}"`);
        }
    }

    // Compare indices
    const expectedIndices = new Set(expected.indexes || []);
    const actualIndices = new Set(actual.indexes || []);

    for (const idx of expectedIndices) {
        if (!actualIndices.has(idx)) {
            differences.push(`Missing index: ${idx}`);
        }
    }

    return differences;
}

async function compareSchemas(snapshotPath: string): Promise<SchemaDiff> {
    console.log('Authenticating...');
    const token = await authenticate();

    console.log('Fetching current schema...');
    const current = await getCurrentSchema(token);

    console.log('Loading snapshot...');
    const snapshotData = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));

    // Filter out system collections
    const systemCollections = ['_superusers', '_authOrigins', '_externalAuths', '_mfas', '_otps'];
    const expected = snapshotData.filter((c: Collection) => !systemCollections.includes(c.name));
    const currentFiltered = current.filter(c => !systemCollections.includes(c.name));

    const currentNames = new Set(currentFiltered.map(c => c.name));
    const expectedNames = new Set(expected.map((c: Collection) => c.name));

    const missing = expected
        .filter((c: Collection) => !currentNames.has(c.name))
        .map((c: Collection) => c.name);

    const extra = currentFiltered
        .filter(c => !expectedNames.has(c.name))
        .map(c => c.name);

    const different: { name: string; differences: string[] }[] = [];

    for (const expectedCol of expected) {
        const actualCol = currentFiltered.find(c => c.name === expectedCol.name);
        if (actualCol) {
            const diffs = compareCollections(expectedCol, actualCol);
            if (diffs.length > 0) {
                different.push({ name: expectedCol.name, differences: diffs });
            }
        }
    }

    return { missing, extra, different };
}

async function exportCurrentSchema(outputPath?: string): Promise<void> {
    console.log('Authenticating...');
    const token = await authenticate();

    console.log('Fetching current schema...');
    const schema = await getCurrentSchema(token);

    const output = outputPath || path.join(__dirname, '..', 'schemas_archive', `local_${new Date().toISOString().split('T')[0]}.json`);

    fs.writeFileSync(output, JSON.stringify(schema, null, 2));
    console.log(`✓ Schema exported to: ${output}`);
}

async function validateSchema(snapshotPath?: string): Promise<void> {
    const snapshot = snapshotPath || path.join(__dirname, '..', 'schemas_archive', 'prod_snapshot_jan_2025.json');

    if (!fs.existsSync(snapshot)) {
        console.error(`❌ Snapshot not found: ${snapshot}`);
        process.exit(1);
    }

    console.log(`\n📊 Validating schema against: ${path.basename(snapshot)}\n`);

    const diff = await compareSchemas(snapshot);

    let hasIssues = false;

    if (diff.missing.length > 0) {
        hasIssues = true;
        console.log('❌ Missing collections:');
        diff.missing.forEach(name => console.log(`   - ${name}`));
        console.log('');
    }

    if (diff.extra.length > 0) {
        hasIssues = true;
        console.log('⚠️  Extra collections (not in snapshot):');
        diff.extra.forEach(name => console.log(`   - ${name}`));
        console.log('');
    }

    if (diff.different.length > 0) {
        hasIssues = true;
        console.log('⚠️  Schema differences:');
        diff.different.forEach(({ name, differences }) => {
            console.log(`   ${name}:`);
            differences.forEach(d => console.log(`      - ${d}`));
        });
        console.log('');
    }

    if (!hasIssues) {
        console.log('✅ Schema matches snapshot perfectly!\n');
        return;
    }

    console.log('💡 To fix schema differences, run:');
    console.log('   npm run migrate:up\n');
}

// CLI
const command = process.argv[2];
const arg = process.argv[3];

(async () => {
    try {
        switch (command) {
            case 'export':
                await exportCurrentSchema(arg);
                break;
            case 'diff':
                if (!arg) {
                    console.error('Usage: schema-compare.ts diff <snapshot-file>');
                    process.exit(1);
                }
                await validateSchema(arg);
                break;
            case 'validate':
                await validateSchema(arg);
                break;
            default:
                console.log(`
Schema Comparison Tool

Usage:
  ts-node schema-compare.ts export [output-file]
    Export current schema to file

  ts-node schema-compare.ts diff <snapshot-file>
    Compare current schema with snapshot

  ts-node schema-compare.ts validate [snapshot-file]
    Validate current schema (defaults to prod snapshot)
`);
        }
    } catch (error) {
        console.error('Error:', error);
        process.exit(1);
    }
})();
