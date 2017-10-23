{
  "query": {
      "simple_query_string" : {
          "query": "testtag*",
          "analyzer": "snowball",
          "fields": ["name^5","_all"],
          "default_operator": "and"
      }
  }
}