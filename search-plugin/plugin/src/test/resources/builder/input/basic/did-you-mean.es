{
	"params": {
		"query": [ "suggest/text" ],
		"lang": [ "invalid.but.still.tested/language" ],
		"projects": [ "invalid.but.still.tested/projects" ]
	},
	"suggest": {
		"text": "%query%",
		"did-you-mean": {
			"match": {
				"field": "fields.content"
			}
		}
	},
	"invalid.but.still.tested": {
		"language": "%lang%",
		"projects": "%projects%"
	}
}
