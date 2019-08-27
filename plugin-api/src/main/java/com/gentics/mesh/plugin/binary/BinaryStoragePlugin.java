package com.gentics.mesh.plugin.binary;

import java.util.function.Consumer;

import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Maybe;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * A binary storage plugin is a plugin which enhances and hooks into the binary handling capabilities of Gentics Mesh. You can use it to process binary data
 * during upload and to add new storage providers to Gentics Mesh.
 */
public interface BinaryStoragePlugin extends MeshPlugin {

	/**
	 * Check whether the plugin can handle binary data with the given id.
	 * 
	 * @param storageId
	 * @return
	 */
	boolean canHandle(String storageId);

	/**
	 * Handle the binary download request for the given data.
	 * 
	 * @param rc
	 * @param storageId
	 * @param contentType
	 */
	void handle(RoutingContext rc, String storageId, String contentType);

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
	Maybe<Consumer<BinaryField>> process(FileUpload upload, String hash);

}
