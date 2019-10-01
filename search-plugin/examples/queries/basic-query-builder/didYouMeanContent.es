{
	"params": {
		"query": [
			"suggest/text"
		]
	},
	"suggest": {
		"text": "%query%",
		"did-you-mean-name": {
			"phrase": {
				"field": "fields.name.suggest",
				"max_errors": 2
			}
		},
		"did-you-mean-title": {
			"phrase": {
				"field": "fields.title.suggest",
				"max_errors": 2
			}
		},
		"did-you-mean-teaser": {
			"phrase": {
				"field": "fields.teaser.suggest",
				"max_errors": 2
			}
		},
		"did-you-mean-pagecontent": {
			"phrase": {
				"field": "fields.pagecontent.suggest",
				"max_errors": 2
			}
		}
	}
}
