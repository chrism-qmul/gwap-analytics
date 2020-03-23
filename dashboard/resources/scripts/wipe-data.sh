#!/bin/sh
apk add curl
curl -XPOST -H 'Content-Type:application/json' -d '{ "interval" : "1970-01-01T00:00:00.000Z/3000-01-01T00:00:00.000Z" }' http://coordinator:8081/druid/coordinator/v1/datasources/finished-games/markUnused
curl -XPOST -H'Content-Type:application/json' -d '{"type": "kill", "dataSource": "finished-games", "interval": 1970-01-01/3000-01-01"}' http://coordinator:8081/druid/indexer/v1/task
