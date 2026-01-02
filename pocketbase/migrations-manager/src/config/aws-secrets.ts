import { SecretsManagerClient, GetSecretValueCommand } from '@aws-sdk/client-secrets-manager';

interface AWSSecrets {
  POCKETBASE_URL?: string;
  POCKETBASE_ADMIN_EMAIL?: string;
  POCKETBASE_ADMIN_PASSWORD?: string;
  [key: string]: string | undefined;
}

export async function getAWSSecrets(
  secretName: string,
  region: string = 'us-east-1'
): Promise<AWSSecrets> {
  const client = new SecretsManagerClient({ region });

  try {
    const command = new GetSecretValueCommand({ SecretId: secretName });
    const data = await client.send(command);

    if (data.SecretString) {
      return JSON.parse(data.SecretString);
    }

    throw new Error('Secret string not found in AWS Secrets Manager response');
  } catch (error) {
    console.error('Error retrieving secrets from AWS:', error);
    throw error;
  }
}
