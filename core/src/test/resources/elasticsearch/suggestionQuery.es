{
  "suggest": {
    "query-suggest" : {
       "text" : "som", 
        "completion" : { 
             "field" : "fields.content.suggest"
        }
      }
  },
  "_source": ["fields.content", "uuid", "language"]
}
