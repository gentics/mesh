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
				}
			]
		}
	}
}
