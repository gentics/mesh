{
  "query": {
     "match_all": { }
  },
  "script_fields": {
    "meshscript.hasPermission": {
        "script": "empty",
        "lang": "native"
    }
  }
}
