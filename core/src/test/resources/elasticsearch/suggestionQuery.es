{
  "suggest": {
    "my-suggestion": {
      "text": "anoter set of importent",
      "phrase": {
        "field": "fields.content.suggest",
        "max_errors": 2,
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
             "field_name": "fields.content.suggest"
           },
           "prune": true
         }
       } 
    }
  }
}