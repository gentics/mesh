{
	"query": {
		"nested": {
			"path": "tagFamilies.colors.tags",
			"query": {
				"match": {
					"tagFamilies.colors.tags.name": "red"
				}
			}
		}
	}
}