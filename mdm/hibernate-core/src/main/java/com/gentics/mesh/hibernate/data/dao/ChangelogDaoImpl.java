package com.gentics.mesh.hibernate.data.dao;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.changelog.Change;
import com.gentics.mesh.core.data.changelog.ChangeMarker;
import com.gentics.mesh.core.data.dao.ChangelogDao;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibChangeMarkerImpl;
import com.gentics.mesh.util.UUIDUtil;

/**
 * High level Changelog DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class ChangelogDaoImpl implements ChangelogDao {

	@Inject
	public ChangelogDaoImpl() {
	}

	@Override
	public Iterator<? extends ChangeMarker> findAll() {
		return HibernateTx.get().loadAll(HibChangeMarkerImpl.class).iterator();
	}

	@Override
	public boolean hasChange(Change change) {
		return HibernateTx.get().load(UUIDUtil.toJavaUuid(change.getUuid()), HibChangeMarkerImpl.class) != null;
	}

	@Override
	public void add(Change change, long duration) {
		HibChangeMarkerImpl marker = new HibChangeMarkerImpl();
		marker.setUuid(change.getUuid());
		marker.setDuration(duration);
		HibernateTx.get().entityManager().persist(marker);
	}

}
