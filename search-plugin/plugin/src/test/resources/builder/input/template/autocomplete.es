{
	"query": {
		"bool": {
			"must": {
				"match": {
					"fields.name": "{{{ query }}}"
				}
			},
			"should": {
				"match": {
					"fields.nickname": "{{{ query }}}"
				}
			},
			"filter": [
				{
					"term": {
						"language": "{{{ lang }}}"
					}
				},
				{
					"regexp": {
						"project.name.raw": {
							"value": "{{{ projects }}}",
							"flags": "ANYSTRING"
						}
					}
				}
			]
		}
	}
}
