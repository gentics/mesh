package com.gentics.mesh.hibernate.data.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Entity;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.DaoGlobal;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.data.dao.PersistingDaoGlobal;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Common implementation for rootless entity DAOs.
 * 
 * @author plyhun
 *
 * @param <T>
 * @param <R>
 * @param <D>
 */
public abstract class AbstractHibDaoGlobal<T extends HibCoreElement<R>, R extends RestModel, D extends T> 
		extends AbstractHibCoreDao<T, R, D> implements DaoGlobal<T>, DaoTransformable<T, R>, PersistingDaoGlobal<T> {
	
	public AbstractHibDaoGlobal(DaoHelper<T,D> daoHelper,
			HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper,
			CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public T findByUuid(String uuid) {
		return daoHelper.findByUuid(uuid);
	}

	@Override
	public Stream<Pair<String, T>> findByUuids(Collection<String> uuids) {
		return SplittingUtils.splitAndMergeInList(uuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			return em().createQuery("select t from " + getPersistenceClass().getAnnotation(Entity.class).name() + " t where t.dbUuid in :uuids", getPersistenceClass())
				.setParameter("uuids", slice.stream().map(UUIDUtil::toJavaUuid).collect(Collectors.toSet()))
				.getResultStream()
				.map(t -> Pair.of(t.getUuid(), (T) t))
				.collect(Collectors.toList());
		}).stream();
	}

	@Override
	public long count() {
		return daoHelper.count();
	}

	@Override
	public Result<? extends T> findAll() {
		return daoHelper.findAll();
	}

	@Override
	public T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return daoHelper.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return PersistingRootDao.shouldSort(pagingInfo) 
				? daoHelper.findAll(ac, InternalPermission.READ_PERM, pagingInfo, Optional.empty()) 
				: daoHelper.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<T> extraFilter) {
		return PersistingRootDao.shouldSort(pagingInfo) 
				? new DynamicStreamPageImpl<>(
						// Since we do not know yet what the extra filter gives to us, we dare at this moment no paging - it will be applied at the PageImpl
						daoHelper.findAll(ac, Optional.empty(), ((PagingParameters) new PagingParametersImpl().putSort(pagingInfo.getSort())), Optional.empty()).stream(), 
						pagingInfo, extraFilter, false)
				: daoHelper.findAll(ac, pagingInfo, extraFilter, true);
	}

	@Override
	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return daoHelper.findAll(ac, InternalPermission.READ_PERM, pagingInfo, Optional.ofNullable(extraFilter));
	}

	@Override
	public T loadObjectByUuid(InternalActionContext ac, String userUuid, InternalPermission perm) {
		return daoHelper.loadObjectByUuid(ac, userUuid, perm);
	}

	@Override
	public T findByName(String name) {
		return daoHelper.findByName(name);
	}

	@Override
	public T createPersisted(String uuid, Consumer<T> inflater) {
		T element = daoHelper.create(uuid, inflater);
		return afterCreatedInDatabase(element);
	}

	@Override
	public T mergeIntoPersisted(T entity) {
		beforeChangedInDatabase(entity);
		return afterChangedInDatabase(em().merge(entity));
	}

	@Override
	public void deletePersisted(T entity) {
		entity = beforeDeletedFromDatabase(entity);
		em().remove(entity);
		afterDeletedFromDatabase(entity);
	}

	/**
	 * Find a global entity by UUID.
	 * 
	 * @param uuid
	 * @return
	 */
	public T findByUuid(UUID uuid) {
		return daoHelper.findByUuid(uuid);
	}
}
