#!/bin/bash

# ============================================
# Docker Setup Script for FareFlow Backend
# ============================================

set -e

echo "========================================"
echo "🚕 FareFlow Backend - Docker Setup"
echo "========================================"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "📝 Creating .env from .env.example..."
    cp .env.example .env
    echo "✅ .env file created"
    echo ""
    echo "⚠️  IMPORTANT: Edit .env and fill in your values:"
    echo "   - MYSQL_ROOT_PASSWORD (required)"
    echo "   - SPRING_MAIL_USERNAME (required)"
    echo "   - SPRING_MAIL_PASSWORD (required - Gmail App Password)"
    echo ""
    echo "Then run this script again."
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '#' | xargs)

# Validate required variables
echo "🔍 Validating configuration..."
required_vars=("MYSQL_ROOT_PASSWORD" "SPRING_MAIL_USERNAME" "SPRING_MAIL_PASSWORD")

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Error: $var is not set in .env file"
        exit 1
    fi
done

echo "✅ All required variables are set"
echo ""

# Show configuration (masked)
echo "📋 Configuration Summary:"
echo "  Database: fareflow"
echo "  MySQL Password: ****"
echo "  Email From: ${SPRING_MAIL_USERNAME}"
echo "  Email Password: ****"
echo ""

# Ask user what to do
echo "What would you like to do?"
echo "1) Build and start services (docker-compose up)"
echo "2) Just build the Docker image"
echo "3) Just start services (already built)"
echo "4) View logs"
echo "5) Stop services"
echo ""

read -p "Enter your choice (1-5): " choice

case $choice in
    1)
        echo ""
        echo "🔨 Building Docker image..."
        docker build -t fleet-manager-be:latest .
        echo ""
        echo "🚀 Starting services with docker-compose..."
        docker-compose up -d
        echo ""
        echo "⏳ Waiting for services to be ready..."
        sleep 5
        echo ""
        echo "✅ Services started!"
        echo ""
        echo "📊 Service Status:"
        docker-compose ps
        echo ""
        echo "🌐 Application: http://localhost:8080"
        echo "🗄️  Database: localhost:3306"
        echo ""
        echo "📖 View logs with: docker-compose logs -f app"
        ;;
    2)
        echo ""
        echo "🔨 Building Docker image..."
        docker build -t fleet-manager-be:latest .
        echo ""
        echo "✅ Build complete!"
        echo ""
        echo "Next, run: docker-compose up -d"
        ;;
    3)
        echo ""
        echo "🚀 Starting services..."
        docker-compose up -d
        echo ""
        echo "✅ Services started!"
        echo ""
        echo "📊 Service Status:"
        docker-compose ps
        echo ""
        echo "🌐 Application: http://localhost:8080"
        ;;
    4)
        echo ""
        docker-compose logs -f app
        ;;
    5)
        echo ""
        echo "🛑 Stopping services..."
        docker-compose down
        echo "✅ Services stopped"
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

echo ""
