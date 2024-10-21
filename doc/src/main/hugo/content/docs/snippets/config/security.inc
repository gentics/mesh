[options="header"]
|======
| Configuration                        | Type    | Default                 | Description
| ```security.tokenExpirationTime```   | Number  | `3600` (1h)             | The JWT expiration timeout in seconds.
| ```security.keystorePath```          | Path    | `config/keystore.jceks` | Path to Java keystore file.
| ```security.keystorePassword```      | String  | -                       | Password for the Java keystore file.
| ```security.algorithm```             | String  | `HS256`                 | Hashing algorithm used to sign, verify and generate tokens.
| ```security.enableAnonymousAccess``` | Boolean | `true`                  | Flag to be used to enable the anonymous access feature.
|======
