{
  "query": {
      "simple_query_string" : {
          "query": "testuser*",
          "analyzer": "snowball",
          "fields": ["name^5","_all"],
          "default_operator": "and"
      }
  }
}