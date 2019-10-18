[options="header"]
|======
| Configuration      | Type   | Default                                                                  | Description
| ```logEnabled```   | Flag   | true                                                                     | Enables the debug info log.
| ```logFolder```    | String | `debuginfo`                                                              | The path of the folder where the debug info log is stored.
| ```logFileSize```  | String | `5MB`                                                                    | The maximum file size of a single log file.
| ```logPattern```   | String | `%d{HH:mm:ss.SSS} [%meshName] %-5level [%thread] [%file:%line] - %msg%n` | The pattern used for each log line.
|======
