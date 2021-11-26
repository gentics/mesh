package com.gentics.mesh.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshFactory;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshImpl;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.dagger.DaggerOrientDBMeshComponent;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.search.index.node.OrientDBNodeIndexHandlerImpl;

/**
 * Factory which will create and maintain the state of a single mesh instance.
 */
public class MeshFactoryImpl implements MeshFactory {

	@Override
	public Mesh create() {
		return create(OptionsLoader.createOrloadOptions(OrientDBMeshOptions.class));
	}

	@Override
	public Mesh create(MeshOptions options) {
		return new MeshImpl(options, DaggerOrientDBMeshComponent.builder().nodeIndexHandlerSupplier(MeshFactoryImpl::nodeIndexHandler));
	}

	public static NodeIndexHandler nodeIndexHandler(Mesh mesh) {
		OrientDBMeshComponent internal = mesh.internal();
		return new OrientDBNodeIndexHandlerImpl(
				internal.nodeContainerTransformer(), 
				internal.nodeContainerMappingProvider(), 
				internal.searchProvider(), 
				internal.database(), 
				internal.boot(), 
				internal.meshHelper(), 
				internal.options(), internal.syncMetersFactory(), 
				internal.bucketManager());
	}
}
