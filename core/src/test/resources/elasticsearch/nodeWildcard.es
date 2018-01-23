{
  "query": {
      "simple_query_string" : {
          "query": "slug*",
          "fields": ["fields.slug.raw"],
          "default_operator": "and"
      }
  }
}