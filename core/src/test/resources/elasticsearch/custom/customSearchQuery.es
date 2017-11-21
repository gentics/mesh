{
  "query": {
    "match": {
      "fields.content.basicsearch": {
        "query": "completely ave", 
        "operator": "and"
      }
    }
  },
  "highlight": {
    "fields": {
      "fields.content.basicsearch": {
        "fragment_size": 150,
        "number_of_fragments": 3,
        "pre_tags" : [ "%hl%" ],
        "post_tags" : [ "%ha%" ]
      }
    }
  },
  "suggest": {
    "did-you-mean": {
      "text": "anoter sat of importent",
      "phrase": {
        "field": "fields.content.basicsearch",
        "max_errors": 4,
        "collate": {
           "query": {
             "inline": {
               "match_phrase": {
                 "{{field_name}}": {
                   "query": "{{suggestion}}",
                   "slop" : 1
                 }
               }
             }
           },
           "params": {
             "field_name": "fields.content.basicsearch"
           },
           "prune": true
         }
       } 
    }
  },
  "_source": ["uuid", "language", "fields.title"]
}