{
	"params": {
		"query": [
			"query/bool/must/multi_match/query"
		],
		"lang": [
			"query/bool/filter[0]/regexp/language/value"
		],
		"projects": [
			"query/bool/filter[1]/regexp/project.name.raw/value"
		],
		"tags": [
			"query/bool/filter[2]/match/fields.searchtags/query"
		]
	},
	"query": {
		"bool": {
			"must": {
				"multi_match": {
					"type": "best_fields",
					"query": "%query%",
					"fuzziness": "auto",
					"fields": [
						"fields.pagecontent.german_search",
						"fields.title.german_search^3",
						"fields.teaser.german_search^2",
						"fields.name.german_search"
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
							"value": "%lang%",
							"flags": "ANYSTRING"
						}
					}
				},
				{
					"regexp": {
						"project.name.raw": {
							"value": "%projects%",
							"flags": "ANYSTRING"
						}
					}
				},
				{
					"match": {
						"fields.searchtags": {
							"query": "%tags%",
							"operator": "and",
							"zero_terms_query": "all"
						}
					}
				}
			]
		}
	},
	"highlight": {
		"fields": {
			"fields.pagecontent.german_search": {
				"fragment_size": 0,
				"number_of_fragments": 0,
				"pre_tags" : [ "%hb%" ],
				"post_tags" : [ "%he%" ]
			},
			"fields.binarycontent.file.content.german_search": {
				"fragment_size": 0,
				"number_of_fragments": 0,
				"pre_tags" : [ "%hb%" ],
				"post_tags" : [ "%he%" ]
			},
			"fields.teaser.german_search": {
				"fragment_size": 0,
				"number_of_fragments": 0,
				"pre_tags" : [ "%hb%" ],
				"post_tags" : [ "%he%" ]
			},
			"fields.title.german_search": {
				"fragment_size": 0,
				"number_of_fragments": 0,
				"pre_tags" : [ "%hb%" ],
				"post_tags" : [ "%he%" ]
			}
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
