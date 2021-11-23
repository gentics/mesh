package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
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
		extends AbstractDaoWrapper<T> implements PersistingDaoGlobal<T>, DaoTransformable<T, R> {

	public AbstractCoreDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T createPersisted(String uuid) {
		D vertex = getRoot().create();
		if (uuid != null) {
			vertex.setUuid(uuid);
		}
		getRoot().addItem(vertex);
		return (T) vertex;
	}

	/**
	 * Since OrientDB does not tell apart POJOs and persistent entities, 
	 * processing the entity updates directly into the persistent state, 
	 * the merge implementation here is empty.
	 */
	@Override
	public T mergeIntoPersisted(T entity) {
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deletePersisted(T entity) {
		((D) entity).remove();
	}

	@Override
	public long count() {
		return getRoot().globalCount();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends T> getPersistenceClass() {
		return (Class<? extends T>) getRoot().getPersistanceClass();
	}

	/**
	 * Get root container for the current entity type.
	 * 
	 * @return
	 */
	protected abstract RootVertex<D> getRoot();
}
