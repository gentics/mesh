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
				}
			]
		}
	}
}
