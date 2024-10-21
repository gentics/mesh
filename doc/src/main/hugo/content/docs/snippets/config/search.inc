[options="header"]
|======
| Configuration                     | Type       | Default                 | Description
| ```search.url```                  | String     | `http://localhost:9200` | URL to the Elasticsearch server.
| ```search.username```             | String     | -                       | Username for basic authentication.
| ```search.password```             | String     | -                       | Password for basic authentication.
| ```search.certPath```             | String     | -                       | Path to the trusted server certificate (PEM format).
| ```search.caPath```               | String     | -                       | Path to the trusted CA certificate (PEM format).
| ```search.hostnameVerification``` | Boolean    | `true`                  | Flag to control SSL hostname verification.
| ```search.timeout```              | Number     | `60000`                 | Timeout for interactions with the search server.
| ```search.startEmbedded```        | Boolean    | `true`                  | Flag that is used to enable or disable the automatic startup and handling of the embedded Elasticsearch server.
| ```search.embeddedArguments```    | String     | Default JVM Arguments   | Set the JVM arguments for the embedded Elasticsearch server process.
| ```search.prefix```               | String     | `mesh-`                 | Elasticsearch installation prefix. Multiple Gentics Mesh installations with different prefixes can utilize the same Elasticsearch server.
| ```search.bulkLimit```            | Number     | `100`                  | Upper size limit for bulk requests.
| ```search.bulkLengthLimit```      | Number     | `5000000`               | Upper limit for the total encoded string length of the bulk requests.
| ```search.eventBufferSize```      | Number     | `1000`                  | Upper limit for mesh events that are to be mapped to elastic search requests.
| ```search.bulkDebounceTime```     | Number     | `2000`                  | The maximum amount of time in milliseconds between two bulkable requests before they are sent.
| ```search.idleDebounceTime```     | Number     | `100`                   | The maximum amount of time in milliseconds between two successful requests before the idle event is emitted.
| ```search.retryInterval```        | Number     | `5000`                  | The time in milliseconds between retries of elastic search requests in case of a failure.
| ```search.retryLimit```           | Number     | `3`                     | The amount of retries on a single request before the request is discarded.
| ```search.waitForIdle```          | Boolean    | `true`                  | If true, search endpoints wait for Elasticsearch to be idle before sending a response.
| ```search.includeBinaryFields```  | Boolean    | `true`                  | If true, the content and metadata of binary fields will be included in the search index.
| ```search.mappingMode```          | String     | `DYNAMIC`               | This setting controls the mapping mode of fields for Elasticsearch. When set to STRICT only fields which have a custom mapping will be added to Elasticsearch. Mode DYNAMIC will automatically use the Gentics Mesh default mappings which can be supplemented with custom mappings.
|======

