# Data EC2 deployment

This deployment runs Kafka, Debezium Connect, and Elasticsearch on the data EC2
instance. PostgreSQL runs on RDS, and Spring runs on ECS.

## Network assumptions

- The instance uses `gangwon-v2-data-sg`.
- TCP 29092 and 9200 are allowed only from `gangwon-v2-backend-sg`.
- TCP 8083 is bound to localhost and is not exposed through the security group.
- Elasticsearch security is disabled, so TCP 9200 must never be opened publicly.

## First start

Create `.env` beside `compose.yaml` with the instance private IPv4 address and a
new Kafka cluster ID. Do not add database credentials to `.env` or commit them.

```sh
cp .env.example .env
hostname -I
sudo docker run --rm confluentinc/cp-kafka:7.8.0 kafka-storage random-uuid
sudo docker compose config --quiet
sudo docker compose up -d --build
sudo docker compose ps
```

## Checks

```sh
curl -fsS http://localhost:9200/_cluster/health
curl -fsS http://localhost:8083/connectors
sudo docker compose logs --tail=100
```

## ECS environment

Configure the Spring container with the data EC2 private IPv4 address:

```text
SEARCH_ENGINE=elasticsearch
SEARCH_INDEXER_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=<private-ip>:29092
ELASTICSEARCH_URL=http://<private-ip>:9200
```

The Debezium connector must be registered separately after its RDS credentials
can be read securely. Never place the RDS password in this repository, terminal
history, screenshots, or chat messages.
