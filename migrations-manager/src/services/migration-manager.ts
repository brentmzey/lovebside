import { promises as fs } from 'fs';
import path from 'path';
import { Config, Migration, MigrationFile, MigrationResult } from '../types/index.js';
import { PocketBaseClient } from './pocketbase.js';

export class MigrationManager {
  private config: Config;
  private client: PocketBaseClient;

  constructor(config: Config, client: PocketBaseClient) {
    this.config = config;
    this.client = client;
  }

  async getMigrationFiles(): Promise<MigrationFile[]> {
    const migrationsDir = path.resolve(process.cwd(), this.config.migrations.dir);
    
    try {
      const files = await fs.readdir(migrationsDir);
      const migrationFiles = files
        .filter((f) => f.endsWith('.js'))
        .sort()
        .map((filename) => {
          const match = filename.match(/^(\d+)_(.+)\.js$/);
          if (!match) {
            throw new Error(`Invalid migration filename: ${filename}`);
          }
          return {
            filename,
            timestamp: match[1],
            name: match[2],
            path: path.join(migrationsDir, filename),
          };
        });

      return migrationFiles;
    } catch (error) {
      throw new Error(`Failed to read migrations directory: ${error}`);
    }
  }

  async getMigrations(): Promise<Migration[]> {
    const files = await this.getMigrationFiles();
    const applied = await this.client.getAppliedMigrations();

    return files.map((file) => ({
      id: file.timestamp,
      name: file.name,
      applied: applied.includes(file.filename),
      file: file.filename,
    }));
  }

  async runPendingMigrations(): Promise<MigrationResult[]> {
    const migrations = await this.getMigrations();
    const pending = migrations.filter((m) => !m.applied);

    if (pending.length === 0) {
      return [];
    }

    const results: MigrationResult[] = [];
    const batch = await this.getNextBatch();

    for (const migration of pending) {
      try {
        await this.runMigration(migration, batch);
        results.push({
          success: true,
          migration: migration.file,
          message: 'Migration applied successfully',
        });
      } catch (error) {
        results.push({
          success: false,
          migration: migration.file,
          error: error as Error,
        });
        break;
      }
    }

    return results;
  }

  async runMigration(migration: Migration, batch: number): Promise<void> {
    const migrationPath = path.resolve(
      process.cwd(),
      this.config.migrations.dir,
      migration.file
    );

    const code = await fs.readFile(migrationPath, 'utf-8');
    
    await this.client.executeMigrationCode(code);
    await this.client.recordMigration(migration.file, batch);
  }

  async rollbackLastBatch(): Promise<MigrationResult[]> {
    const lastBatch = await this.getLastBatch();
    if (lastBatch === null) {
      return [];
    }

    const migrations = await this.getMigrations();
    const toRollback = migrations
      .filter((m) => m.applied)
      .reverse();

    const results: MigrationResult[] = [];

    for (const migration of toRollback) {
      try {
        await this.client.removeMigration(migration.file);
        results.push({
          success: true,
          migration: migration.file,
          message: 'Migration rolled back',
        });
      } catch (error) {
        results.push({
          success: false,
          migration: migration.file,
          error: error as Error,
        });
        break;
      }
    }

    return results;
  }

  async createMigration(name: string): Promise<string> {
    const timestamp = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14);
    const filename = `${timestamp}_${name.toLowerCase().replace(/\s+/g, '_')}.js`;
    const filepath = path.resolve(process.cwd(), this.config.migrations.dir, filename);

    const template = `/// <reference path="../pb_data/types.d.ts" />
migrate((db) => {
  // Add your migration code here
  // Example: Creating a collection
  
  // const collection = new Collection({
  //   name: "example",
  //   type: "base",
  //   schema: [
  //     {
  //       name: "field_name",
  //       type: "text",
  //       required: true,
  //     },
  //   ],
  // });
  
  // return db.saveCollection(collection);
}, (db) => {
  // Add your rollback code here (optional)
  
  // const collection = db.findCollectionByNameOrId("example");
  // return db.deleteCollection(collection);
});
`;

    await fs.writeFile(filepath, template, 'utf-8');
    return filename;
  }

  private async getNextBatch(): Promise<number> {
    const records = await this.client
      .getClient()
      .collection(this.config.migrations.table)
      .getFullList({
        sort: '-batch',
        limit: 1,
      });

    return records.length > 0 ? (records[0].batch as number) + 1 : 1;
  }

  private async getLastBatch(): Promise<number | null> {
    const records = await this.client
      .getClient()
      .collection(this.config.migrations.table)
      .getFullList({
        sort: '-batch',
        limit: 1,
      });

    return records.length > 0 ? (records[0].batch as number) : null;
  }
}
