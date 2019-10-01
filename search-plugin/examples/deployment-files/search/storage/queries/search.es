{
	"query": {
		"bool": {
			"must": {
				"multi_match": {
					"type": "best_fields",
					"query": "{{{ query }}}",
					"fuzziness": "auto",
					"fields": [
						"fields.title.german_search^3",
						"fields.teaser.german_search^2",
						"fields.pagecontent.german_search",
						"fields.binarycontent.file.content.german_search"
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
				},
				{
					"match": {
						"fields.searchtags": {
							"query": "{{{ tags }}}",
							"operator": "and",
							"zero_terms_query": "all"
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
			"fields.pagecontent.german_search": {},
			"fields.binarycontent.file.content.german_search": {},
			"fields.teaser.german_search": {},
			"fields.title.german_search": {}
		}
	},
	"aggregations": {
		"searchtags": {
			"terms": {
				"field": "fields.searchtags.taglist",
				"size": 9999,
				"min_doc_count": 1
			}
		}
	},
	"_source": [
		"project.name",
		"uuid",
		"language",
		"fields.title",
		"fields.binarycontent.filename"
	]
}
