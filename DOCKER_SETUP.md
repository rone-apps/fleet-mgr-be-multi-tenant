# Docker Setup Guide - FareFlow Backend

## Quick Start

### 1. Create Environment File

Copy the example environment file and fill in your values:

```bash
cp .env.example .env
```

Edit `.env` and add your values:
```
MYSQL_ROOT_PASSWORD=your-secure-password
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

### 2. Build Docker Image

```bash
docker build -t fleet-manager-be:latest .
```

### 3. Start Services

```bash
docker-compose up -d
```

Application will be available at: `http://localhost:8080`

---

## Configuration

### Email Setup (Gmail)

1. **Enable 2-Factor Authentication** on your Google account
2. **Generate App Password**:
   - Go to https://myaccount.google.com/apppasswords
   - Select "Mail" and your device
   - Copy the 16-character password
   - Paste into `.env` as `SPRING_MAIL_PASSWORD`

3. **Example .env**:
```bash
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=xxxx xxxx xxxx xxxx
SPRING_MAIL_SENDER_NAME=FareFlow - Yellow Cabs Newyork
```

### Database Setup

The Docker Compose automatically creates MySQL and initializes the database:

```bash
MYSQL_ROOT_PASSWORD=your-secure-password
MYSQL_DATABASE=fareflow
```

### JWT Configuration (Optional)

Customize JWT tokens:
```bash
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000  # 24 hours in milliseconds
```

---

## File Structure

```
.env                    # Environment variables (DO NOT COMMIT)
.env.example           # Template file (safe to commit)
docker-compose.yml     # Docker Compose configuration
Dockerfile            # Docker build instructions
```

---

## Running Different Scenarios

### Development (Local with Database)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Development (Against Local MySQL)

```bash
# Start only database
docker-compose up -d db

# Run application locally
./gradlew bootRun
```

### Production

```bash
# Use environment variables instead of .env file
docker run \
  -e MYSQL_ROOT_PASSWORD=prod-password \
  -e SPRING_DATASOURCE_PASSWORD=prod-password \
  -e SPRING_MAIL_USERNAME=prod-email@gmail.com \
  -e SPRING_MAIL_PASSWORD=prod-app-password \
  -e JWT_SECRET=prod-secret \
  -p 8080:8080 \
  fleet-manager-be:latest
```

### Kubernetes/Docker Swarm

Create a Kubernetes secret:

```bash
kubectl create secret generic fleet-mgr-secrets \
  --from-literal=MYSQL_ROOT_PASSWORD=your-password \
  --from-literal=SPRING_MAIL_USERNAME=your-email@gmail.com \
  --from-literal=SPRING_MAIL_PASSWORD=your-app-password \
  --from-literal=JWT_SECRET=your-secret
```

Then reference in deployment:
```yaml
env:
  - name: MYSQL_ROOT_PASSWORD
    valueFrom:
      secretKeyRef:
        name: fleet-mgr-secrets
        key: MYSQL_ROOT_PASSWORD
```

---

## Troubleshooting

### Email Not Sending

Check logs:
```bash
docker-compose logs app | grep EMAIL
```

Common issues:
- **Gmail password wrong**: Use App Password, not regular password
- **SMTP blocked**: Make sure port 587 is not blocked by firewall
- **Authentication failed**: Enable 2FA and generate new app password

### Database Connection Failed

Check MySQL is healthy:
```bash
docker-compose logs db
```

Verify credentials in `.env` match

### Port Already in Use

Change ports in `docker-compose.yml`:
```yaml
ports:
  - "3307:3306"  # MySQL on 3307 instead of 3306
  - "8081:8080"  # App on 8081 instead of 8080
```

---

## Security Best Practices

✅ **Never commit .env file** - Already ignored by .gitignore
✅ **Use strong passwords** - Especially for database and JWT secret
✅ **Use Gmail App Password** - Not your regular Gmail password
✅ **Rotate secrets regularly** - Especially in production
✅ **Use environment variables** - Never hardcode credentials

---

## Useful Docker Commands

```bash
# View container status
docker-compose ps

# View application logs
docker-compose logs app

# View database logs
docker-compose logs db

# View logs in real-time
docker-compose logs -f

# Connect to MySQL in container
docker-compose exec db mysql -h db -u root -p

# Restart services
docker-compose restart

# Stop services
docker-compose stop

# Remove services and volumes
docker-compose down -v

# Rebuild image and restart
docker-compose up -d --build
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| MYSQL_ROOT_PASSWORD | ✅ Yes | - | MySQL root user password |
| MYSQL_ROOT_USERNAME | ❌ No | root | MySQL root username |
| MYSQL_DATABASE | ❌ No | fareflow | Database name |
| SPRING_DATASOURCE_URL | ❌ No | auto | Database connection URL |
| SPRING_DATASOURCE_USERNAME | ❌ No | root | Database username |
| SPRING_DATASOURCE_PASSWORD | ✅ Yes (same as MYSQL_ROOT_PASSWORD) | - | Database password |
| SPRING_JPA_HIBERNATE_DDL_AUTO | ❌ No | update | Hibernate DDL strategy |
| SPRING_MAIL_USERNAME | ✅ Yes | - | Gmail email address |
| SPRING_MAIL_PASSWORD | ✅ Yes | - | Gmail App Password |
| SPRING_MAIL_SENDER_NAME | ❌ No | FareFlow - Yellow Cabs Newyork | Email sender display name |
| JWT_SECRET | ❌ No | embedded | JWT signing secret |
| JWT_EXPIRATION | ❌ No | 86400000 | Token expiration (ms) |

---

## Next Steps

1. ✅ Copy `.env.example` to `.env`
2. ✅ Fill in your Gmail credentials
3. ✅ Fill in a secure MySQL password
4. ✅ Run `docker-compose up -d`
5. ✅ Verify app is running: `docker-compose logs app`
6. ✅ Test email: Send an invoice and it should arrive in your inbox

---

## Support

For issues:
1. Check logs: `docker-compose logs app`
2. Verify .env file has all required values
3. Ensure Docker daemon is running
4. Check port availability (8080, 3306)
