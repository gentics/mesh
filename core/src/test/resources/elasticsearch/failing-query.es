{
	"sort": {
		"bogus": {
			"order": "asc"
		}
	},
	"query": {
		"nested": {
			"path": "tags",
			"query": {
				"match": {
					"tags.name": "Solar"
				},
				"match": {
					"tags.name": "Blue"
				}
			}
		}
	}
}