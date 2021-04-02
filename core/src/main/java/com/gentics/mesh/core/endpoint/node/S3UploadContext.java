package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Context object for upload operations. The context is used to keep track of the upload data (upload, binaryUuids, storing) during the upload process.
 */
public class S3UploadContext {

	private String binaryUuid;

	private String s3ObjectKey;

	private String hash;

	private String temporaryId;

	private boolean invokeStore = false;

	public S3UploadContext() {
		this.temporaryId = UUIDUtil.randomUUID();
	}

	public void setBinaryUuid(String binaryUuid) {
		this.binaryUuid = binaryUuid;
	}

	public String getBinaryUuid() {
		return binaryUuid;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getHash() {
		return hash;
	}

	public String getTemporaryId() {
		return temporaryId;
	}

	@Setter
	public void setInvokeStore() {
		this.invokeStore = true;
	}

	public boolean isInvokeStore() {
		return this.invokeStore;
	}


	public void setS3ObjectKey(String s3ObjectKey) {
		this.s3ObjectKey = s3ObjectKey;
	}

	public String getS3ObjectKey() {
		return s3ObjectKey;
	}
}
