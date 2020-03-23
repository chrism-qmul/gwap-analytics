# GWAP Analytics

This is an analytics system designed specifically for tracking analytics on
GWAPs

## Components

1. ZooKeeper
2. Kakfa
3. Kafka Streams (eventprocessor)
4. Dashboard

## Running

```
docker-compose up
```

once up:

```
docker-compose exec dashboard /resources/scripts/send-spec.sh
```

## Dashboard Development

```
docker-compose up
docke-compose stop dashboard
cd dashboard
DRUID_BROKER=localhost:8082 DRUID_ROUTER=localhost:8888
KAKFA_BOOTSTRAP_SERVERS=localhost:9092 ZOOKEEPER_SERVERS=localhost:2181 lein
repl
lein figwheel
lein less4j auto
```
