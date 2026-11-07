{
	"params": {
		"query": [
			"suggest/text"
		]
	},
	"suggest": {
		"text": "%query%",
		"did-you-mean-filename": {
			"phrase": {
				"field": "fields.name.suggest",
				"max_errors": 2
			}
		},
		"did-you-mean-binarycontent": {
			"phrase": {
				"field": "fields.binarycontent.file.content.suggest",
				"max_errors": 2
			}
		}
	}
}
