export function formatDate(date: Date): string {
  return date.toISOString().replace('T', ' ').slice(0, 19);
}

export function formatMigrationName(filename: string): string {
  const match = filename.match(/^\d+_(.+)\.js$/);
  return match ? match[1].replace(/_/g, ' ') : filename;
}

export function validateMigrationName(name: string): boolean {
  return /^[a-zA-Z0-9_\s-]+$/.test(name);
}
