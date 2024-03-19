package com.gentics.mesh.core.data.dao;

import java.util.Optional;
import java.util.function.Consumer;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.cli.GraphDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;

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

	public AbstractCoreDaoWrapper(Lazy<GraphDBBootstrapInitializer> boot) {
		super(boot);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T createPersisted(String uuid, Consumer<T> inflater) {
		D vertex = getRoot().create();
		if (uuid != null) {
			vertex.setUuid(uuid);
		}
		getRoot().addItem(vertex);
		inflater.accept((T) vertex);
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

	@SuppressWarnings("unchecked")
	@Override
	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ((RootVertex<? extends T>) getRoot()).findAll(ac, pagingInfo, Optional.ofNullable(extraFilter));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		RootVertex<? extends T> root = ((RootVertex<? extends T>) getRoot());
		return PersistingRootDao.shouldSort(pagingInfo) ? root.findAll(ac, pagingInfo, Optional.empty()) : root.findAll(ac, pagingInfo);
	}

	/**
	 * Get root container for the current entity type.
	 * 
	 * @return
	 */
	protected abstract RootVertex<D> getRoot();
}
