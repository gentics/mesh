[options="header"]
|======
| Configuration                     | Type     | Default  | Description
| ```updateCheck```                 | Flag     | true     | An update check to the Gentics Mesh update server will be invoked during startup if this flag is set to true.
| ```defaultLanguage```             | String   | en       | Default language which serves as a fallback when no language has been specified within a request.
| ```tempDirectory```               | Path     | data/tmp | Path to the main temporary filesystem directory.
| ```languagesFilePath```           | Path     | -        | Optional path to a JSON file containing additional languages.
| ```pluginDirectory```             | Path     | plugins  | Path to the plugin directory.
| ```pluginTimeout```               | Number   | 120      | Timeout in seconds which is used for the plugin startup, initialization, de-initialization and stop processes.
| ```pluginUseHttp2```              | Flag     | false    | If true, plugin HTTP client will be forced to use HTTP/2 protocol version.
| ```nodeName```                    | String   | -        | Name of the cluster node instance. If not specified a name will be generated.
| ```startInReadOnly```             | Flag     | false    | If true, Gentics Mesh will be started in read only mode.
| ```versionPurgeMaxBatchSize```    | Number   | 10       | The maximum amount of node versions that are purged before the database transaction is committed.
| ```migrationMaxBatchSize```       | Number   | 50       | The maximum amount of entities to be migrated in a single transaction. This setting affects schema, microschema and branch migrations.
| ```migrationTriggerInterval```    | Number   | 60000    | Interval in ms for the automatic migration job trigger. Setting this to a non-positive value will disable automatic job triggering.
|======

NOTE: The password of the initial admin user cannot be set in the `mesh.yml`. Use the environment variables `MESH_INITIAL_ADMIN_PASSWORD` and `MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET` instead (see <<env>>).
