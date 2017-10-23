{
  "query": {
      "simple_query_string" : {
          "query": "testuser111235*",
          "analyzer": "snowball",
          "fields": ["name^5","_all"],
          "default_operator": "and"
      }
  }
}