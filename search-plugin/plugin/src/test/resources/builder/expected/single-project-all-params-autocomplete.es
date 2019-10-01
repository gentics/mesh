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
				}
			]
		}
	}
}
