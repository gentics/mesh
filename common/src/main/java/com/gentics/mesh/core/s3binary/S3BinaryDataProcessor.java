package com.gentics.mesh.core.s3binary;

import java.util.function.Consumer;

import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;

import io.reactivex.Maybe;

/**
 * A S3 binary data processor accepts a fileupload in order to extract specific information from the data.
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
	Maybe<Consumer<S3HibBinaryField>> process(S3BinaryDataProcessorContext context);

}
