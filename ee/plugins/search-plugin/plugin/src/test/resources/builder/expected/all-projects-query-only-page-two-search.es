{
	"query": {
		"bool": {
			"must": {
				"match": {
					"fields.name": "foo"
				}
			},
			"should": {
				"match": {
					"fields.nickname": "foo"
				}
			},
			"filter": [
				{
					"term": {
						"language": "@"
					}
				},
				{
					"regexp": {
						"project.name.raw": {
							"value": "@",
							"flags": "ANYSTRING"
						}
					}
				},
				{
					"match": {
						"fields.tags": {
							"query": "",
							"operator": "and",
							"zero_terms_query": "all"
						}
					}
				}
			]
		}
	},
	"size": 8,
	"from": 16
}
