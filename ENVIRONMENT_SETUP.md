# Environment Variables Setup Guide

## Overview

All sensitive credentials (database passwords, email passwords, JWT secrets) are now managed through **environment variables** instead of being hardcoded in configuration files.

This is a **security best practice** that allows:
- ✅ Safe commits to GitHub (no passwords exposed)
- ✅ Different configurations per environment (dev, staging, production)
- ✅ Easy credential rotation
- ✅ Compliance with security standards

---

## Quick Start

### 1. Create Your Environment File

```bash
# Copy the template
cp .env.example .env

# Edit with your values
nano .env
```

### 2. Set Required Values

Edit `.env` and update:

```bash
# MySQL Database
MYSQL_ROOT_PASSWORD=your-secure-password-here

# Gmail SMTP (for sending invoices)
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password-here
```

### 3. Run with Docker

**Easiest way (Interactive Script):**
```bash
chmod +x docker-setup.sh
./docker-setup.sh
```

**Or manually:**
```bash
docker-compose up -d
```

---

## Gmail Setup (For Email Feature)

### Step 1: Enable 2-Factor Authentication
1. Go to https://myaccount.google.com/security
2. Enable "2-Step Verification"

### Step 2: Generate App Password
1. Go to https://myaccount.google.com/apppasswords
2. Select "Mail" and "Windows Computer" (or your device)
3. Google generates a 16-character password
4. Copy it exactly: `xxxx xxxx xxxx xxxx`
5. Paste into `.env`:
   ```
   SPRING_MAIL_PASSWORD=xxxx xxxx xxxx xxxx
   ```

### Step 3: Set Email Address
```bash
SPRING_MAIL_USERNAME=your-email@gmail.com
```

---

## Running Locally Without Docker

If you want to run the application without Docker:

```bash
# Set environment variables in terminal
export SPRING_MAIL_USERNAME=your-email@gmail.com
export SPRING_MAIL_PASSWORD='xxxx xxxx xxxx xxxx'
export SPRING_DATASOURCE_PASSWORD=your-db-password

# Start the application
./gradlew bootRun
```

Or create `.env.local` and source it:

```bash
# Create file
cat > .env.local << 'EOF'
export SPRING_MAIL_USERNAME=your-email@gmail.com
export SPRING_MAIL_PASSWORD='xxxx xxxx xxxx xxxx'
export SPRING_DATASOURCE_PASSWORD=your-db-password
EOF

# Source it
source .env.local

# Run application
./gradlew bootRun
```

---

## File Reference

### Files That Read Environment Variables

| File | Purpose | Variables |
|------|---------|-----------|
| `src/main/resources/application.properties` | Spring config | SPRING_MAIL_* JWT_* |
| `docker-compose.yml` | Docker services | All SPRING_* MYSQL_* |
| `Dockerfile` | Container build | (comments showing required vars) |

### Files That Should NOT Be Committed

```
.env              ← Your actual credentials (NEVER commit!)
.env.local        ← Local development file (NEVER commit!)
```

These are already in `.gitignore` ✅

### Files Safe to Commit

```
.env.example      ← Template file (safe to commit)
DOCKER_SETUP.md   ← Setup instructions
ENVIRONMENT_SETUP.md ← This file
docker-setup.sh   ← Setup script
```

---

## Verification

### Verify Docker Setup

```bash
# Check services are running
docker-compose ps

# Check application logs
docker-compose logs app | grep "EMAIL\|Started\|Application"

# Test email configuration
curl -X POST "http://localhost:8080/api/invoices/test/send-email?recipientEmail=your-email@gmail.com" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: fareflow"
```

### Verify Local Setup

```bash
# Check if environment variables are loaded
echo $SPRING_MAIL_USERNAME
echo $SPRING_DATASOURCE_PASSWORD

# View application logs
./gradlew bootRun | grep "EMAIL\|Started"
```

---

## Troubleshooting

### Problem: "Email variable is not set"

**Solution:**
```bash
# Verify .env file exists
ls -la .env

# Verify variables are loaded
grep SPRING_MAIL .env

# Make sure to source the file or use docker-compose
docker-compose up -d
```

### Problem: "Gmail authentication failed"

**Solution:**
- ✅ Make sure you're using App Password, NOT regular password
- ✅ Verify 2-Factor Authentication is enabled
- ✅ Check the password has no extra spaces: `xxxx xxxx xxxx xxxx` (exactly)
- ✅ Regenerate app password if unsure

### Problem: "Database connection failed"

**Solution:**
- ✅ Verify MYSQL_ROOT_PASSWORD is set in .env
- ✅ Verify password matches in SPRING_DATASOURCE_PASSWORD
- ✅ Wait a few seconds for MySQL to be ready: `docker-compose logs db | grep ready`

---

## Production Deployment

### Option 1: Using Environment Variables (Recommended)

```bash
docker run \
  -e MYSQL_ROOT_PASSWORD=prod-password \
  -e SPRING_DATASOURCE_PASSWORD=prod-password \
  -e SPRING_MAIL_USERNAME=prod-email@gmail.com \
  -e SPRING_MAIL_PASSWORD=prod-app-password \
  -e JWT_SECRET=prod-secret-key \
  -p 8080:8080 \
  fleet-manager-be:latest
```

### Option 2: Using Secrets Manager (AWS, Azure, GCP)

Inject secrets from your cloud provider:

```bash
# AWS Secrets Manager
aws secretsmanager get-secret-value --secret-id fleet-mgr-secrets

# Then pass to container
docker run \
  -e MYSQL_ROOT_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-password --query SecretString --output text) \
  ...
```

### Option 3: Kubernetes Secrets

```bash
kubectl create secret generic fleet-mgr-secrets \
  --from-literal=MYSQL_ROOT_PASSWORD=... \
  --from-literal=SPRING_MAIL_USERNAME=... \
  --from-literal=SPRING_MAIL_PASSWORD=...
```

---

## Security Checklist

- [ ] `.env` file is created and gitignored
- [ ] All required variables are filled in `.env`
- [ ] Gmail App Password is used (not regular password)
- [ ] Database password is strong (16+ characters)
- [ ] JWT secret is random (if changed from default)
- [ ] `.env` file is NOT committed to Git
- [ ] `.gitignore` includes `.env` and `.env.*`
- [ ] Production uses separate credentials from development

---

## Need Help?

See `DOCKER_SETUP.md` for detailed Docker instructions and troubleshooting.

---

## Summary of Changes

✅ **Removed hardcoded passwords** from `application.properties`
✅ **Added environment variable support** to `application.properties`
✅ **Updated docker-compose.yml** to use environment variables
✅ **Created .env.example** template for easy setup
✅ **Created docker-setup.sh** interactive script
✅ **Already in .gitignore** - .env files won't be committed
✅ **Created this guide** for quick reference

Your credentials are now safe! 🔐
