# 🚀 Staging Environment Deployment Guide

**Target:** AWS ECS Fargate (Staging)  
**Timeline:** 2-3 hours setup, 1 hour testing  
**Status:** 📋 READY TO EXECUTE  
**Date:** 2026-01-31

---

## Prerequisites ✅

Before starting, ensure you have:

- [ ] AWS Account with admin access
- [ ] AWS CLI installed and configured (`aws configure`)
- [ ] Docker Desktop running
- [ ] Terraform installed (v1.6+)
- [ ] Domain name for staging (e.g., `staging.bside.app`)
- [ ] Budget approval (~$150-200/month for staging)

---

## Architecture Overview

### Staging Stack
```
Route 53 (DNS)
    ↓
CloudFront (CDN)
    ↓
Application Load Balancer (ALB)
    ↓
    ├── ECS Fargate (Backend) × 2 tasks
    ├── ECS Fargate (PocketBase) × 1 task
    └── ElastiCache Redis × 1 node
```

**Services:**
- **Backend API** - 2 Fargate tasks (auto-scaling 1-4)
- **PocketBase** - 1 Fargate task with EBS volume
- **Redis** - 1 ElastiCache node (cache.t3.micro)
- **ALB** - Application Load Balancer
- **CloudFront** - CDN for static assets
- **S3** - Static file storage
- **CloudWatch** - Logs & metrics
- **Secrets Manager** - Credentials storage

**NOT included in staging (to save cost):**
- ❌ Prometheus/Grafana (use CloudWatch instead)
- ❌ Multi-region (single region: us-east-1)
- ❌ Auto-scaling beyond 4 tasks
- ❌ High-availability database (single AZ)

---

## Step 1: Prepare Docker Images (15 min)

### 1.1 Build and Tag Images

```bash
# Navigate to project
cd ~/bside

# Build backend
./gradlew :server:shadowJar
docker build -t bside-backend:staging -f ./server/Dockerfile .

# Build PocketBase
docker build -t bside-pocketbase:staging ./pocketbase

# Tag for ECR (replace ACCOUNT_ID and REGION)
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=us-east-1

docker tag bside-backend:staging ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/bside-backend:staging
docker tag bside-pocketbase:staging ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/bside-pocketbase:staging
```

### 1.2 Create ECR Repositories

```bash
# Create repositories
aws ecr create-repository --repository-name bside-backend --region us-east-1
aws ecr create-repository --repository-name bside-pocketbase --region us-east-1

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com

# Push images
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/bside-backend:staging
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/bside-pocketbase:staging
```

---

## Step 2: Infrastructure as Code (30 min)

### 2.1 Initialize Terraform

Create `terraform/staging/` directory:

```bash
mkdir -p terraform/staging
cd terraform/staging
```

### 2.2 Create `main.tf`

```hcl
terraform {
  required_version = ">= 1.6"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket         = "bside-terraform-state"
    key            = "staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bside-terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = "staging"
      Project     = "bside"
      ManagedBy   = "terraform"
    }
  }
}

# VPC
module "vpc" {
  source = "../modules/vpc"
  
  environment = "staging"
  cidr_block  = "10.1.0.0/16"
}

# ECS Cluster
module "ecs" {
  source = "../modules/ecs"
  
  environment    = "staging"
  vpc_id         = module.vpc.vpc_id
  private_subnets = module.vpc.private_subnets
  public_subnets  = module.vpc.public_subnets
}

# Redis
module "redis" {
  source = "../modules/redis"
  
  environment     = "staging"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnets
  node_type       = "cache.t3.micro"
}

# Load Balancer
module "alb" {
  source = "../modules/alb"
  
  environment     = "staging"
  vpc_id          = module.vpc.vpc_id
  public_subnets  = module.vpc.public_subnets
  certificate_arn = var.certificate_arn
}

# Backend Service
module "backend" {
  source = "../modules/ecs-service"
  
  name            = "backend"
  environment     = "staging"
  cluster_id      = module.ecs.cluster_id
  image           = "${var.aws_account_id}.dkr.ecr.us-east-1.amazonaws.com/bside-backend:staging"
  cpu             = 512
  memory          = 1024
  desired_count   = 2
  
  environment_variables = {
    REDIS_URL      = module.redis.endpoint
    ENVIRONMENT    = "staging"
    LOG_LEVEL      = "info"
  }
  
  secrets = {
    POCKETBASE_URL = aws_secretsmanager_secret.pocketbase_url.arn
  }
  
  target_group_arn = module.alb.backend_target_group_arn
  
  subnets          = module.vpc.private_subnets
  security_groups  = [module.ecs.backend_security_group_id]
}

# PocketBase Service
module "pocketbase" {
  source = "../modules/ecs-service"
  
  name            = "pocketbase"
  environment     = "staging"
  cluster_id      = module.ecs.cluster_id
  image           = "${var.aws_account_id}.dkr.ecr.us-east-1.amazonaws.com/bside-pocketbase:staging"
  cpu             = 512
  memory          = 1024
  desired_count   = 1
  
  environment_variables = {
    ENVIRONMENT = "staging"
  }
  
  secrets = {
    ADMIN_EMAIL    = aws_secretsmanager_secret.admin_email.arn
    ADMIN_PASSWORD = aws_secretsmanager_secret.admin_password.arn
  }
  
  target_group_arn = module.alb.pocketbase_target_group_arn
  
  subnets          = module.vpc.private_subnets
  security_groups  = [module.ecs.pocketbase_security_group_id]
  
  # Persistent storage for database
  volumes = [{
    name      = "pocketbase-data"
    host_path = null
    efs_volume_configuration = {
      file_system_id = aws_efs_file_system.pocketbase.id
      root_directory = "/pb_data"
    }
  }]
}

# CloudFront CDN
module "cdn" {
  source = "../modules/cloudfront"
  
  environment       = "staging"
  alb_domain_name   = module.alb.dns_name
  s3_bucket_name    = aws_s3_bucket.static_assets.bucket
  domain_name       = "staging.bside.app"
  certificate_arn   = var.certificate_arn
}

# S3 for static assets
resource "aws_s3_bucket" "static_assets" {
  bucket = "bside-static-staging"
}

# EFS for PocketBase data
resource "aws_efs_file_system" "pocketbase" {
  creation_token = "bside-pocketbase-staging"
  
  tags = {
    Name = "bside-pocketbase-staging"
  }
}

# Secrets
resource "aws_secretsmanager_secret" "pocketbase_url" {
  name = "bside-staging-pocketbase-url"
}

resource "aws_secretsmanager_secret" "admin_email" {
  name = "bside-staging-admin-email"
}

resource "aws_secretsmanager_secret" "admin_password" {
  name = "bside-staging-admin-password"
}

# CloudWatch Logs
resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/bside-staging-backend"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "pocketbase" {
  name              = "/ecs/bside-staging-pocketbase"
  retention_in_days = 7
}
```

### 2.3 Create `variables.tf`

```hcl
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
}

variable "certificate_arn" {
  description = "ACM Certificate ARN for HTTPS"
  type        = string
}

variable "domain_name" {
  description = "Domain name for staging"
  type        = string
  default     = "staging.bside.app"
}
```

### 2.4 Create `outputs.tf`

```hcl
output "alb_dns_name" {
  description = "DNS name of the load balancer"
  value       = module.alb.dns_name
}

output "cloudfront_domain" {
  description = "CloudFront distribution domain"
  value       = module.cdn.cloudfront_domain
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = module.redis.endpoint
}

output "backend_url" {
  description = "Backend API URL"
  value       = "https://staging.bside.app/api"
}
```

---

## Step 3: Deploy Infrastructure (20 min)

### 3.1 Initialize Terraform

```bash
cd terraform/staging

# Create S3 bucket for state
aws s3 mb s3://bside-terraform-state --region us-east-1

# Create DynamoDB table for locking
aws dynamodb create-table \
  --table-name bside-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1

# Initialize
terraform init
```

### 3.2 Plan and Apply

```bash
# Plan
terraform plan \
  -var="aws_account_id=$(aws sts get-caller-identity --query Account --output text)" \
  -var="certificate_arn=arn:aws:acm:us-east-1:ACCOUNT:certificate/CERT_ID" \
  -out=tfplan

# Review the plan carefully!

# Apply
terraform apply tfplan
```

**Expected time:** 15-20 minutes (VPC, ECS, ALB creation is slow)

---

## Step 4: Configure Secrets (5 min)

```bash
# Set PocketBase URL
aws secretsmanager put-secret-value \
  --secret-id bside-staging-pocketbase-url \
  --secret-string "http://pocketbase.local:8090" \
  --region us-east-1

# Set admin credentials
aws secretsmanager put-secret-value \
  --secret-id bside-staging-admin-email \
  --secret-string "admin@bside.app" \
  --region us-east-1

aws secretsmanager put-secret-value \
  --secret-id bside-staging-admin-password \
  --secret-string "$(openssl rand -base64 32)" \
  --region us-east-1
```

---

## Step 5: DNS Configuration (10 min)

### 5.1 Get CloudFront Domain

```bash
terraform output cloudfront_domain
# Example: d123456abcdef.cloudfront.net
```

### 5.2 Create DNS Records

In your DNS provider (Route 53, Cloudflare, etc.):

```
Type: CNAME
Name: staging.bside.app
Value: d123456abcdef.cloudfront.net
TTL: 300
```

OR if using Route 53:

```bash
# Get Hosted Zone ID
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
  --query "HostedZones[?Name=='bside.app.'].Id" \
  --output text | cut -d'/' -f3)

# Create alias record
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://dns-change.json
```

`dns-change.json`:
```json
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "staging.bside.app",
      "Type": "CNAME",
      "TTL": 300,
      "ResourceRecords": [{"Value": "d123456abcdef.cloudfront.net"}]
    }
  }]
}
```

---

## Step 6: Verify Deployment (10 min)

### 6.1 Check ECS Services

```bash
# List services
aws ecs list-services --cluster bside-staging --region us-east-1

# Check service status
aws ecs describe-services \
  --cluster bside-staging \
  --services bside-staging-backend bside-staging-pocketbase \
  --region us-east-1 \
  --query 'services[*].[serviceName,status,desiredCount,runningCount]' \
  --output table
```

### 6.2 Test Health Endpoints

```bash
# Wait for DNS propagation (2-5 minutes)
sleep 300

# Test backend
curl https://staging.bside.app/api/health

# Test PocketBase
curl https://staging.bside.app/api/pb/api/health

# Expected: Both should return healthy status
```

### 6.3 Check CloudWatch Logs

```bash
# Backend logs
aws logs tail /ecs/bside-staging-backend --follow --region us-east-1

# PocketBase logs
aws logs tail /ecs/bside-staging-pocketbase --follow --region us-east-1
```

---

## Step 7: Smoke Tests (15 min)

### 7.1 API Tests

```bash
# Backend health
curl https://staging.bside.app/api/health

# PocketBase health
curl https://staging.bside.app/api/pb/api/health

# Backend metrics (if enabled)
curl https://staging.bside.app/api/metrics
```

### 7.2 Load Test

```bash
# Install k6 if not already
brew install k6

# Run simple load test
k6 run - <<EOF
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
};

export default function () {
  const res = http.get('https://staging.bside.app/api/health');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
EOF
```

### 7.3 End-to-End Test

1. Open https://staging.bside.app in browser
2. Create test user account
3. Login
4. Test key user flows
5. Check browser console for errors
6. Check Network tab for failed requests

---

## Step 8: Monitoring Setup (10 min)

### 8.1 CloudWatch Dashboard

```bash
# Create dashboard
aws cloudwatch put-dashboard \
  --dashboard-name bside-staging \
  --dashboard-body file://dashboard.json \
  --region us-east-1
```

`dashboard.json`:
```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ECS", "CPUUtilization", {"stat": "Average"}],
          [".", "MemoryUtilization", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "ECS Resource Usage"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ApplicationELB", "TargetResponseTime", {"stat": "Average"}],
          [".", "RequestCount", {"stat": "Sum"}]
        ],
        "period": 300,
        "region": "us-east-1",
        "title": "ALB Performance"
      }
    }
  ]
}
```

### 8.2 CloudWatch Alarms

```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name bside-staging-high-cpu \
  --alarm-description "Alert when CPU > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --region us-east-1

# High error rate alarm
aws cloudwatch put-metric-alarm \
  --alarm-name bside-staging-high-errors \
  --alarm-description "Alert when 5xx errors > 10" \
  --metric-name HTTPCode_Target_5XX_Count \
  --namespace AWS/ApplicationELB \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --region us-east-1
```

---

## Step 9: CI/CD Pipeline (Optional, 20 min)

### 9.1 GitHub Actions Workflow

Create `.github/workflows/deploy-staging.yml`:

```yaml
name: Deploy to Staging

on:
  push:
    branches: [staging]

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY_BACKEND: bside-backend
  ECR_REPOSITORY_POCKETBASE: bside-pocketbase

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build Backend
        run: |
          ./gradlew :server:shadowJar
          docker build -t $ECR_REPOSITORY_BACKEND:staging -f server/Dockerfile .
      
      - name: Build PocketBase
        run: |
          docker build -t $ECR_REPOSITORY_POCKETBASE:staging pocketbase/
      
      - name: Push images
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        run: |
          docker tag $ECR_REPOSITORY_BACKEND:staging $ECR_REGISTRY/$ECR_REPOSITORY_BACKEND:staging
          docker tag $ECR_REPOSITORY_POCKETBASE:staging $ECR_REGISTRY/$ECR_REPOSITORY_POCKETBASE:staging
          docker push $ECR_REGISTRY/$ECR_REPOSITORY_BACKEND:staging
          docker push $ECR_REGISTRY/$ECR_REPOSITORY_POCKETBASE:staging
      
      - name: Update ECS services
        run: |
          aws ecs update-service \
            --cluster bside-staging \
            --service bside-staging-backend \
            --force-new-deployment \
            --region $AWS_REGION
          
          aws ecs update-service \
            --cluster bside-staging \
            --service bside-staging-pocketbase \
            --force-new-deployment \
            --region $AWS_REGION
      
      - name: Wait for deployment
        run: |
          aws ecs wait services-stable \
            --cluster bside-staging \
            --services bside-staging-backend bside-staging-pocketbase \
            --region $AWS_REGION
```

---

## Cost Breakdown (Staging)

| Service | Cost/Month | Notes |
|---------|-----------|-------|
| **ECS Fargate** | $50-80 | 3 tasks (2 backend + 1 pocketbase) |
| **ALB** | $20 | Standard load balancer |
| **ElastiCache** | $15 | cache.t3.micro |
| **EFS** | $5 | ~20GB storage |
| **CloudFront** | $5-10 | Light traffic |
| **S3** | $2 | Static assets |
| **CloudWatch** | $5 | Logs & metrics |
| **Data Transfer** | $5-10 | Egress |
| **Secrets Manager** | $2 | 3 secrets |
| **NAT Gateway** | $30 | Required for private subnets |
| **Route 53** | $1 | Hosted zone |
| **ECR** | $1 | Image storage |
| **TOTAL** | **~$140-180** | Per month |

**Ways to Reduce:**
- Use Fargate Spot (50% discount)
- Stop overnight (8 hours savings)
- Use t4g instances (ARM, cheaper)

---

## Rollback Plan

If deployment fails:

```bash
# Rollback to previous task definition
aws ecs update-service \
  --cluster bside-staging \
  --service bside-staging-backend \
  --task-definition bside-staging-backend:PREVIOUS_VERSION \
  --region us-east-1

# OR destroy and recreate
cd terraform/staging
terraform destroy -auto-approve
# Fix issues, then
terraform apply
```

---

## Maintenance

### Daily
- [ ] Check CloudWatch dashboard
- [ ] Review error logs
- [ ] Monitor costs

### Weekly
- [ ] Review CloudWatch Insights
- [ ] Check for security updates
- [ ] Verify backups

### Monthly
- [ ] Review and optimize costs
- [ ] Update dependencies
- [ ] Security audit

---

## Troubleshooting

### Services won't start
```bash
# Check task failures
aws ecs describe-tasks \
  --cluster bside-staging \
  --tasks $(aws ecs list-tasks --cluster bside-staging --query 'taskArns[0]' --output text) \
  --region us-east-1

# Check logs
aws logs tail /ecs/bside-staging-backend --follow
```

### High latency
```bash
# Check ALB target health
aws elbv2 describe-target-health \
  --target-group-arn $(terraform output -raw backend_target_group_arn)

# Check CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name TargetResponseTime \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average
```

---

## Next Steps

After staging is stable:

1. **Production Deployment** - Same process, different environment
2. **Blue/Green Deployments** - Zero-downtime updates
3. **Auto-Scaling** - Scale based on load
4. **Multi-Region** - Deploy to multiple regions
5. **Disaster Recovery** - Backup and restore procedures

---

**Status:** 📋 READY TO EXECUTE  
**Estimated Time:** 2-3 hours (first time), 30 min (subsequent)  
**Prerequisites:** AWS account, domain name, budget approval  
**Next:** Execute Step 1 when ready

---

**Questions?** See `.code-hq/ENTERPRISE_ROADMAP.md` for the full deployment strategy.
