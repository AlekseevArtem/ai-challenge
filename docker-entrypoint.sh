#!/bin/bash
set -e

# Fix Docker socket permissions
if [ -S /var/run/docker.sock ]; then
    echo "Fixing Docker socket permissions..."
    DOCKER_SOCK_GID=$(stat -c '%g' /var/run/docker.sock 2>/dev/null || echo "999")
    echo "Docker socket group ID: $DOCKER_SOCK_GID"

    # Add appuser to the correct docker group
    if ! getent group $DOCKER_SOCK_GID > /dev/null 2>&1; then
        groupadd -g $DOCKER_SOCK_GID docker-host
    fi
    usermod -aG $DOCKER_SOCK_GID appuser

    echo "Docker socket permissions fixed"
fi

# Switch to appuser and run the application
exec gosu appuser "$@"
