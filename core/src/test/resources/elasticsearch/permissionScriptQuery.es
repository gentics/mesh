{
  "query": {
    "match_all": {}
  },
  "script_fields": {
    "meshscript.hasPermission": {
      "script": "hasPermission",
      "lang": "native",
      "params": {
        "userUuid": "uuid",
        "roles": ["a", "b", "c"]
      }
    }
  }
}