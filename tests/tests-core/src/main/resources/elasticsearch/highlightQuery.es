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
        "pre_tags" : [ "<highlight>" ],
        "post_tags" : [ "</highlight>" ]
      }
    }
  },
  "_source": ["uuid", "language"]
}