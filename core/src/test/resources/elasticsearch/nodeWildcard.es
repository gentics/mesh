{
  "query": {
      "simple_query_string" : {
          "query": "slug*",
          "analyzer": "snowball",
          "fields": ["_all"],
          "default_operator": "and"
      }
  }
}