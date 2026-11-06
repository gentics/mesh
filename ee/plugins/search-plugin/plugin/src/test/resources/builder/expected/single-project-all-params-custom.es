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
						"language": "bar"
					}
				},
				{
					"regexp": {
						"project.name.raw": {
							"value": "dummy",
							"flags": "ANYSTRING"
						}
					}
				},
				{
					"match": {
						"fields.tags": {
							"query": "foo bar doo",
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
