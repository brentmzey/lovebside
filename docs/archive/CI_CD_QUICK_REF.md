# 🎯 Quick Reference - CI/CD Commands

## Push and Trigger CI/CD

```bash
# Commit and push changes
git add .
git commit -m "Your commit message"
git push origin development

# Create a pull request (triggers PR check)
gh pr create --title "My Feature" --body "Description"

# Create a release (triggers release build)
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
gh release create v1.0.0 --generate-notes
```

## View Build Status

```bash
# List recent workflow runs
gh run list

# View specific run
gh run view

# Watch a running workflow
gh run watch

# Download artifacts
gh run download
```

## Manage Secrets

```bash
# Set a secret
gh secret set SECRET_NAME

# Set secret from file
gh secret set SERVER_SSH_KEY < ~/.ssh/id_rsa

# List secrets
gh secret list

# Delete a secret
gh secret delete SECRET_NAME
```

## Local Testing (with act)

```bash
# Install act (GitHub Actions locally)
brew install act  # macOS
# or: curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run workflows locally
act                        # Run on push
act pull_request          # Run on PR
act -l                    # List workflows
act -j build-backend      # Run specific job
```

## Docker Commands

```bash
# Build Docker image
docker build -t bside/server:latest server/

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Build and push to Docker Hub
docker build -t yourusername/bside-server:v1.0.0 server/
docker push yourusername/bside-server:v1.0.0
```

## Deployment Commands

```bash
# Deploy backend manually
scp server/build/libs/server-1.0.0-all.jar user@server:/opt/bside/
ssh user@server "sudo systemctl restart bside-server"

# Check deployment status
ssh user@server "systemctl status bside-server"
ssh user@server "curl http://localhost:8080/health"

# View server logs
ssh user@server "journalctl -u bside-server -f"
```

## Build Commands

```bash
# Build all platforms locally
./gradlew build

# Build specific platforms
./gradlew :server:shadowJar                         # Backend
./gradlew :composeApp:assembleDebug                  # Android
./gradlew :composeApp:packageDistributionForCurrentOS # Desktop
./gradlew :composeApp:jsBrowserProductionWebpack     # Web

# Clean and rebuild
./gradlew clean build
```

## Troubleshooting

```bash
# Check workflow syntax
gh workflow view ci-cd.yml

# Re-run failed jobs
gh run rerun <run-id>

# Cancel a running workflow
gh run cancel

# Enable workflow dispatch (manual trigger)
gh workflow run ci-cd.yml

# View workflow logs
gh run view --log
```

## Useful GitHub URLs

```bash
# Actions dashboard
https://github.com/brentmzey/lovebside/actions

# Workflow files
https://github.com/brentmzey/lovebside/tree/main/.github/workflows

# Secrets management
https://github.com/brentmzey/lovebside/settings/secrets/actions

# Releases
https://github.com/brentmzey/lovebside/releases
```

## Badge for README

```markdown
![CI/CD](https://github.com/brentmzey/lovebside/actions/workflows/ci-cd.yml/badge.svg)
![Release](https://github.com/brentmzey/lovebside/actions/workflows/release.yml/badge.svg)
```

## Environment Variables

```bash
# Set for local development
export POCKETBASE_URL=http://localhost:8090
export SERVER_PORT=8080
export ENVIRONMENT=development

# Set for production (in GitHub secrets or server)
SERVER_HOST=your-server.com
SERVER_USER=deploy
DOCKER_USERNAME=yourusername
```

## Monitoring

```bash
# Check build time trends
gh run list --limit 10

# Download build artifacts
gh run download <run-id>

# View artifact list
gh api repos/brentmzey/lovebside/actions/artifacts
```

---

**For complete setup instructions, see [CI_CD_SETUP.md](CI_CD_SETUP.md)**
