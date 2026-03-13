# AWS CloudFront + S3 CDN Setup for B-Side

## Overview

This guide sets up **AWS CloudFront** with **S3** as a CDN for B-Side media files (images, videos, GIFs), ensuring fast, scalable, and cost-effective delivery worldwide.

---

## Architecture

```
User Request → CloudFront (CDN) → S3 Bucket → Files
                     ↓
              (Edge Cache - 24hr TTL)
```

**Benefits:**

- ⚡ Low latency worldwide (edge locations)
- 💰 Reduced bandwidth costs
- 🔒 Secure with signed URLs/cookies
- 📈 Scales automatically

---

## Prerequisites

- AWS Account
- AWS CLI installed (`brew install awscli`)
- Appropriate IAM permissions

---

## Step 1: Create S3 Bucket

### Via AWS Console

1. Navigate to **S3** → **Create bucket**
2. **Bucket name**: `bside-media-prod` (must be globally unique)
3. **Region**: `us-east-1` (or closest to your users)
4. **Block Public Access**: Keep enabled (CloudFront will access privately)
5. **Versioning**: Enable (optional, for backup)
6. **Encryption**: Enable with SSE-S3
7. Click **Create bucket**

### Via AWS CLI

```bash
aws s3 mb s3://bside-media-prod --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket bside-media-prod \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket bside-media-prod \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

---

## Step 2: Create CloudFront Distribution

### Via AWS Console

1. Navigate to **CloudFront** → **Create distribution**
2. **Origin Settings**:
   - **Origin domain**: Select your S3 bucket (`bside-media-prod.s3.us-east-1.amazonaws.com`)
   - **Origin access**: **Origin access control** (OAC)
   - **Create control setting**: Auto-create OAC
3. **Default cache behavior**:
   - **Viewer protocol policy**: Redirect HTTP to HTTPS
   - **Allowed HTTP methods**: GET, HEAD, OPTIONS
   - **Cache policy**: CachingOptimized
   - **Origin request policy**: CORS-S3Origin
4. **Settings**:
   - **Price class**: Use all edge locations (or select specific regions)
   - **Alternate domain names (CNAMEs)**: `cdn.bside.app` (if using custom domain)
   - **SSL certificate**: Request or import ACM certificate
5. Click **Create distribution**
6. **Copy the S3 bucket policy** from the CloudFront console and apply it to your S3 bucket

### Via AWS CLI

```bash
# Create CloudFront OAC
aws cloudfront create-origin-access-control \
  --origin-access-control-config '{
    "Name": "bside-media-oac",
    "OriginAccessControlOriginType": "s3",
    "SigningBehavior": "always",
    "SigningProtocol": "sigv4"
  }'

# Create distribution (save this as cloudfront-config.json)
cat > cloudfront-config.json <<EOF
{
  "CallerReference": "bside-media-$(date +%s)",
  "Comment": "B-Side Media CDN",
  "Enabled": true,
  "Origins": {
    "Quantity": 1,
    "Items": [{
      "Id": "S3-bside-media-prod",
      "DomainName": "bside-media-prod.s3.us-east-1.amazonaws.com",
      "OriginAccessControlId": "YOUR_OAC_ID",
      "S3OriginConfig": {
        "OriginAccessIdentity": ""
      }
    }]
  },
  "DefaultCacheBehavior": {
    "TargetOriginId": "S3-bside-media-prod",
    "ViewerProtocolPolicy": "redirect-to-https",
    "AllowedMethods": {
      "Quantity": 3,
      "Items": ["GET", "HEAD", "OPTIONS"]
    },
    "CachePolicyId": "658327ea-f89d-4fab-a63d-7e88639e58f6",
    "OriginRequestPolicyId": "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"
  }
}
EOF

aws cloudfront create-distribution --distribution-config file://cloudfront-config.json
```

---

## Step 3: Update S3 Bucket Policy

Apply this policy to allow CloudFront to access S3:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "AllowCloudFrontServicePrincipal",
    "Effect": "Allow",
    "Principal": {
      "Service": "cloudfront.amazonaws.com"
    },
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::bside-media-prod/*",
    "Condition": {
      "StringEquals": {
        "AWS:SourceArn": "arn:aws:cloudfront::YOUR_ACCOUNT_ID:distribution/YOUR_DISTRIBUTION_ID"
      }
    }
  }]
}
```

Apply via CLI:

```bash
aws s3api put-bucket-policy \
  --bucket bside-media-prod \
  --policy file://bucket-policy.json
```

---

## Step 4: Configure CORS on S3

Allow web clients to fetch media:

```bash
cat > cors-config.json <<EOF
{
  "CORSRules": [{
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedOrigins": [
      "https://bside.app",
      "https://www.bside.app",
      "http://localhost:*"
    ],
    "ExposeHeaders": ["ETag", "Content-Length"],
    "MaxAgeSeconds": 3000
  }]
}
EOF

aws s3api put-bucket-cors \
  --bucket bside-media-prod \
  --cors-configuration file://cors-config.json
```

---

## Step 5: Set Up Custom Domain (Optional)

### Create SSL Certificate in ACM

```bash
# Must be in us-east-1 for CloudFront
aws acm request-certificate \
  --domain-name cdn.bside.app \
  --validation-method DNS \
  --region us-east-1
```

### Add DNS Record

Add the CNAME validation record to your DNS provider (Route 53, CloudFlare, etc.)

### Update CloudFront

1. Go to **CloudFront** → Your distribution → **Edit**
2. **Alternate domain names**: Add `cdn.bside.app`
3. **Custom SSL certificate**: Select your ACM certificate
4. Save changes

### Add CNAME in DNS

Point `cdn.bside.app` to your CloudFront distribution domain:

```
cdn.bside.app  CNAME  d111111abcdef8.cloudfront.net
```

---

## Step 6: Configure Environment Variables

### PocketBase `.env`

```bash
# CDN Configuration
CDN_ENABLED=true
CDN_BASE_URL=https://d111111abcdef8.cloudfront.net
# Or with custom domain:
# CDN_BASE_URL=https://cdn.bside.app

# S3 Configuration
AWS_REGION=us-east-1
AWS_S3_BUCKET=bside-media-prod
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

### Docker Compose

```yaml
services:
  pocketbase:
    environment:
      - CDN_ENABLED=${CDN_ENABLED:-false}
      - CDN_BASE_URL=${CDN_BASE_URL}
      - AWS_REGION=${AWS_REGION}
      - AWS_S3_BUCKET=${AWS_S3_BUCKET}
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
```

---

## Step 7: Upload Files to S3

### Manual Upload

```bash
aws s3 cp /path/to/file.jpg s3://bside-media-prod/images/file.jpg \
  --content-type image/jpeg \
  --cache-control "max-age=31536000"
```

### Programmatic Upload (from PocketBase)

Add to PocketBase hooks (`pocketbase/src/hooks/s3_upload.ts`):

```typescript
// Example S3 upload hook
import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";

const s3Client = new S3Client({
  region: process.env.AWS_REGION || 'us-east-1',
  credentials: {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID!,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY!
  }
});

export async function uploadToS3(file: Buffer, key: string, contentType: string) {
  await s3Client.send(new PutObjectCommand({
    Bucket: process.env.AWS_S3_BUCKET!,
    Key: key,
    Body: file,
    ContentType: contentType,
    CacheControl: 'max-age=31536000'
  }));
  
  return `${process.env.CDN_BASE_URL}/${key}`;
}
```

---

## Step 8: Test CDN

```bash
# Upload test file
aws s3 cp test.jpg s3://bside-media-prod/test.jpg

# Access via CloudFront
curl -I https://d111111abcdef8.cloudfront.net/test.jpg

# Should return:
# HTTP/2 200
# x-cache: Hit from cloudfront (after first request)
```

---

## Cost Optimization

### S3 Lifecycle Rules

Move old files to cheaper storage:

```bash
cat > lifecycle-policy.json <<EOF
{
  "Rules": [{
    "Id": "MoveToIA",
    "Status": "Enabled",
    "Transitions": [{
      "Days": 90,
      "StorageClass": "STANDARD_IA"
    }, {
      "Days": 180,
      "StorageClass": "GLACIER"
    }]
  }]
}
EOF

aws s3api put-bucket-lifecycle-configuration \
  --bucket bside-media-prod \
  --lifecycle-configuration file://lifecycle-policy.json
```

### CloudFront Cache Settings

- **TTL**: 24 hours for media (rarely changes)
- **Compress**: Enable for images
- **Price Class**: Select regions closest to users

---

## Monitoring

### CloudWatch Metrics

- **CloudFront**: Requests, Bytes Downloaded, Error Rate
- **S3**: Bucket size, Request count

### Set Up Alarms

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name bside-cdn-error-rate \
  --metric-name 4xxErrorRate \
  --namespace AWS/CloudFront \
  --statistic Average \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold
```

---

## Security Best Practices

1. **Signed URLs**: Generate time-limited URLs for sensitive media
2. **Least Privilege IAM**: Only grant necessary S3 permissions
3. **Enable S3 Access Logs**: Track all access
4. **WAF**: Add AWS WAF to CloudFront for DDoS protection
5. **Rotate Keys**: Regularly rotate AWS access keys

---

## Useful Commands

```bash
# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id YOUR_DIST_ID \
  --paths "/*"

# List S3 objects
aws s3 ls s3://bside-media-prod/

# Sync local directory to S3
aws s3 sync ./media/ s3://bside-media-prod/ --delete

# Get distribution info
aws cloudfront get-distribution --id YOUR_DIST_ID
```

---

## Troubleshooting

### "Access Denied" errors

→ Check S3 bucket policy includes CloudFront OAC

### Files not caching

→ Verify `Cache-Control` headers are set on S3 objects

### Slow first load

→ Normal - CloudFront caches on first request per edge

### CORS errors

→ Check S3 CORS configuration includes your domain

---

## Next Steps

1. ✅ Set up S3 bucket and CloudFront
2. ✅ Configure environment variables
3. [ ] Implement S3 upload hooks in PocketBase
4. [ ] Update client code to use CDN URLs
5. [ ] Test upload/download flow
6. [ ] Monitor costs and performance

---

## Related Documentation

- [AWS CloudFront Docs](https://docs.aws.amazon.com/cloudfront/)
- [AWS S3 Docs](https://docs.aws.amazon.com/s3/)
- [PocketBase File Storage](https://pocketbase.io/docs/files-handling/)
