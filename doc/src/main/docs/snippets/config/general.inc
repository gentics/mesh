[options="header"]
|======
| Configuration                     | Type     | Default  | Description
| ```updateCheck```                 | Flag     | true     | An update check to the Gentics Mesh update server will be invoked during startup if this flag is set to true.
| ```defaultLanguage```             | String   | en       | Default language which serves as a fallback when no language has been specified within a request.
| ```verticles```                   | List     | -        | List of Vert.x java verticle classes which will be loaded during startup.
| ```tempDirectory```               | Path     | data/tmp | Path to the main temporary filesystem directory.
| ```languagesFilePath```           | Path     | -        | Optional path to a JSON file containing additional languages.
| ```startInReadOnly```             | Flag     | false    | If true, Gentics Mesh will be started in read only mode.
| ```versionPurgeMaxBatchSize```    | Number   | 10       | The maximum amount of node versions that are purged before the database transaction is committed.
|======
