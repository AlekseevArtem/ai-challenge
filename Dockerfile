# syntax=docker/dockerfile:1

# Stage 1: Build the Kotlin/Wasm application
FROM gradle:8.10-jdk17 AS builder

# Metadata
LABEL maintainer="Your Team"
LABEL description="Kotlin/Wasm Compose Multiplatform Application"
LABEL version="1.0"

WORKDIR /app

# Copy Gradle wrapper and configuration files first (these change rarely)
COPY gradle gradle
COPY gradlew* ./
COPY gradle.properties .
COPY settings.gradle.kts .

# Copy all project files (build.gradle.kts and source code)
# .dockerignore will exclude unnecessary files (build/, .gradle/, etc.)
COPY . .

# Download dependencies with cache mount (this layer will be cached)
# The cache mount persists between builds, significantly speeding up dependency downloads
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew dependencies --no-daemon --stacktrace || true

# Build the Wasm distribution with cache mount for build outputs
# This significantly speeds up incremental builds
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon --stacktrace

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy nginx configuration first
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from builder
COPY --from=builder /app/composeApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

# Set proper permissions and cleanup in one layer
RUN chmod -R 755 /usr/share/nginx/html && \
    rm -f /etc/nginx/conf.d/default.conf.default

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]