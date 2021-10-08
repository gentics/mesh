package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;

import dagger.Lazy;

/**
 * A generalized OrientDB implementation for {@link OrientDBRootDao}.
 * Due to the limitation of Java type system, we need to carry around two distinct generics for the same end type of an entity.
 * 
 * @author plyhun
 *
 * @param <RM> REST leaf entity model
 * @param <L> MDM API leaf entity counterpart
 * @param <D> OrientDB Vertex leaf entity counterpart
 * @param <R> MDM API root entity type 
 */
public abstract class AbstractRootDaoWrapper<RM extends RestModel, L extends HibCoreElement<RM>, D extends MeshCoreVertex<RM>, R extends HibCoreElement<? extends RestModel>> 
	extends AbstractDaoWrapper<L> implements DaoTransformable<L, RM> {

	public AbstractRootDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@SuppressWarnings("unchecked")
	public L persist(R root, String uuid) {
		D vertex = getRoot(root).create();
		L entity = (L) vertex;
		if (uuid != null) {
			entity.setUuid(uuid);
		}
		getRoot(root).addItem(vertex);
		return entity;
	}

	public void unpersist(R root, L element) {
		getRoot(root).findByUuid(element.getUuid()).remove();
	}

	@Override
	public String getETag(L element, InternalActionContext ac) {
		return element.getETag(ac);
	}

	/**
	 * Get container vertex for the given root entity.  
	 * 
	 * @param root
	 * @return
	 */
	protected abstract RootVertex<D> getRoot(R root);
}
