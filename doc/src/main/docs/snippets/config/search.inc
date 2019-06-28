[options="header"]
|======
| Configuration                     | Type       | Default                 | Description
| ```search.url```                  | String     | `http://localhost:9200` | URL to the Elasticsearch server.
| ```search.username```             | String     | -                       | Username for basic authentication.
| ```search.password```             | String     | -                       | Password for basic authentication.
| ```search.certPath```             | String     | -                       | Path to the trusted server certificate (PEM format).
| ```search.caPath```               | String     | -                       | Path to the trusted CA certificate (PEM format).
| ```search.hostnameVerification``` | Boolean    | `true`                  | Flag to control SSL hostname verification.
| ```search.timeout```              | Number     | `3000`                  | Timeout for interactions with the search server.
| ```search.startEmbedded```        | Boolean    | `true`                  | Flag that is used to enable or disable the automatic startup and handling of the embedded Elasticsearch server.
| ```search.embeddedArguments```    | String     | Default JVM Arguments   | Set the JVM arguments for the embedded Elasticsearch server process.
| ```search.bulkLimit```            | Number     | `2000`                  | Upper size limit for bulk requests.
| ```search.prefix```               | String     | `mesh-`                 | Elasticsearch installation prefix. Multiple Gentics Mesh installations with different prefixes can utilize the same Elasticsearch server.
|======

