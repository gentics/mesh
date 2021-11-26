package com.gentics.mesh.search.index.node;

import java.util.function.Function;
import java.util.stream.Stream;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.search.index.node.NodeContainerMappingProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

public class OrientDBNodeIndexHandlerImpl extends NodeIndexHandlerImpl {

	public OrientDBNodeIndexHandlerImpl(NodeContainerTransformer transformer, NodeContainerMappingProvider mappingProvider, 
			SearchProvider searchProvider, Database db, BootstrapInitializer boot,
			MeshHelper helper, MeshOptions options, SyncMetersFactory syncMetersFactory, BucketManager bucketManager) {
		super(transformer, mappingProvider, searchProvider, db, boot, helper, options, syncMetersFactory, bucketManager);
	}

	@Override
	public Function<String, HibNode> elementLoader() {
		return (uuid) -> HibClassConverter.toGraph(db).index().findByUuid(Node.class, uuid);
	}

	@Override
	public Stream<? extends HibNode> loadAllElements() {
		return HibClassConverter.toGraph(db).type().findAll(Node.class);
	}
}
