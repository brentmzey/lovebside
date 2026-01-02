#!/usr/bin/env node

import { Command } from 'commander';
import chalk from 'chalk';
import ora from 'ora';
import inquirer from 'inquirer';
import { loadConfig } from './config/index.js';
import { PocketBaseClient } from './services/pocketbase.js';
import { MigrationManager } from './services/migration-manager.js';
import { formatMigrationName, validateMigrationName } from './utils/format.js';

const program = new Command();

program
  .name('pb-migrate')
  .description('PocketBase migration management CLI')
  .version('1.0.0');

program
  .command('migrate')
  .description('Run all pending migrations')
  .action(async () => {
    const spinner = ora('Loading configuration...').start();
    
    try {
      const config = await loadConfig();
      spinner.text = 'Connecting to PocketBase...';
      
      const client = new PocketBaseClient(config);
      await client.authenticate();
      await client.ensureMigrationsCollection();
      
      spinner.text = 'Running migrations...';
      const manager = new MigrationManager(config, client);
      const results = await manager.runPendingMigrations();
      
      spinner.stop();
      
      if (results.length === 0) {
        console.log(chalk.yellow('No pending migrations'));
        return;
      }
      
      console.log(chalk.green('\n✓ Migrations completed:\n'));
      results.forEach((result) => {
        if (result.success) {
          console.log(chalk.green(`  ✓ ${formatMigrationName(result.migration)}`));
        } else {
          console.log(chalk.red(`  ✗ ${formatMigrationName(result.migration)}`));
          console.log(chalk.red(`    ${result.error?.message}`));
        }
      });
    } catch (error) {
      spinner.fail('Migration failed');
      console.error(chalk.red((error as Error).message));
      process.exit(1);
    }
  });

program
  .command('migrate:status')
  .description('Show migration status')
  .action(async () => {
    const spinner = ora('Loading migration status...').start();
    
    try {
      const config = await loadConfig();
      const client = new PocketBaseClient(config);
      await client.authenticate();
      await client.ensureMigrationsCollection();
      
      const manager = new MigrationManager(config, client);
      const migrations = await manager.getMigrations();
      
      spinner.stop();
      
      console.log(chalk.bold('\nMigration Status:\n'));
      
      migrations.forEach((migration) => {
        const status = migration.applied
          ? chalk.green('✓ Applied')
          : chalk.yellow('○ Pending');
        console.log(`${status}  ${formatMigrationName(migration.file)}`);
      });
      
      const pending = migrations.filter((m) => !m.applied).length;
      const applied = migrations.filter((m) => m.applied).length;
      
      console.log(
        chalk.dim(`\nTotal: ${migrations.length} | Applied: ${applied} | Pending: ${pending}\n`)
      );
    } catch (error) {
      spinner.fail('Failed to get status');
      console.error(chalk.red((error as Error).message));
      process.exit(1);
    }
  });

program
  .command('migrate:up')
  .description('Run next pending migration')
  .action(async () => {
    const spinner = ora('Running next migration...').start();
    
    try {
      const config = await loadConfig();
      const client = new PocketBaseClient(config);
      await client.authenticate();
      await client.ensureMigrationsCollection();
      
      const manager = new MigrationManager(config, client);
      const migrations = await manager.getMigrations();
      const pending = migrations.find((m) => !m.applied);
      
      if (!pending) {
        spinner.info('No pending migrations');
        return;
      }
      
      const batch = await (manager as any).getNextBatch();
      await manager.runMigration(pending, batch);
      
      spinner.succeed(`Migration applied: ${formatMigrationName(pending.file)}`);
    } catch (error) {
      spinner.fail('Migration failed');
      console.error(chalk.red((error as Error).message));
      process.exit(1);
    }
  });

program
  .command('migrate:down')
  .description('Rollback last batch of migrations')
  .action(async () => {
    const answers = await inquirer.prompt([
      {
        type: 'confirm',
        name: 'confirm',
        message: 'Are you sure you want to rollback the last batch of migrations?',
        default: false,
      },
    ]);
    
    if (!answers.confirm) {
      console.log(chalk.yellow('Rollback cancelled'));
      return;
    }
    
    const spinner = ora('Rolling back migrations...').start();
    
    try {
      const config = await loadConfig();
      const client = new PocketBaseClient(config);
      await client.authenticate();
      
      const manager = new MigrationManager(config, client);
      const results = await manager.rollbackLastBatch();
      
      spinner.stop();
      
      if (results.length === 0) {
        console.log(chalk.yellow('No migrations to rollback'));
        return;
      }
      
      console.log(chalk.green('\n✓ Rollback completed:\n'));
      results.forEach((result) => {
        if (result.success) {
          console.log(chalk.green(`  ✓ ${formatMigrationName(result.migration)}`));
        } else {
          console.log(chalk.red(`  ✗ ${formatMigrationName(result.migration)}`));
          console.log(chalk.red(`    ${result.error?.message}`));
        }
      });
    } catch (error) {
      spinner.fail('Rollback failed');
      console.error(chalk.red((error as Error).message));
      process.exit(1);
    }
  });

program
  .command('migrate:create <name>')
  .description('Create a new migration file')
  .action(async (name: string) => {
    if (!validateMigrationName(name)) {
      console.error(
        chalk.red('Invalid migration name. Use only letters, numbers, spaces, hyphens, and underscores.')
      );
      process.exit(1);
    }
    
    const spinner = ora('Creating migration...').start();
    
    try {
      const config = await loadConfig();
      const client = new PocketBaseClient(config);
      const manager = new MigrationManager(config, client);
      
      const filename = await manager.createMigration(name);
      
      spinner.succeed(`Migration created: ${filename}`);
    } catch (error) {
      spinner.fail('Failed to create migration');
      console.error(chalk.red((error as Error).message));
      process.exit(1);
    }
  });

program.parse();
