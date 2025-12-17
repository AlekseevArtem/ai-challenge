# syntax=docker/dockerfile:1

# Stage 1: Build the Kotlin/Wasm application
FROM gradle:8.10-jdk17 AS builder

WORKDIR /app

# Copy Gradle wrapper and configuration files first (these change rarely)
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY gradle/libs.versions.toml gradle/libs.versions.toml

# Copy all build.gradle.kts files (needed for dependency resolution)
COPY build.gradle.kts .
COPY shared/build.gradle.kts shared/
COPY composeApp/build.gradle.kts composeApp/
COPY core-common/build.gradle.kts core-common/
COPY core-network/build.gradle.kts core-network/
COPY feature-main/build.gradle.kts feature-main/
COPY feature-welcome/build.gradle.kts feature-welcome/

# Download dependencies with cache mount (this layer will be cached)
# The cache mount persists between builds, significantly speeding up dependency downloads
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew dependencies --no-daemon --stacktrace || true

# Copy all source code (this changes frequently, so it's last)
COPY shared shared
COPY core-common core-common
COPY core-network core-network
COPY feature-main feature-main
COPY feature-welcome feature-welcome
COPY composeApp composeApp

# Build the Wasm distribution with cache mount for build outputs
# This significantly speeds up incremental builds
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon --stacktrace

# Stage 2: Serve with Nginx
FROM nginx:1.25-alpine

# Copy built files from builder
COPY --from=builder /app/composeApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

# Set proper permissions
RUN chmod -R 755 /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf.default 2>/dev/null || true

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]