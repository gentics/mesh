[options="header"]
|======
| Configuration                   | Type    | Default        | Description
| ```storage.directory```         | Path    | `data/graphdb` | Path to the graph database storage location.
| ```storage.backupDirectory```   | Path    | `data/backup`  | Backup directory.
| ```storage.exportDirectory```   | Path    | `data/export`  | Export directory.
| ```storage.startServer```       | Boolean | `false`        | Flag that indicates whether the graph database server component should be started. By default only an embedded graph database is used which does not start a graph server.
| ```storage.parameters```        | JSON    | -              | Additional JSON parameters that will be passed on to the used graph database implementation.
| ```storage.synchronizeWrites``` | Boolean | `true`         | Flag which controls whether write operations/transactions should be handled synchronously. This setting will be enforced when clustering is active.
| ```storage.txRetryDelay```      | Number  | `10`           | The transaction retry delay in milliseconds which is applied when retrying failed transactions due to concurrent changes. A value of 0 will disable the delay.
|======
