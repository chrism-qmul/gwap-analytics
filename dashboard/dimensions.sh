curl -X POST localhost:8083/druid/v2/?pretty -H"Content-Type:application/json"  -d'
{
	"queryType": "topN",
	"dataSource": "finished-games",
	"dimension": "game",
	"threshold": 1000,
	"metric": "judgements",
	"granularity": "all",
	"aggregations": [{
	"type": "longSum",
	"name": "judgements",
	"fieldName": "count"
}],
"context": {"timeout": 1000},
"intervals": [ "1900-08-31T00:00:00.000/3000-09-03T00:00:00.000" ]
}'
