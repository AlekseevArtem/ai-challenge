.PHONY: help build run stop clean logs shell health test build-js run-js

# Variables
IMAGE_NAME := myapplication-web
CONTAINER_NAME := myapplication-web
PORT := 8080

# Default target
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Wasm commands (default)
build: ## Build Docker image with Wasm target
	docker-compose build

run: ## Run application with Wasm target
	docker-compose up -d
	@echo "\n✅ Application is starting at http://localhost:$(PORT)"
	@echo "⏳ Wait ~10 seconds for health check..."

dev: ## Run in development mode with live logs
	docker-compose up

stop: ## Stop running containers
	docker-compose stop

down: ## Stop and remove containers
	docker-compose down

restart: ## Restart containers
	docker-compose restart

# JS commands (fallback)
build-js: ## Build Docker image with JS target
	docker-compose -f docker-compose.js.yml build

run-js: ## Run application with JS target
	docker-compose -f docker-compose.js.yml up -d
	@echo "\n✅ Application (JS) is starting at http://localhost:$(PORT)"

dev-js: ## Run JS version in development mode
	docker-compose -f docker-compose.js.yml up

stop-js: ## Stop JS version
	docker-compose -f docker-compose.js.yml stop

down-js: ## Stop and remove JS containers
	docker-compose -f docker-compose.js.yml down

# Utility commands
logs: ## Show container logs
	docker-compose logs -f

shell: ## Open shell in running container
	docker-compose exec web sh

health: ## Check application health
	@curl -s http://localhost:$(PORT)/health || echo "❌ Application is not healthy"

clean: ## Remove all containers, images, and volumes
	docker-compose down --rmi all -v
	@echo "✅ Cleaned up Docker resources"

clean-all: ## Clean everything including build cache
	docker-compose down --rmi all -v
	docker builder prune -af
	@echo "✅ Cleaned up all Docker resources"

# Test commands
test-local: ## Test local Gradle build
	./gradlew :composeApp:wasmJsBrowserDistribution --stacktrace

test-docker: ## Test Docker build
	docker build -t $(IMAGE_NAME):test .
	@echo "✅ Docker build successful"

# Production commands
prod-build: ## Build production image
	docker build -t $(IMAGE_NAME):latest -t $(IMAGE_NAME):$$(date +%Y%m%d) .

prod-tag: ## Tag image for registry (set REGISTRY variable)
	@if [ -z "$(REGISTRY)" ]; then \
		echo "❌ Error: REGISTRY variable not set"; \
		echo "Usage: make prod-tag REGISTRY=your-registry.com"; \
		exit 1; \
	fi
	docker tag $(IMAGE_NAME):latest $(REGISTRY)/$(IMAGE_NAME):latest
	docker tag $(IMAGE_NAME):latest $(REGISTRY)/$(IMAGE_NAME):$$(date +%Y%m%d)

prod-push: ## Push image to registry (set REGISTRY variable)
	@if [ -z "$(REGISTRY)" ]; then \
		echo "❌ Error: REGISTRY variable not set"; \
		exit 1; \
	fi
	docker push $(REGISTRY)/$(IMAGE_NAME):latest
	docker push $(REGISTRY)/$(IMAGE_NAME):$$(date +%Y%m%d)

# Debug commands
inspect: ## Inspect running container
	docker-compose exec web ls -la /usr/share/nginx/html/

nginx-test: ## Test nginx configuration
	docker-compose exec web nginx -t

nginx-reload: ## Reload nginx configuration
	docker-compose exec web nginx -s reload

# Quick commands
all: build run ## Build and run

rebuild: down build run ## Rebuild and run from scratch

open: ## Open application in browser
	@command -v open >/dev/null 2>&1 && open http://localhost:$(PORT) || \
	command -v xdg-open >/dev/null 2>&1 && xdg-open http://localhost:$(PORT) || \
	echo "Please open http://localhost:$(PORT) in your browser"

# Status check
status: ## Show container status
	@echo "Container status:"
	@docker-compose ps
	@echo "\nHealth check:"
	@curl -s http://localhost:$(PORT)/health && echo " ✅" || echo " ❌"
	@echo "\nLogs (last 10 lines):"
	@docker-compose logs --tail=10