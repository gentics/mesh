{
  "query": {
    "match": {
      "fields.content.trigram": "anoth"
    }
  },
  "highlight": {
    "fields": {
      "fields.content.trigram": {
        "fragment_size": 150,
        "number_of_fragments": 3,
        "pre_tags" : [ "%hl%" ],
        "post_tags" : [ "%hl%" ]
      }
    }
  },
  "_source": ["uuid", "language"]
}