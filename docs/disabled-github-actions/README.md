# Disabled GitHub Actions

These workflow files have been disabled to reduce costs.

## Files moved here:
- `ci-cd.yml` - Main CI/CD pipeline
- `nightly.yml` - Nightly builds
- `pr-check.yml` - Pull request checks
- `release.yml` - Release automation

## To re-enable:
Move the desired workflow files back to `.github/workflows/` directory:
```bash
cp ./docs/disabled-github-actions/*.yml .github/workflows/
```

## Date disabled:
November 16, 2025
