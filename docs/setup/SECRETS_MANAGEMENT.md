# Secrets Management & Environment Configuration

## Overview
B-Side uses a `.env` file to manage environment-specific configuration and secrets. This file is **not tracked by git** to prevent sensitive information from being exposed.

## Local Development
For local development, a `.env` file is automatically created from `.env.example` when you run `just start` (or can be copied manually).

**Default Local `.env`:**
```dotenv
# PocketBase
PB_PUBLIC_URL=http://localhost:8090
POCKETBASE_ADMIN_EMAIL=tester_admin@bside.love
POCKETBASE_ADMIN_PASSWORD=password123

# CDN Configuration (AWS CloudFront + S3)
CDN_ENABLED=false
...
```

The default credentials (`tester_admin@bside.love` / `password123`) are safe for local Docker-based development.

## Production & Secrets
For production environments or to access external services (like AWS S3 for CDN), you will need real API keys.

**🚫 Do not commit real secrets to the repository.**

### How to obtain secrets:
1.  **Ask an Admin:** Contact the project administrator or DevOps lead to obtain the necessary API keys and secrets for your specific environment.
2.  **SSO/Auth:** If the project uses Single Sign-On (SSO) for development services, follow the internal onboarding guide.
3.  **Update Local `.env`:** Once you have the secrets, update your local `.env` file. Since this file is ignored by git, your secrets will remain safe on your machine.

### Adding New Secrets
If you add a new environment variable to the code:
1.  Add it to `.env` for your local use.
2.  Add it to `.env.example` with a blank or dummy value so other developers know it's required.
3.  Document it in this file if special instructions are needed.

## Troubleshooting
*   **Missing `.env`:** Run `cp .env.example .env` to restore defaults.
*   **"Variable not set" warnings:** Check that your `.env` file contains all keys listed in `.env.example`.
