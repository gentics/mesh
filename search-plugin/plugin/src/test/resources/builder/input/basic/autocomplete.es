{
	"params": {
		"query": [
			"query/bool/must/match/fields.name",
			"query/bool/should/match/fields.nickname"
		],
		"lang": [ "query/bool/filter[0]/term/language" ],
		"projects": [ "query/bool/filter[1]/regexp/project.name.raw/value" ],
		"tags": [ "query/bool/filter[2]/match/fields.tags/query" ]
	},
	"query": {
		"bool": {
			"must": {
				"match": {
					"fields.name": "%query%"
				}
			},
			"should": {
				"match": {
					"fields.nickname": "%query%"
				}
			},
			"filter": [
				{
					"term": {
						"language": "%lang%"
					}
				},
				{
					"regexp": {
						"project.name.raw": {
							"value": "%projects%",
							"flags": "ANYSTRING"
						}
					}
				}
			]
		}
	}
}
