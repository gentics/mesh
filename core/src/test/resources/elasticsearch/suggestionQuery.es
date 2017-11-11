{
  "query" : {
    "match_phrase_prefix" : {
      "fields.content.suggest": {
         "query": "Some",
         "slop":  10,
         "max_expansions": 50
       }
    }
  },
  "highlight" : {
    "fields" : {
      "fields.content.suggest" : {
        "number_of_fragments" : 3,
        "fragment_size" : 150,
        "pre_tags" : [ "<highlight>" ],
        "post_tags" : [ "</highlight>" ]
      }
    }
  },
  "_source": [
    "fields.content",
    "uuid",
    "language"
  ]
}