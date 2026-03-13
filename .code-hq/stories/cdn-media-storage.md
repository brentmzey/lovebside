# Story: AWS CloudFront CDN Media Storage

**Priority**: High | **Status**: TODO | **Sprint**: 2

## Goal
Move media from PocketBase DB → AWS S3 + CloudFront CDN

## Quick Tasks
- [ ] Add AWS SDK to backend
- [ ] Create S3 buckets (dev/prod)
- [ ] Setup CloudFront distributions  
- [ ] Create `media_references` PocketBase collection
- [ ] Add upload endpoints to Ktor API
- [ ] Build client upload UI
- [ ] Migration script for existing media

## Config Needed
```bash
AWS_S3_BUCKET=bside-media-dev
AWS_CLOUDFRONT_DOMAIN=xxx.cloudfront.net
AWS_REGION=us-east-1
MAX_FILE_SIZE_MB=10
```

Full details: `AWS_CDN_IMPLEMENTATION_GUIDE.md`
