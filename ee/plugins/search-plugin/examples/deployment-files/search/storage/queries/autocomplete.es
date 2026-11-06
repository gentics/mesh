{
	"size": 5,
	"query": {
		"bool": {
			"must": {
				"multi_match": {
					"type": "phrase_prefix",
					"query": "{{{ query }}}",
					"fields": [
						"fields.title.german_autocomplete",
						"fields.teaser.german_autocomplete",
						"fields.pagecontent.german_autocomplete",
						"fields.binarycontent.file.content.german_autocomplete"
					]
				}
			},
			"must_not": {
				"term": {
					"fields.excludeFromSearch": true
				}
			},
			"filter": [
				{
					"regexp": {
						"language": {
							"value": "{{{ lang }}}",
							"flags": "ANYSTRING"
						}
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
	},
	"highlight": {
		"pre_tags" : [ "%hb%" ],
		"post_tags" : [ "%he%" ],
		"fragment_size": 0,
		"number_of_fragments": 0,
		"fields": {
			"fields.pagecontent.german_autocomplete": {},
			"fields.binarycontent.file.content.german_autocomplete": {},
			"fields.teaser.german_autocomplete": {},
			"fields.title.german_autocomplete": {}
		}
	},
	"_source": [
		"uuid",
		"project.name",
		"language"
	]
}
