{
	"params": {
		"query": [ "query/bool/must/multi_match/query" ]
	},
	"size": 5,
	"query": {
		"bool": {
			"must": {
				"multi_match": {
					"type": "cross_fields",
					"fields": [
						"fields.pagecontent.auto",
						"fields.title.auto^3",
						"fields.teaser.auto^2",
						"fields.name.auto"
					],
					"analyzer": "german_search",
					"query": "{{{ query }}}",
					"operator": "and"
				}
			},
			"must_not": {
				"term": {
					"fields.excludeFromSearch": true
				}
			}
		}
	},
	"highlight": {
		"fragment_size": 0,
		"number_of_fragments": 50,
		"pre_tags": [
			"%hb%"
		],
		"post_tags": [
			"%he%"
		],
		"fields": {
			"fields.pagecontent.auto": {},
			"fields.title.auto": {},
			"fields.teaser.auto": {},
			"fields.name.auto": {}
		}
	},
	"_source": [
		"uuid",
		"project.name",
		"language"
	]
}
