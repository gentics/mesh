package com.gentics.mesh.core.binary;

import java.util.function.Consumer;

import com.gentics.mesh.core.data.node.field.HibBinaryField;

import io.reactivex.Maybe;

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
	 * Process the binary data and return a consumer for the binary field.
	 * 
	 * @param upload
	 * @param hash SHA512 sum of the upload
	 * @return Modifier for the binary graph field.
	 */
	Maybe<Consumer<HibBinaryField>> process(BinaryDataProcessorContext context);

}
