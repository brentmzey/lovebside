import { describe, it, expect, beforeEach, vi } from 'vitest';
import { MigrationManager } from '../services/migration-manager.js';
import { PocketBaseClient } from '../services/pocketbase.js';
import type { Config } from '../types/index.js';

describe('MigrationManager', () => {
  let config: Config;
  let client: PocketBaseClient;
  let manager: MigrationManager;

  beforeEach(() => {
    config = {
      pocketbase: {
        url: 'https://test.pockethost.io',
        email: 'test@example.com',
        password: 'password123',
      },
      migrations: {
        dir: './test-migrations',
        table: '_migrations',
      },
    };

    client = new PocketBaseClient(config);
    manager = new MigrationManager(config, client);
  });

  it('should initialize correctly', () => {
    expect(manager).toBeDefined();
  });

  it('should validate migration names correctly', () => {
    const { validateMigrationName } = require('../utils/format.js');
    
    expect(validateMigrationName('add_users_table')).toBe(true);
    expect(validateMigrationName('add users table')).toBe(true);
    expect(validateMigrationName('add-users-table')).toBe(true);
    expect(validateMigrationName('add@users#table')).toBe(false);
  });
});
