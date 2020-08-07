package com.gentics.mesh.plugin.binary;

import java.util.function.Consumer;

import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Maybe;
import io.vertx.ext.web.FileUpload;

/**
 * {@link BinaryProcessorPlugin}'s provide ways to analyze the uploaded data in order to parse and extract additional information from the data.
 */
public interface BinaryProcessorPlugin extends MeshPlugin {

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
	 * @param hash
	 *            SHA512 sum of the upload
	 * @return Modifier for the binary field.
	 */
	// TODO Maybe use a dedicated container instead of BinaryField since not all fields should be mutable.
	Maybe<Consumer<BinaryField>> process(FileUpload upload, String hash);
}
