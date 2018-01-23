{
  "query": {
      "simple_query_string" : {
          "query": "testuser*",
          "analyzer": "snowball",
          "fields": ["username.raw"],
          "default_operator": "and"
      }
  }
}