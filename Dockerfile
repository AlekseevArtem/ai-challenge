# 1. Builder stage
FROM gradle:8.2-jdk17 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew build --dry-run || true
COPY . .
RUN ./gradlew browserProductionWebpack --no-daemon --max-workers=1

# 2. Runner stage
FROM nginx:stable
COPY --from=builder /app/build/dist/js/productionExecutable/ /usr/share/nginx/html/
EXPOSE 80