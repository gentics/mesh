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
                            "lat" : 50.0,
                            "lon" : 10.0
                        },
                        "bottom_right" : {
                            "lat" : -40.0,
                            "lon" : 19.0
                        }
                    }
                }
            }
        }
    }
}