SHELL := /usr/bin/env bash
ENV_FILE ?= .env

.PHONY: check bootstrap compose-config up down logs ps api-test api-run

check:
	bash scripts/check-env.sh

bootstrap:
	bash scripts/bootstrap-local.sh

compose-config:
	docker compose --env-file $(ENV_FILE) config

up:
	docker compose --env-file $(ENV_FILE) up -d

down:
	docker compose --env-file $(ENV_FILE) down

logs:
	docker compose --env-file $(ENV_FILE) logs -f

ps:
	docker compose --env-file $(ENV_FILE) ps

api-test:
	cd backend && ./mvnw test

api-run:
	cd backend && ./mvnw spring-boot:run
