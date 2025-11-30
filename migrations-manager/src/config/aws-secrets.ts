import AWS from 'aws-sdk';

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
  const client = new AWS.SecretsManager({ region });

  try {
    const data = await client.getSecretValue({ SecretId: secretName }).promise();
    
    if (data.SecretString) {
      return JSON.parse(data.SecretString);
    }
    
    throw new Error('Secret string not found in AWS Secrets Manager response');
  } catch (error) {
    console.error('Error retrieving secrets from AWS:', error);
    throw error;
  }
}
