[options="header"]
|======
| Configuration                              | Type    | Default         | Description
| ```httpServer.port```                      | Number  | 8080            | HTTP Port number.
| ```httpServer.sslPort```                   | Number  | 8443            | HTTP Port number.
| ```httpServer.host```                      | String  | 0.0.0.0         | Host to bind mesh to
| ```httpServer.http```                      | Boolean | true            | Enable or disable HTTP server.
| ```httpServer.ssl```                       | Boolean | false           | Enable or disable HTTPS server.
| ```httpServer.corsAllowedOriginPattern```  | RegEx   | -               | Regex which will validate the origin CORS header.
| ```httpServer.scorsAllowCredentials```     | Boolean | false           | Enable CORS credential support.
| ```httpServer.enableCors```                | Boolean | false           | Enable CORS support.
| ```httpServer.certPath```                  | Path    | config/cert.pem | SSL certificate path.
| ```httpServer.keyPath```                   | Path    | config/key.pem  | SSL key path.
| ```httpServer.clientAuthMode```            | String  | none            | SSL client authentication mode. Can be none, request or require.
| ```httpServer.trustedCertPaths```          | Paths   | -               | Paths to the trusted SSL certificates.
| ```httpServer.verticleAmount```            | Number  | 2 * CPU Cores   | Amount of rest API verticles to be deployed.
|======