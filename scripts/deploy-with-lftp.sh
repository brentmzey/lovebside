#!/bin/bash
set -e

# Check for lftp
if ! command -v lftp &> /dev/null; then
    echo "Error: lftp is not installed. Please run 'brew install lftp' first."
    exit 1
fi

# Check env vars
if [ -z "$POCKETHOST_FTP_USER" ] || [ -z "$POCKETHOST_FTP_PASSWORD" ]; then
    echo "Error: POCKETHOST_FTP_USER and POCKETHOST_FTP_PASSWORD environment variables must be set."
    exit 1
fi

HOST="ftp.pockethost.io"
LOCAL_DIR="pocketbase/migrations"
REMOTE_DIR="pb_migrations"

echo "Connecting to $HOST as $POCKETHOST_FTP_USER..."

# Connectivity tweaks:
# set net:timeout 10 - fail faster if hanging
# set ftp:ssl-protect-data false - often fixes hangs behind NATs while control channel is still encrypted
# set ftp:passive-mode true - force passive mode

lftp -u "$POCKETHOST_FTP_USER","$POCKETHOST_FTP_PASSWORD" $HOST <<EOF
set net:timeout 10
set ftp:ssl-force true
set ftp:ssl-protect-data false
set ssl:verify-certificate no
set ftp:passive-mode true
echo "--- Root Directory Listing ---"
ls
echo "------------------------------"
echo "Creating remote directory $REMOTE_DIR..."
mkdir -p "$REMOTE_DIR"
echo "Starting mirror..."
mirror -R -v --parallel=2 "$LOCAL_DIR" "$REMOTE_DIR"
bye
EOF

echo "Migration upload complete."
echo "Please restart your Pockethost instance to apply changes."
