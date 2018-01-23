{
  "query": {
      "simple_query_string" : {
          "query": "testuser*",
          "fields": ["username.raw"],
          "default_operator": "and"
      }
  }
}