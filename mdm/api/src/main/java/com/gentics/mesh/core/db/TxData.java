package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;

/**
 * Definition of data that can be accessed via a Transaction {@link Tx#data()} method.
 */
public interface TxData {

	/**
	 * Mesh server options.
	 * 
	 * @return
	 */
	MeshOptions options();

	/**
	 * Version info.
	 * 
	 * @return
	 */
	HibMeshVersion meshVersion();

	/**
	 * References to permission root elements.
	 * 
	 * @return
	 */
	PermissionRoots permissionRoots();

	/**
	 * Vert.x reference.
	 * 
	 * @return
	 */
	Vertx vertx();

	/**
	 * A reference to the indexing engine.
	 * 
	 * @return
	 */
	NodeIndexHandler nodeIndexHandler();

	/**
	 * A reference to the link replacer.
	 * 
	 * @return
	 */
	WebRootLinkReplacer webRootLinkReplacer();
}
