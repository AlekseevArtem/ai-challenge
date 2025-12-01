# 1. Builder stage
FROM gradle:8.2-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle browserProductionWebpack --no-daemon

# 2. Runner stage
FROM nginx:stable
COPY --from=builder /app/build/dist/js/productionExecutable/ /usr/share/nginx/html/
EXPOSE 80