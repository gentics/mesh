package com.gentics.mesh.core.s3binary;

import com.gentics.mesh.context.InternalActionContext;
import io.vertx.ext.web.FileUpload;

/**
 * Information required by binary processors.
 */
public class S3BinaryDataProcessorContext {

	private final InternalActionContext ac;
	private final String nodeUuid;
	private final String fieldName;
	private final FileUpload upload;

	public S3BinaryDataProcessorContext(InternalActionContext actionContext, String nodeUuid, String fieldName, FileUpload upload) {
		this.ac = actionContext;
		this.nodeUuid = nodeUuid;
		this.fieldName = fieldName;
		this.upload = upload;
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
}
