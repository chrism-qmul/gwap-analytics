curl -X POST localhost:8083/druid/v2/?pretty -H"Content-Type:application/json"  -d'{
	"queryType": "groupBy",
		"dataSource": "finished-games",
		"granularity": "month",
		"aggregations":[{"type": "longSum", "name": "judgement-count", "fieldName": "judgements"}],
		"intervals": "1970-01-01/3000-01-01"
}'

#		"postAggregations": [{"type": "arithmetic",
#			"name": "CPJ",
#			"fn": "/",
#			"fields": [
#				{"type": "finalizingFieldAccess", "fieldName": "campaign-cost"},
#				{"type": "constant", "name": "one", "value": 1}]
#			}],

#		"postAggregations": [{"type": "arithmetic",
#			"name": "CPJ",
#			"fn": "/",
#			"fields": [
#			{
#				"type" : "extraction",
#				"dimension" : "campaign",
#				"outputName" :  "campaign-cost",
#				"extractionFn" : {
#					"type":"lookup",
#					"replaceMissingValueWith":2000.0,
#					"retainMissingValue":false,
#					"lookup":{"type": "map", "map":{"mturk":"1000.00", "ldc": "2000.0"}, "isOneToOne":false},
#				}}
#				{"type": "constant", "name": "one", "value": 1}
#			]}],

#		"postAggregations": [{"type": "arithmetic",
#			"name": "CPJ",
#			"fn": "/",
#			"fields": [{
#				"type":"lookup",
#				"dimension":"campaign",
#				"outputName":"campaign-cost",
#				"replaceMissingValueWith":2000.0,
#				"retainMissingValue":false,
#				"lookup":{"type": "map", "map":{"mturk":"1000.00", "ldc": "2000.0"}, "isOneToOne":false},
#				{"type": "constant", "name": "one", "value": 1}
#			}]}],

#		{
#				"type":"lookup",
#				"dimension":"campaign",
#				"outputName":"campaign-cost",
#				"replaceMissingValueWith":2000.0,
#				"retainMissingValue":false,
#				"lookup":{"type": "map", "map":{"mturk":"1000.00", "ldc": "2000.0"}, "isOneToOne":false}
#			}
