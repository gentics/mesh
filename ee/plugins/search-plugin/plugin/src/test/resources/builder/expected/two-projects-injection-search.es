{
	"query": {
		"bool": {
			"must": {
				"match": {
					"fields.name": "{ \"this is bad\": \"foo bar \" }"
				}
			},
			"should": {
				"match": {
					"fields.nickname": "{ \"this is bad\": \"foo bar \" }"
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
							"value": "one|two",
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
	"from": 0
}
