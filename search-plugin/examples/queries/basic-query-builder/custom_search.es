{
	"params": {
		"query": [ "query/match/fields.teaser.german_search" ]
	},
	"query": {
		"match": {
			"fields.teaser.german_search": "{{{ query }}}"
		}
	},
	"_source": [
		"project.name",
		"language",
		"fields.teaser"
	]
}
