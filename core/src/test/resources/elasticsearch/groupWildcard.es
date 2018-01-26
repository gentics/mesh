{
  "query": {
      "simple_query_string" : {
          "query": "testgroup*",
          "fields": ["name.raw^5"],
          "default_operator": "and"
      }
  }
}