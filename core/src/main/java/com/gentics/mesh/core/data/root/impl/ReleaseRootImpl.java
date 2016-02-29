package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

public class ReleaseRootImpl extends AbstractRootVertex<Release> implements ReleaseRoot {

	public static void init(Database database) {
		database.addVertexType(ReleaseRootImpl.class);
	}

	@Override
	public Observable<Release> create(InternalActionContext ac) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Release> getPersistanceClass() {
		return ReleaseImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_RELEASE;
	}
}
