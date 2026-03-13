# Media Storage & CDN Configuration
# Self-hosted S3 + CloudFront Setup for Bside

## Architecture Overview

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│                 │         │                  │         │                 │
│  PocketBase DB  │────────▶│  S3 Bucket       │────────▶│  CloudFront CDN │
│  (URI refs)     │         │  (Media Storage) │         │  (Global Edge)  │
│                 │         │                  │         │                 │
└─────────────────┘         └──────────────────┘         └─────────────────┘
        │                            │                            │
        │                            │                            │
        ▼                            ▼                            ▼
   Metadata                     Raw Files                   Optimized
   Queries                      Upload/Delete               Delivery
   Relationships                                            Caching
```

## Current State (Phase 1)
- ✅ PocketBase stores files locally
- ✅ Database has CDN URI fields ready
- ✅ Dual storage support in schema

## Migration Path

### Phase 1: Dual Storage (Current)
- PocketBase continues to store files
- New CDN URI fields capture S3 URLs
- Gradual upload to S3 in background

### Phase 2: Hybrid (Transition)
- New uploads go directly to S3
- Old files remain in PocketBase
- Background migration job runs
- CDN URIs populate automatically

### Phase 3: S3-Only (Target)
- All files served from CloudFront
- PocketBase stores only URIs
- Local storage becomes cache only

---

## AWS S3 Setup

### 1. Create S3 Bucket

```bash
# Create bucket with versioning and encryption
aws s3api create-bucket \
  --bucket bside-media-production \
  --region us-east-1 \
  --create-bucket-configuration LocationConstraint=us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket bside-media-production \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket bside-media-production \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

### 2. Configure Bucket Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::bside-media-production/public/*"
    },
    {
      "Sid": "CloudFrontOriginAccess",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity YOUR_OAI_ID"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::bside-media-production/*"
    }
  ]
}
```

### 3. Configure CORS

```json
{
  "CORSRules": [
    {
      "AllowedOrigins": [
        "https://bside.app",
        "https://www.bside.app",
        "https://dev.bside.app",
        "http://localhost:3000"
      ],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "x-amz-request-id"],
      "MaxAgeSeconds": 3000
    }
  ]
}
```

### 4. Lifecycle Rules

```json
{
  "Rules": [
    {
      "Id": "DeleteOldThumbnails",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "thumbnails/"
      },
      "Expiration": {
        "Days": 90
      }
    },
    {
      "Id": "TransitionToGlacier",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "archived/"
      },
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ]
    }
  ]
}
```

---

## CloudFront Distribution Setup

### 1. Create Distribution

```json
{
  "DistributionConfig": {
    "CallerReference": "bside-media-2026",
    "Aliases": {
      "Quantity": 1,
      "Items": ["media.bside.app"]
    },
    "DefaultRootObject": "",
    "Origins": {
      "Quantity": 1,
      "Items": [
        {
          "Id": "S3-bside-media",
          "DomainName": "bside-media-production.s3.amazonaws.com",
          "S3OriginConfig": {
            "OriginAccessIdentity": "origin-access-identity/cloudfront/YOUR_OAI_ID"
          }
        }
      ]
    },
    "DefaultCacheBehavior": {
      "TargetOriginId": "S3-bside-media",
      "ViewerProtocolPolicy": "redirect-to-https",
      "AllowedMethods": {
        "Quantity": 3,
        "Items": ["GET", "HEAD", "OPTIONS"]
      },
      "Compress": true,
      "MinTTL": 0,
      "DefaultTTL": 86400,
      "MaxTTL": 31536000,
      "ForwardedValues": {
        "QueryString": false,
        "Cookies": {
          "Forward": "none"
        },
        "Headers": {
          "Quantity": 1,
          "Items": ["Origin"]
        }
      }
    },
    "CacheBehaviors": {
      "Quantity": 2,
      "Items": [
        {
          "PathPattern": "/thumbnails/*",
          "TargetOriginId": "S3-bside-media",
          "ViewerProtocolPolicy": "redirect-to-https",
          "MinTTL": 0,
          "DefaultTTL": 604800,
          "MaxTTL": 31536000,
          "Compress": true
        },
        {
          "PathPattern": "/videos/*",
          "TargetOriginId": "S3-bside-media",
          "ViewerProtocolPolicy": "redirect-to-https",
          "MinTTL": 0,
          "DefaultTTL": 2592000,
          "MaxTTL": 31536000,
          "Compress": false
        }
      ]
    },
    "ViewerCertificate": {
      "ACMCertificateArn": "arn:aws:acm:us-east-1:ACCOUNT:certificate/CERT_ID",
      "SSLSupportMethod": "sni-only",
      "MinimumProtocolVersion": "TLSv1.2_2021"
    },
    "Enabled": true
  }
}
```

---

## Directory Structure

```
s3://bside-media-production/
├── profiles/
│   ├── avatars/
│   │   ├── original/
│   │   ├── 50x50/
│   │   ├── 100x100/
│   │   ├── 200x200/
│   │   └── 400x400/
│   └── photos/
│       ├── original/
│       ├── 200x200/
│       ├── 600x600/
│       └── 1200x1200/
├── messages/
│   ├── images/
│   │   ├── original/
│   │   ├── 100x100/
│   │   ├── 400x400/
│   │   ├── 800x800/
│   │   └── 1200x1200/
│   ├── videos/
│   │   ├── original/
│   │   ├── 480p/
│   │   ├── 720p/
│   │   └── 1080p/
│   ├── documents/
│   └── audio/
├── conversations/
│   └── avatars/
│       ├── original/
│       ├── 50x50/
│       ├── 100x100/
│       └── 200x200/
└── temp/
    └── uploads/
```

---

## Environment Configuration

### `.env.production`

```bash
# S3 Configuration
AWS_REGION=us-east-1
AWS_S3_BUCKET=bside-media-production
AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY
AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY

# CloudFront
CDN_DOMAIN=media.bside.app
CDN_BASE_URL=https://media.bside.app

# Media Processing
MEDIA_PROCESSING_ENABLED=true
THUMBNAIL_GENERATION=true
VIDEO_TRANSCODING=true

# Storage Strategy
STORAGE_MODE=hybrid  # local | hybrid | s3-only
LOCAL_STORAGE_FALLBACK=true
UPLOAD_DIRECTLY_TO_S3=true

# Upload Limits
MAX_FILE_SIZE_MB=50
MAX_VIDEO_SIZE_MB=500
ALLOWED_IMAGE_TYPES=jpg,jpeg,png,webp,gif
ALLOWED_VIDEO_TYPES=mp4,webm,mov
ALLOWED_DOCUMENT_TYPES=pdf,doc,docx

# Performance
ENABLE_CDN_CACHE_HEADERS=true
CDN_CACHE_TTL_IMAGES=604800
CDN_CACHE_TTL_VIDEOS=2592000
CDN_CACHE_TTL_DOCUMENTS=86400
```

### `.env.development`

```bash
# Use local storage in development
STORAGE_MODE=local
LOCAL_STORAGE_PATH=./pocketbase/pb_data/storage

# Mock S3 for testing
USE_LOCALSTACK=true
LOCALSTACK_ENDPOINT=http://localhost:4566
```

---

## Migration Script

See: `scripts/migrate-media-to-s3.js`

```bash
# Start migration (dry run)
npm run migrate:media -- --dry-run

# Migrate specific collection
npm run migrate:media -- --collection=s_profiles --field=photos

# Full migration
npm run migrate:media -- --all

# Monitor progress
npm run migrate:media -- --status
```

---

## Testing

```bash
# Test S3 connection
npm run test:s3-connection

# Test CloudFront distribution
npm run test:cdn-distribution

# Upload test file
npm run test:upload -- --file=./test-image.jpg

# Verify media integrity
npm run test:media-integrity
```

---

## Cost Estimation

### S3 Storage (First 50 TB/Month)
- $0.023 per GB = ~$23/TB/month
- 1,000 users × 100MB average = 100GB = **$2.30/month**
- 10,000 users × 100MB average = 1TB = **$23/month**

### CloudFront Data Transfer
- First 10 TB: $0.085/GB
- 1M requests/month with 500KB avg = 500GB = **$42.50/month**

### Request Costs
- 10M GET requests = $10
- 1M POST/PUT requests = $5

### Total Estimated Costs
- 10,000 active users: **~$100-200/month**
- 100,000 active users: **~$500-1,000/month**

---

## Monitoring & Alerts

```yaml
# CloudWatch Alarms
Alarms:
  - Name: HighS3Costs
    Metric: EstimatedCharges
    Threshold: $200
    Period: 1 day
  
  - Name: CloudFrontErrors
    Metric: 5xxErrorRate
    Threshold: 1%
    Period: 5 minutes
  
  - Name: SlowMediaLoading
    Metric: OriginLatency
    Threshold: 500ms
    Period: 5 minutes
```

---

## Security Best Practices

1. **Presigned URLs**: Generate temporary upload URLs
2. **Virus Scanning**: Scan all uploads before S3
3. **Content Validation**: Verify MIME types server-side
4. **Rate Limiting**: Limit uploads per user
5. **Access Control**: Use IAM roles, not access keys
6. **Encryption**: Enable at-rest and in-transit
7. **Monitoring**: Track all access attempts
8. **Backup**: Enable versioning and cross-region replication

---

## Next Steps

1. ✅ Create AWS account and configure credentials
2. ✅ Set up S3 bucket with policies
3. ✅ Configure CloudFront distribution
4. ✅ Update DNS (media.bside.app → CloudFront)
5. ⏳ Deploy migration scripts
6. ⏳ Test upload/download flows
7. ⏳ Monitor performance and costs
8. ⏳ Gradual migration of existing files

---

## Support Resources

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [CloudFront Documentation](https://docs.aws.amazon.com/cloudfront/)
- [PocketBase File Storage](https://pocketbase.io/docs/files-handling/)
- [Cost Calculator](https://calculator.aws/)

---

**Last Updated**: 2026-02-01  
**Version**: 1.0  
**Status**: Ready for Implementation
