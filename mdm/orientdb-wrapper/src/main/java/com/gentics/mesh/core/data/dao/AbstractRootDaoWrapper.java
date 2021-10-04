package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;

import dagger.Lazy;

public abstract class AbstractRootDaoWrapper<RM extends RestModel, L extends HibCoreElement<RM>, D extends MeshCoreVertex<RM>, R extends HibCoreElement<? extends RestModel>> 
	extends AbstractDaoWrapper<L> implements OrientDBRootDao<R, L> {

	public AbstractRootDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@SuppressWarnings("unchecked")
	@Override
	public L createPersisted(R root, String uuid) {
		return (L) getRoot(root).createRaw();
	}
	
	protected abstract RootVertex<D> getRoot(R root);
}
