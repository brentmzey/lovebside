# AWS S3 + CloudFront CDN Implementation Guide

## 🎯 Overview

Complete implementation of AWS S3 storage with CloudFront CDN for media delivery in the Bside app.

**Status**: ✅ Ready to implement
**Last Updated**: February 1, 2026

---

## 📋 Table of Contents

1. [Architecture](#architecture)
2. [AWS Setup](#aws-setup)
3. [Configuration](#configuration)
4. [Implementation](#implementation)
5. [Migration Strategy](#migration-strategy)
6. [Performance Optimization](#performance-optimization)
7. [Testing](#testing)

---

## 🏗️ Architecture

### Current State: PocketBase Database Storage
- ✅ Images, videos, GIFs stored in PocketBase
- ✅ Direct database file storage
- ⚠️ Not optimized for scale
- ⚠️ No CDN acceleration

### Target State: AWS S3 + CloudFront
- ✅ Media stored in S3 buckets
- ✅ CloudFront CDN for global delivery
- ✅ Database stores URI references only
- ✅ Signed URLs for security
- ✅ Multi-region replication
- ✅ Automatic optimization

### Architecture Diagram
```
┌─────────────────┐
│   Mobile Apps   │
│  iOS/Android    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│   CloudFront    │◄─────┤  S3 Bucket   │
│      CDN        │      │  (Primary)   │
└────────┬────────┘      └──────────────┘
         │                       │
         │                       ▼
         │               ┌──────────────┐
         │               │  S3 Bucket   │
         │               │ (Replication)│
         │               └──────────────┘
         ▼
┌─────────────────┐
│   PocketBase    │
│   (URI refs)    │
└─────────────────┘
```

---

## 🔧 AWS Setup

### Step 1: Create S3 Bucket

```bash
# Create production bucket
aws s3 mb s3://bside-media-prod --region us-east-1

# Create development bucket
aws s3 mb s3://bside-media-dev --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
    --bucket bside-media-prod \
    --versioning-configuration Status=Enabled

# Configure lifecycle policy
aws s3api put-bucket-lifecycle-configuration \
    --bucket bside-media-prod \
    --lifecycle-configuration file://s3-lifecycle.json
```

### S3 Lifecycle Policy (`s3-lifecycle.json`)
```json
{
  "Rules": [
    {
      "Id": "TransitionToIA",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 180,
          "StorageClass": "GLACIER_IR"
        }
      ]
    },
    {
      "Id": "DeleteOldVersions",
      "Status": "Enabled",
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 30
      }
    }
  ]
}
```

### Step 2: Configure CORS

```bash
aws s3api put-bucket-cors \
    --bucket bside-media-prod \
    --cors-configuration file://s3-cors.json
```

### S3 CORS Configuration (`s3-cors.json`)
```json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "x-amz-request-id"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

### Step 3: Create CloudFront Distribution

```bash
aws cloudfront create-distribution \
    --distribution-config file://cloudfront-config.json
```

### CloudFront Configuration (`cloudfront-config.json`)
```json
{
  "CallerReference": "bside-media-cdn-2026",
  "Comment": "Bside Media CDN",
  "Enabled": true,
  "Origins": {
    "Quantity": 1,
    "Items": [
      {
        "Id": "S3-bside-media-prod",
        "DomainName": "bside-media-prod.s3.amazonaws.com",
        "S3OriginConfig": {
          "OriginAccessIdentity": "origin-access-identity/cloudfront/YOUR_OAI_ID"
        }
      }
    ]
  },
  "DefaultCacheBehavior": {
    "TargetOriginId": "S3-bside-media-prod",
    "ViewerProtocolPolicy": "redirect-to-https",
    "AllowedMethods": {
      "Quantity": 7,
      "Items": ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    },
    "Compress": true,
    "MinTTL": 0,
    "DefaultTTL": 86400,
    "MaxTTL": 31536000
  },
  "PriceClass": "PriceClass_All"
}
```

### Step 4: Create IAM User and Policy

```bash
# Create IAM user for the application
aws iam create-user --user-name bside-media-service

# Create access key
aws iam create-access-key --user-name bside-media-service

# Attach policy
aws iam put-user-policy \
    --user-name bside-media-service \
    --policy-name BsideMediaAccess \
    --policy-document file://iam-policy.json
```

### IAM Policy (`iam-policy.json`)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:GetObjectVersion"
      ],
      "Resource": [
        "arn:aws:s3:::bside-media-prod/*",
        "arn:aws:s3:::bside-media-prod"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation",
        "cloudfront:GetInvalidation",
        "cloudfront:ListInvalidations"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## ⚙️ Configuration

### Environment Variables

Add to `.env`:
```bash
# AWS Configuration
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key_here
AWS_SECRET_ACCESS_KEY=your_secret_key_here

# S3 Configuration
AWS_S3_BUCKET_PROD=bside-media-prod
AWS_S3_BUCKET_DEV=bside-media-dev
AWS_S3_BUCKET=${AWS_S3_BUCKET_DEV}  # Override in production

# CloudFront Configuration
AWS_CLOUDFRONT_DISTRIBUTION_ID=your_distribution_id
AWS_CLOUDFRONT_DOMAIN=d1234567890abc.cloudfront.net

# Media Configuration
MEDIA_MAX_SIZE_MB=100
MEDIA_ALLOWED_TYPES=image/*,video/*,audio/*
MEDIA_SIGNED_URL_EXPIRY=3600

# Feature Flags
ENABLE_CDN=true
ENABLE_S3_UPLOAD=true
FALLBACK_TO_POCKETBASE=true
```

### Production Configuration (`.env.production`)
```bash
AWS_S3_BUCKET=${AWS_S3_BUCKET_PROD}
ENABLE_CDN=true
ENABLE_S3_UPLOAD=true
FALLBACK_TO_POCKETBASE=false
```

---

## 💻 Implementation

### File Structure
```
shared/src/
├── commonMain/kotlin/love/bside/app/core/media/
│   ├── MediaStorageService.kt         # Interface
│   ├── AwsMediaStorage.kt             # AWS implementation
│   ├── PocketBaseMediaStorage.kt      # PocketBase implementation
│   ├── HybridMediaStorage.kt          # Migration wrapper
│   └── MediaConfig.kt                 # Configuration
├── jvmMain/kotlin/love/bside/app/core/media/
│   └── AwsS3Client.kt                 # JVM-specific AWS client
├── androidMain/kotlin/love/bside/app/core/media/
│   └── AwsS3ClientAndroid.kt          # Android-specific
└── iosMain/kotlin/love/bside/app/core/media/
    └── AwsS3ClientIOS.kt              # iOS-specific
```

### Key Components Created

1. **MediaStorageService.kt** - ✅ Created
2. **AwsMediaStorage.kt** - Implementation for AWS S3 + CloudFront
3. **HybridMediaStorage.kt** - Migration bridge (PocketBase → AWS)
4. **MediaConfig.kt** - Configuration management

---

## 🔄 Migration Strategy

### Phase 1: Dual Write (Current → Week 1)
```kotlin
// Write to both PocketBase and S3
// Read from PocketBase
upload() {
    pocketbase.upload()  // Primary
    s3.upload()          // Secondary (async)
}
```

### Phase 2: Dual Read (Week 2-3)
```kotlin
// Write to both
// Read from S3, fallback to PocketBase
fetch() {
    s3.fetch() ?: pocketbase.fetch()
}
```

### Phase 3: S3 Primary (Week 4+)
```kotlin
// Write to S3 only
// Read from S3 only
// PocketBase stores URI references
```

### Migration Script
```bash
#!/bin/bash
# migrate-media-to-s3.sh

# 1. Export all media from PocketBase
echo "Exporting media from PocketBase..."
curl -X GET "$POCKETBASE_URL/api/collections/m_messages/records" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | \
  jq -r '.items[] | select(.attachments != null) | .attachments[]' > media_list.txt

# 2. Upload to S3
echo "Uploading to S3..."
while IFS= read -r media_url; do
  filename=$(basename "$media_url")
  aws s3 cp "$media_url" "s3://$AWS_S3_BUCKET/messages/$filename"
done < media_list.txt

# 3. Update database with S3 URIs
echo "Updating database..."
# Run update script

echo "Migration complete!"
```

---

## 🚀 Performance Optimization

### 1. Image Optimization
```kotlin
// Automatic image optimization on upload
class ImageOptimizer {
    fun optimize(image: ByteArray): ByteArray {
        // Resize to max 2048px
        // Convert to WebP
        // Apply compression
        return optimized
    }
}
```

### 2. Video Transcoding
```kotlin
// AWS MediaConvert integration
class VideoTranscoder {
    suspend fun transcode(video: ByteArray): VideoSet {
        // Generate multiple resolutions
        // 1080p, 720p, 480p, 360p
        // HLS streaming format
        return videoSet
    }
}
```

### 3. CloudFront Caching
```
Cache-Control: public, max-age=31536000, immutable
```

### 4. Pre-signed URL Caching
```kotlin
// Cache signed URLs for 50 minutes (expires in 60)
val urlCache = LRUCache<String, SignedUrl>(maxSize = 1000)
```

---

## 🧪 Testing

### Unit Tests
```kotlin
class AwsMediaStorageTest {
    @Test
    fun `upload media successfully`() = runTest {
        val media = MediaUpload(/*...*/)
        val result = storage.uploadMedia(media)
        assertTrue(result.isRight())
    }
    
    @Test
    fun `fallback to PocketBase on S3 failure`() = runTest {
        // Simulate S3 failure
        // Verify fallback works
    }
}
```

### Integration Tests
```bash
# Test S3 upload
./gradlew :shared:testDebugUnitTest --tests "*AwsMediaStorage*"

# Test CloudFront delivery
curl -I https://d1234567890abc.cloudfront.net/test-image.jpg

# Test signed URL generation
./test-signed-urls.sh
```

### Load Testing
```bash
# Apache Bench
ab -n 10000 -c 100 https://d1234567890abc.cloudfront.net/test-image.jpg

# Expected: < 50ms p99 latency
```

---

## 📊 Monitoring

### CloudWatch Metrics
- S3 bucket size
- Request count
- Error rate
- Latency

### CloudFront Metrics
- Cache hit ratio (target: > 90%)
- Data transfer
- 4xx/5xx errors

### Alerts
```yaml
- S3 error rate > 1%
- CloudFront cache hit < 85%
- Upload failures > 5/min
- Storage quota > 80%
```

---

## 💰 Cost Estimation

### Monthly Costs (10,000 users)
```
S3 Storage (100 GB):          $2.30
S3 Requests (1M):             $0.40
CloudFront Transfer (500 GB): $42.50
CloudFront Requests (10M):    $10.00
-------------------------------------
Total:                        ~$55/month
```

### Scaling to 100,000 users
```
S3 Storage (1 TB):            $23.00
CloudFront Transfer (5 TB):   $425.00
-------------------------------------
Total:                        ~$550/month
```

---

## ✅ Implementation Checklist

### AWS Setup
- [ ] Create S3 buckets (prod + dev)
- [ ] Configure bucket policies
- [ ] Enable versioning
- [ ] Set up lifecycle rules
- [ ] Create CloudFront distribution
- [ ] Configure Origin Access Identity
- [ ] Set up SSL certificate
- [ ] Create IAM user and policy
- [ ] Generate access keys

### Code Implementation
- [x] MediaStorageService interface
- [ ] AWS S3 client (common)
- [ ] Platform-specific implementations
- [ ] Hybrid storage wrapper
- [ ] Migration utilities
- [ ] Unit tests
- [ ] Integration tests

### Database Schema
- [ ] Add `cdn_url` column to messages
- [ ] Add `storage_provider` enum
- [ ] Create media_references table
- [ ] Migration script for existing data

### Deployment
- [ ] Add environment variables to CI/CD
- [ ] Update Docker configurations
- [ ] Deploy to staging
- [ ] Run migration script
- [ ] Monitor for 24 hours
- [ ] Deploy to production

---

## 🎓 Next Steps

1. **Complete AWS setup** (30 minutes)
2. **Implement AWS S3 client** (2 hours)
3. **Create hybrid storage** (1 hour)
4. **Write tests** (2 hours)
5. **Update database schema** (1 hour)
6. **Deploy to staging** (30 minutes)
7. **Run migration** (varies by data size)
8. **Monitor and optimize** (ongoing)

---

## 📚 References

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [CloudFront Documentation](https://docs.aws.amazon.com/cloudfront/)
- [AWS SDK for Kotlin](https://github.com/awslabs/aws-sdk-kotlin)
- [PocketBase File Storage](https://pocketbase.io/docs/files/)

---

**Status**: Ready to implement
**Priority**: High (required for scale)
**Estimated Time**: 1-2 days
**Team**: Backend + DevOps

