[options="header"]
|======
| Configuration                              | Type    | Default        | Description
| ```httpServer.port```                      | Number  | 8080           | HTTP Port number.
| ```httpServer.host```                      | String  | 0.0.0.0        | Host to bind mesh to
| ```httpServer.ssl```                       | Boolean | false          | Enable or disable SSL support.
| ```httpServer.corsAllowedOriginPattern```  | RegEx   | -              | Regex which will validate the origin CORS header.
| ```httpServer.scorsAllowCredentials```     | Boolean | false          | Enable CORS credential support.
| ```httpServer.enableCors```                | Boolean | false          | Enable CORS support.
| ```httpServer.certPath```                  | Path    | -              | SSL certificate path.
| ```httpServer.keyPath```                   | Path    | -              | SSL key path.
| ```httpServer.verticleAmount```            | Number  | 2 * CPU Cores  | Amount of rest API verticles to be deployed.
|======