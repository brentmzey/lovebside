import { z } from 'zod';

export const ConfigSchema = z.object({
  pocketbase: z.object({
    url: z.string().url(),
    email: z.string().email(),
    password: z.string().min(8),
  }),
  migrations: z.object({
    dir: z.string(),
    table: z.string()
      .default('pb_migrations')
      .refine((value) => !value.startsWith('_'), {
        message: 'MIGRATION_TABLE cannot start with an underscore; PocketBase reserves names like _migrations. Use pb_migrations or another custom name.',
      }),
  }),
  secrets: z.object({
    provider: z.enum(['env', 'aws', 'microconfig']).default('env'),
    aws: z.object({
      region: z.string().optional(),
      secretName: z.string().optional(),
    }).optional(),
    microconfig: z.object({
      url: z.string().url().optional(),
      token: z.string().optional(),
    }).optional(),
  }).optional(),
});

export type Config = z.infer<typeof ConfigSchema>;

export interface Migration {
  id: string;
  name: string;
  applied: boolean;
  appliedAt?: Date;
  file: string;
}

export interface MigrationFile {
  filename: string;
  timestamp: string;
  name: string;
  path: string;
}

export interface MigrationResult {
  success: boolean;
  migration: string;
  message?: string;
  error?: Error;
}
