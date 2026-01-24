FROM ghcr.io/muchobien/pocketbase:latest

# Copy local migrations to the image to ensure schema reproducibility
COPY ./pb_migrations /pb_migrations

# Copy hooks (if/when enabled)
COPY ./pb_hooks /pb_hooks

# Copy public directory if serving static frontend
COPY ./pb_public /pb_public

# Copy entrypoint script
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 8090

# Use entrypoint for auto-admin creation
ENTRYPOINT ["/usr/local/bin/entrypoint.sh", "--dir=/pb_data", "--publicDir=/pb_public", "--migrationsDir=/pb_migrations", "--hooksDir=/pb_hooks"]
