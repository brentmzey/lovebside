import * as dotenv from 'dotenv';
import { Config, ConfigSchema } from '../types/index.js';
import { getAWSSecrets } from './aws-secrets.js';
import { getMicroconfigSecrets } from './microconfig.js';

dotenv.config();

export async function loadConfig(): Promise<Config> {
  const secretsProvider = process.env.SECRETS_PROVIDER || 'env';

  let pocketbaseConfig = {
    url: process.env.POCKETBASE_URL || '',
    email: process.env.POCKETBASE_ADMIN_EMAIL || '',
    password: process.env.POCKETBASE_ADMIN_PASSWORD || '',
  };

  if (secretsProvider === 'aws' && process.env.AWS_SECRET_NAME) {
    const awsSecrets = await getAWSSecrets(
      process.env.AWS_SECRET_NAME,
      process.env.AWS_REGION || 'us-east-1'
    );
    pocketbaseConfig = {
      url: awsSecrets.POCKETBASE_URL || pocketbaseConfig.url,
      email: awsSecrets.POCKETBASE_ADMIN_EMAIL || pocketbaseConfig.email,
      password: awsSecrets.POCKETBASE_ADMIN_PASSWORD || pocketbaseConfig.password,
    };
  } else if (secretsProvider === 'microconfig' && process.env.MICROCONFIG_URL) {
    const microconfigSecrets = await getMicroconfigSecrets(
      process.env.MICROCONFIG_URL,
      process.env.MICROCONFIG_TOKEN
    );
    pocketbaseConfig = {
      url: microconfigSecrets.POCKETBASE_URL || pocketbaseConfig.url,
      email: microconfigSecrets.POCKETBASE_ADMIN_EMAIL || pocketbaseConfig.email,
      password: microconfigSecrets.POCKETBASE_ADMIN_PASSWORD || pocketbaseConfig.password,
    };
  }

  const config: Config = {
    pocketbase: pocketbaseConfig,
    migrations: {
      dir: process.env.MIGRATIONS_DIR || '../pocketbase/migrations',
      table: process.env.MIGRATION_TABLE || 'pb_migrations',
    },
    secrets: {
      provider: secretsProvider as 'env' | 'aws' | 'microconfig',
      aws: {
        region: process.env.AWS_REGION,
        secretName: process.env.AWS_SECRET_NAME,
      },
      microconfig: {
        url: process.env.MICROCONFIG_URL,
        token: process.env.MICROCONFIG_TOKEN,
      },
    },
  };

  return ConfigSchema.parse(config);
}
