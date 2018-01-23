{
  "query": {
      "simple_query_string" : {
          "query": "testtag*",
          "fields": ["name.raw^5"],
          "default_operator": "and"
      }
  }
}