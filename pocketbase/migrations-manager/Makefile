.PHONY: help install build test lint format clean status migrate up down create

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

install: ## Install dependencies
	npm install

build: ## Build the project
	npm run build

test: ## Run tests
	npm test

lint: ## Lint code
	npm run lint

format: ## Format code
	npm run format

clean: ## Clean build artifacts
	rm -rf dist coverage node_modules

status: ## Check migration status
	npm run migrate:status

migrate: ## Run all pending migrations
	npm run migrate

up: ## Run next pending migration
	npm run migrate:up

down: ## Rollback last batch
	npm run migrate:down

create: ## Create new migration (usage: make create NAME="migration name")
	@if [ -z "$(NAME)" ]; then \
		echo "Error: NAME is required. Usage: make create NAME=\"migration name\""; \
		exit 1; \
	fi
	npm run migrate:create "$(NAME)"

dev: ## Run in development mode
	npm run dev
