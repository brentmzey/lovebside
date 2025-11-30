import fetch from 'node-fetch';

interface MicroconfigSecrets {
  POCKETBASE_URL?: string;
  POCKETBASE_ADMIN_EMAIL?: string;
  POCKETBASE_ADMIN_PASSWORD?: string;
  [key: string]: string | undefined;
}

export async function getMicroconfigSecrets(
  url: string,
  token?: string
): Promise<MicroconfigSecrets> {
  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${url}/secrets/pocketbase`, {
      method: 'GET',
      headers,
    });

    if (!response.ok) {
      throw new Error(`Microconfig server responded with status: ${response.status}`);
    }

    const data = await response.json();
    return data as MicroconfigSecrets;
  } catch (error) {
    console.error('Error retrieving secrets from microconfig:', error);
    throw error;
  }
}
