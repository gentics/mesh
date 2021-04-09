package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.annotation.Setter;

/**
 * Context object for upload operations of a S3 Binary Field. The context is used to keep track of the upload data (upload, binaryUuids, storing) during the upload process.
 */
public class S3UploadContext {

	private String s3binaryUuid;

	private String s3ObjectKey;

	private String fileName;

	public S3UploadContext() {
	}

	@Setter
	public void setS3BinaryUuid(String s3binaryUuid) {
		this.s3binaryUuid = s3binaryUuid;
	}

	public String getS3BinaryUuid() {
		return s3binaryUuid;
	}

	@Setter
	public void setS3ObjectKey(String s3ObjectKey) {
		this.s3ObjectKey = s3ObjectKey;
	}


	public String getS3ObjectKey() {
		return s3ObjectKey;
	}

	@Setter
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}
}
