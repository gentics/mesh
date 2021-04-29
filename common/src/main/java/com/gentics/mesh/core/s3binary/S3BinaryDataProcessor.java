package com.gentics.mesh.core.s3binary;

import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import io.reactivex.Maybe;

import java.util.function.Consumer;

/**
 * A S3 binary data processor accepts a fileupload in order to extract specific information from the data. The found data can later be stored in the binary field
 * to be finally persisted along with the binary data.
 */
public interface S3BinaryDataProcessor {

	/**
	 * Check whether the processor accepts the specified content type.
	 * 
	 * @param contentType
	 * @return
	 */
	boolean accepts(String contentType);

	/**
	 * Process the s3 binary data and return a consumer for the binary field.
	 * 
	 * @param context
	 * @return Modifier for the s3 binary graph field.
	 */
	Maybe<Consumer<S3BinaryGraphField>> process(S3BinaryDataProcessorContext context);

}
