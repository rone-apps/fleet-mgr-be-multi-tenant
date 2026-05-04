#!/bin/bash

# ===================================================
# WCB Fleet Corporate Backend - Docker Build Script
# ===================================================

set -e

echo "=========================================="
echo "Building WCB Fleet Corporate Backend"
echo "=========================================="

# Ensure we're in the correct directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "✓ Building from: $(pwd)"
echo "✓ Files present:"
ls -lah Dockerfile build.gradle | awk '{print "  " $NF}'

# Build Docker image
echo ""
echo "Building Docker image..."
docker build -t hpooni/fleet-manager-app-backend:wcb .

echo ""
echo "✓ Build complete!"
echo "✓ Image: hpooni/fleet-manager-app-backend:wcb"
echo ""
echo "Next steps:"
echo "  Set environment variables (see DOCKER_SETUP.md)"
echo "  docker-compose up -d"
echo ""
