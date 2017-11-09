{
  "query": {
    "match": {
      "fields.content.auto": "can po"
    }
  },
  "_source": ["fields.content", "uuid", "language"]
}