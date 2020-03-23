#!/bin/sh
apk add curl
URL="http://$DRUID_ROUTER/druid/indexer/v1/supervisor"
echo $URL
curl -X POST -H 'Content-Type: application/json' -d @/resources/druid/kafka-spec.json $URL
