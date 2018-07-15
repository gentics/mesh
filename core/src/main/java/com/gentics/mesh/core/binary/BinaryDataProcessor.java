package com.gentics.mesh.core.binary;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.handler.ActionContext;

import io.reactivex.Completable;
import io.vertx.ext.web.FileUpload;

/**
 * A binary data processor accepts a fileupload in order to extract specific information from the data. The found data can later be stored in the binary field
 * to be finally persisted along with the binary data.
 */
public interface BinaryDataProcessor {

	/**
	 * Check whether the processor accepts the specified content type.
	 * 
	 * @param contentType
	 * @return
	 */
	boolean accepts(String contentType);

	/**
	 * Process the binary data and store the found meta data in the binary field.
	 * 
	 * @param ac
	 * @param upload
	 * @param field
	 * @return
	 */
	Completable process(ActionContext ac, FileUpload upload, BinaryGraphField field);

}
