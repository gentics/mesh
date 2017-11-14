{
  "query": {
    "match": {
      "fields.content.auto": "co"
      }
  },
  "highlight": {
    "fields": {
      "fields.content.auto": {
        "fragment_size": 150,
        "number_of_fragments": 3,
        "pre_tags" : [ "%ha%" ],
        "post_tags" : [ "%he%" ]
      }
    }
  },
  "_source": ["uuid", "language"]
}