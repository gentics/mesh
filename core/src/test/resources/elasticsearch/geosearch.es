{
    "query": {
        "bool" : {
            "must" : {
                "match_all" : {}
            },
            "filter" : {
                "geo_bounding_box" : {
                    "fields.binary.metadata.location" : {
                        "top_left" : {
                            "lat" : 15.0,
                            "lon" : 30.0
                        },
                        "bottom_right" : {
                            "lat" : 12.0,
                            "lon" : 60.0
                        }
                    }
                }
            }
        }
    }
}