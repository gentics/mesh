package com.gentics.mesh.dagger;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.core.search.index.node.NodeContainerMappingProvider;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

/**
 * Core extension dagger mesh component.
 */
public interface CoreMeshComponent extends MeshComponent {

	@Getter
	NodeContainerTransformer nodeContainerTransformer();

	@Getter
	NodeContainerMappingProvider nodeContainerMappingProvider();

	@Getter
	MeshHelper meshHelper();

	@Getter
	SyncMetersFactory syncMetersFactory();

	/**
	 * Builder for the core dagger component. 
	 */
	interface Builder extends MeshComponent.Builder {

		@Override
		CoreMeshComponent build();
	}
}

