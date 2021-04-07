{
  "query": {
    "match": {
      "fields.content.auto": {
        "query": "content co",
        "analyzer": "standard"
      }
    }
  },
  "highlight": {
    "fields": {
      "fields.content.auto": {
        "fragment_size": 0,
        "number_of_fragments": 10,
        "pre_tags" : [ "%ha%" ],
        "post_tags" : [ "%he%" ]
      }
    }
  },
  "_source": ["uuid", "language"]
}