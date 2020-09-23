package com.gentics.mesh.core.binary;

import com.gentics.mesh.context.InternalActionContext;

import io.vertx.ext.web.FileUpload;

/**
 * Information required by binary processors.
 */
public class BinaryDataProcessorContext {
	private final InternalActionContext ac;
	private final String nodeUuid;
	private final String fieldName;
	private final FileUpload upload;
	private final String hash;

	public BinaryDataProcessorContext(InternalActionContext actionContext, String nodeUuid, String fieldName, FileUpload upload, String hash) {
		this.ac = actionContext;
		this.nodeUuid = nodeUuid;
		this.fieldName = fieldName;
		this.upload = upload;
		this.hash = hash;
	}

	public InternalActionContext getActionContext() {
		return ac;
	}

	public String getNodeUuid() {
		return nodeUuid;
	}

	public String getFieldName() {
		return fieldName;
	}

	public FileUpload getUpload() {
		return upload;
	}

	public String getHash() {
		return hash;
	}
}
