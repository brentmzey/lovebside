import PocketBase from 'pocketbase';
import { Config } from '../types/index.js';

export class PocketBaseClient {
  private client: PocketBase;
  private config: Config;

  constructor(config: Config) {
    this.config = config;
    this.client = new PocketBase(config.pocketbase.url);
  }

  async authenticate(): Promise<void> {
    try {
      await this.client.collection('_superusers').authWithPassword(
        this.config.pocketbase.email,
        this.config.pocketbase.password
      );
    } catch (error) {
      throw new Error(`Failed to authenticate with PocketBase: ${error}`);
    }
  }

  async ensureMigrationsCollection(): Promise<void> {
    try {
      await this.client.collections.getOne(this.config.migrations.table);
    } catch {
      await this.client.collections.create({
        name: this.config.migrations.table,
        type: 'base',
        schema: [
          {
            name: 'migration',
            type: 'text',
            required: true,
            options: { min: 1, max: 255 },
          },
          {
            name: 'applied_at',
            type: 'date',
            required: true,
          },
          {
            name: 'batch',
            type: 'number',
            required: true,
          },
        ],
        indexes: [
          'CREATE UNIQUE INDEX idx_migration ON ' + this.config.migrations.table + ' (migration)',
        ],
      });
    }
  }

  async getAppliedMigrations(): Promise<string[]> {
    try {
      const records = await this.client.collection(this.config.migrations.table).getFullList({
        sort: 'applied_at',
      });
      return records.map((r) => r.migration as string);
    } catch {
      return [];
    }
  }

  async recordMigration(migration: string, batch: number): Promise<void> {
    await this.client.collection(this.config.migrations.table).create({
      migration,
      applied_at: new Date().toISOString(),
      batch,
    });
  }

  async removeMigration(migration: string): Promise<void> {
    const records = await this.client.collection(this.config.migrations.table).getFullList({
      filter: `migration = "${migration}"`,
    });
    
    if (records.length > 0) {
      await this.client.collection(this.config.migrations.table).delete(records[0].id);
    }
  }

  async executeMigrationCode(code: string): Promise<void> {
    const AsyncFunction = Object.getPrototypeOf(async function(){}).constructor;
    const migrationFunc = new AsyncFunction('migrate', code);
    
    await migrationFunc((db: PocketBase) => {
      return db;
    })(this.client);
  }

  getClient(): PocketBase {
    return this.client;
  }
}
