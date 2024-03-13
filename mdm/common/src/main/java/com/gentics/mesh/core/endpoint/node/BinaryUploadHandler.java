package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.context.InternalActionContext;

import io.vertx.core.MultiMap;

/**
 * Handler which contains field API specific request handlers.
 */
public interface BinaryUploadHandler {

	/**
	 * Handle a request to create a new field.
	 *
	 * @param ac
	 * @param nodeUuid
	 *            UUID of the node which should be updated
	 * @param fieldName
	 *            Name of the field which should be created
	 * @param attributes
	 *            Additional form data attributes
	 */
	void handleUpdateField(InternalActionContext ac, String nodeUuid, String fieldName, MultiMap attributes);

	/**
	 * Update the check status field of the specified binary field with the
	 * status from the JSON in the request body.
	 *
	 * @param ac The current request.
	 * @param nodeUuid The nodes UUID.
	 * @param fieldName The binary field name.
	 */
	void handleBinaryCheckResult(InternalActionContext ac, String nodeUuid, String fieldName);
}
