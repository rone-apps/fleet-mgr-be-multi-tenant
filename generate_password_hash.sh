#!/bin/bash
# Generate BCrypt password hash for Yellow Cabs admin user

cd "$(dirname "$0")"

echo "Generating BCrypt password hashes..."
echo ""

./mvnw -q exec:java -Dexec.mainClass="com.taxi.util.PasswordHashGenerator" -Dexec.classpathScope=test
