# WCB Fleet Corporate - Docker Deployment Guide

This docker-compose configuration runs the WCB (Workers' Compensation Board) service as a separate multi-tenant instance using a shared MySQL database.

## Quick Start

### 1. Set Environment Variables on Host

```bash
# MySQL Configuration (shared RDS instance, different schema)
export MYSQL_HOST="rone2.ci2owkd8ejjp.us-west-2.rds.amazonaws.com"
export MYSQL_USER="hpooni1"
export MYSQL_PASSWORD="!Inder101"

# API Keys (required for features)
export ANTHROPIC_API_KEY="sk-ant-xxxxxxxxxxxxx"
export NEW_RELIC_LICENSE_KEY="nrpe-xxxxxxxxxxxxx"
```

### 2. Build Docker Images

**Backend:**
```bash
cd /path/to/fleet-mgr-be-multi-tenant
docker build -t hpooni/fleet-manager-app-backend:wcb .
```

**Frontend:**
```bash
cd /path/to/fleet-mgr-app-multi-tenant
docker build -t hpooni/fleet-manager-app-frontend:wcb .
cd ../fleet-mgr-be-multi-tenant
```

### 3. Start Services

```bash
# Start both backend and frontend
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Check status
docker-compose ps
```

### 4. Access the Service

- **Backend API**: http://localhost:8082
- **Frontend UI**: http://localhost:3002

## Configuration Details

### Database

- **Host**: `${MYSQL_HOST}` (environment variable from host)
- **User**: `${MYSQL_USER}` (environment variable from host)
- **Password**: `${MYSQL_PASSWORD}` (environment variable from host)
- **Schema**: `fleet_corporate` (separate from main instance)
- **Connection URL**: `jdbc:mysql://${MYSQL_HOST}:3306/fleet_corporate`

### Port Mapping

| Service | Internal Port | External Port | Endpoint |
|---------|---------------|---------------|----------|
| Backend | 8080 | 8082 | http://localhost:8082 |
| Frontend | 3000 | 3002 | http://localhost:3002 |

### Container Names

- Backend: `wcb-fleet-corporate-backend`
- Frontend: `wcb-fleet-corporate-frontend`

## Troubleshooting

### Database Connection Fails

```bash
# Verify environment variables are set
env | grep -E "MYSQL_|ANTHROPIC_API_KEY|NEW_RELIC_LICENSE_KEY"

# Test MySQL connectivity from host
mysql -h $MYSQL_HOST -u $MYSQL_USER -p

# Check if fleet_corporate schema exists
mysql -h $MYSQL_HOST -u $MYSQL_USER -p -e "SHOW DATABASES;"
```

### Containers Won't Start

```bash
# View detailed logs
docker-compose logs backend
docker-compose logs frontend

# Check if images exist
docker images | grep fleet-manager

# Rebuild images
docker-compose down -v
docker-compose up --build
```

### API Not Responding

```bash
# Test backend health
curl http://localhost:8082/actuator/health

# Test frontend
curl http://localhost:3002
```

### Database Schema Missing

The `fleet_corporate` schema must be created beforehand. Run this on the shared MySQL instance:

```sql
CREATE SCHEMA fleet_corporate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Stopping Services

```bash
# Stop all containers
docker-compose stop

# Stop and remove containers (data persists in RDS)
docker-compose down

# Completely remove everything including volumes
docker-compose down -v
```

## Logs

```bash
# Tail backend logs
docker-compose logs -f backend

# Tail frontend logs
docker-compose logs -f frontend

# View all logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100 backend
```

## Important Notes

- The MySQL instance is **shared** with other applications
- Each application uses a **separate schema** (`fleet_corporate` for WCB)
- Environment variables must be set **before** running `docker-compose`
- The `.env` file is **not required** if environment variables are exported on the host
- See `.env.example` for a template of required variables
