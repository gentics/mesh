[options="header"]
|======
| Configuration                   | Type             | Default                 | Description
| ```upload.byteLimit```          | Number           | `262144000` (250 MB)    | Upload limit in bytes.
| ```upload.directory```          | Path             | `data/binaryFiles`      | Filesystem directory for uploaded binary data.
| ```upload.tempDirectory```      | Path             | `data/tmp/file-uploads` | Temporary directory for uploaded binary data. Finished files will be moved to the upload directory.
| ```upload.parser```             | Boolean          | `true`                  | Controls whether the upload document parser should be enabled. The parser is responsible for extracting metadata and plain text from uploads. Disabling the parser will reduce CPU and memory usage during upload processing.
| ```upload.parserLimit```        | Number           | `40000`                 | Controls the parser limit. The parser will stop extracting plain contents when this limit has been reached. Please note that finding all inline annotations in PDF documents requires the parser limit to be set to -1 to fully parse the document.
| ```upload.metadataWhitelist```  | List of strings  | null                    | If set, the parser will only extract metadata with the keys specified in the list.
|======
