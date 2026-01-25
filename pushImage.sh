#!/bin/bash
git add .
git commit . -m "Fixed stuff"
git push
./gradlew clean build
docker buildx build \
  --platform linux/amd64 \
  -t hpooni/fleet-manager-app-backend:latest \
  --push \
  .

