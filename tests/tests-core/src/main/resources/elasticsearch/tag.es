{
  "query": {
    "nested": {
      "path": "tags",
      "query": {
        "bool": {
          "must": {
            "match_phrase": {
              "tags.name": "Twinjet"
            }
          }
        }
      }
    }
  }
}