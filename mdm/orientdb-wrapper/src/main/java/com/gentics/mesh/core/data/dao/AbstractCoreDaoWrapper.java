package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;

import dagger.Lazy;

/**
 * A generalized OrientDB implementation for {@link OrientDBDaoGlobal}.
 * Due to the limitation of Java type system, we need to carry around two distinct generics for the same end type of an entity.
 * 
 * @author plyhun
 *
 * @param <R> REST entity model
 * @param <T> MDM API entity counterpart
 * @param <D> OrientDB Vertex entity counterpart
 */
public abstract class AbstractCoreDaoWrapper<R extends RestModel, T extends HibCoreElement<R>, D extends MeshCoreVertex<R>> 
		extends AbstractDaoWrapper<T> implements OrientDBDaoGlobal<T> {

	public AbstractCoreDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T createPersisted(String uuid) {
		D vertex = getRoot().createRaw();
		T entity = (T) vertex;
		if (uuid != null) {
			entity.setUuid(uuid);
			entity = mergeIntoPersisted(entity);
		}
		getRoot().addItem(vertex);
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deletePersisted(T element) {
		D vertex = (D) element;
		vertex.remove();
	}
	/**
	 * Get root container for the current entity type.
	 * 
	 * @return
	 */
	protected abstract RootVertex<D> getRoot();
}
