[options="header"]
|======
| Configuration               | Type   | Default               | Description
| ```upload.byteLimit```      | Number | `262144000` (250 MB)  | Upload limit in bytes.
| ```upload.directory```      | Path   | `data/binaryFiles`    | Filesystem directory for uploaded binary data.
| ```upload.tempDirectory```  | Path   | `data/tmp/file-uploads` | Temporary directory for uploaded binary data. Finished files will be moved to the upload directory.
|======
