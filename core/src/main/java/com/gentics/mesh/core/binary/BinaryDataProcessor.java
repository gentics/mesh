package com.gentics.mesh.core.binary;

import java.util.function.Consumer;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;

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
	 * @param upload
	 * @return consumer which can update the field
	 */
	Consumer<BinaryGraphField> process(FileUpload upload);

}
