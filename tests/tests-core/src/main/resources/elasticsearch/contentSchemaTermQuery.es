{
    "sort": {
        "created": {
            "order": "asc"
        }
    },
    "query": {
        "bool": {
            "must": {
                "term": {
                    "schema.name.raw": "content"
                }
            }
        }
    }
}