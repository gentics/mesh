# Cai Lun

## Usage 

java -jar cailun.jar

Example configuration file (cailun.json)

```
{
    "httpPort":8080,
    "storageDirectory":"/tmp/graphdb",
    "verticles":
        {
            "com.gentics.cailun.demo.verticle.CustomerVerticle":{
                "verticleConfig": null
             },
             "com.gentics.cailun.verticle.admin.AdminVerticle":{
                "verticleConfig": null
             },
             "com.gentics.cailun.core.verticle.AuthenticationVerticle":{
                "verticleConfig": null
             },
             "com.gentics.cailun.nav.NavigationVerticle":{
                "verticleConfig": null
             },
             "com.gentics.cailun.core.verticle.PageVerticle":{
                "verticleConfig": null
             },
             "com.gentics.cailun.core.verticle.TagVerticle":{
                "verticleConfig": null
             }
        }
    }
}
```

## Development

