package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_URLFIELD_INDEX_NAME;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * @see WebRootService
 */
@Singleton
public class WebRootServiceImpl extends AbstractWebRootService {

	@Inject
	public WebRootServiceImpl(Database database, WebrootPathCache pathStore) {
		super(database, pathStore);
	}

	@Override
	public NodeGraphFieldContainer findByUrlFieldPath(String branchUuid, String path, ContainerType type) {
		Object key = GraphFieldContainerEdgeImpl.composeWebrootUrlFieldIndexKey(HibClassConverter.toGraph(database), path, branchUuid, type);
		GraphFieldContainerEdge edge = HibClassConverter.toGraph(database).findEdge(WEBROOT_URLFIELD_INDEX_NAME, key, GraphFieldContainerEdgeImpl.class);
		if (edge != null) {
			return (NodeGraphFieldContainer) edge.getNodeContainer();
		} else {
			return null;
		}
	}

}
