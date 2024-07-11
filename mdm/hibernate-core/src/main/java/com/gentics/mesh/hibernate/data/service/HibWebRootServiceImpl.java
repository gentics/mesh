package com.gentics.mesh.hibernate.data.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.service.AbstractWebRootService;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;

/**
 * Webroot service implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibWebRootServiceImpl extends AbstractWebRootService {
	@Inject
	public HibWebRootServiceImpl(Database database, WebrootPathCache pathStore) {
		super(database, pathStore);
	}

	@Override
	public NodeFieldContainer findByUrlFieldPath(String branchUuid, String path, ContainerType type) {
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		return contentDao.streamEdgesOfWebrootField(path, branchUuid, type)
				.map(contentDao::getFieldContainerOfEdge)
				.findAny().orElse(null);
	}

}
