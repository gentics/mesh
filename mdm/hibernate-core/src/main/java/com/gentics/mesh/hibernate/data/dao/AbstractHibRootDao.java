package com.gentics.mesh.hibernate.data.dao;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Entity;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.UUIDUtil;
import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Common implementation for DAOs of entities, that relate to some root entity. 
 * 
 * @author plyhun
 *
 * @param <L> item entity type
 * @param <RM> item entity REST model
 * @param <DL> item entity implementation
 * @param <R> root entity type
 * @param <DR> root entity implementation
 */
public abstract class AbstractHibRootDao<
			L extends CoreElement<RM>, RM extends RestModel, DL extends L, 
			R extends CoreElement<? extends RestModel>, DR extends R
		> extends AbstractHibCoreDao<L, RM, DL> implements PersistingRootDao<R, L> {
	
	protected final RootDaoHelper<L, DL, R, DR> rootDaoHelper;

	public AbstractHibRootDao(RootDaoHelper<L, DL, R, DR> rootDaoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(rootDaoHelper.getDaoHelper(), permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.rootDaoHelper = rootDaoHelper;
	}

	@Override
	public L mergeIntoPersisted(R root, L entity) {
		beforeChangedInDatabase(entity);
		L merged = em().merge(entity);
		return afterChangedInDatabase(merged);
	}

	@Override
	public L findByUuid(R root, String uuid) {
		if (!UUIDUtil.isUUID(uuid)) {
			return null;
		}
		L element = rootDaoHelper.getDaoHelper().findByUuid(uuid);

		// double check that element actually belongs to the root
		if (doesItemBelongToRoot(element, root)) {
			return element;
		}

		return null;
	}

	@Override
	public Stream<Pair<String, L>> findByUuids(R root, Collection<String> uuids) {
		return SplittingUtils.splitAndMergeInList(uuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			return em().createQuery("select l from " + getPersistenceClass(root).getAnnotation(Entity.class).name() + " l where l.dbUuid in :uuids", getPersistenceClass(root))
				.setParameter("uuids", slice.stream().map(UUIDUtil::toJavaUuid).collect(Collectors.toSet()))
				.getResultStream()
				.filter(item -> doesItemBelongToRoot(item, root))
				.map(l -> Pair.of(l.getUuid(), (L) l))
				.collect(Collectors.toList());
		}).stream();
	}

	@Override
	public long count(R root) {
		return rootDaoHelper.countInRoot(root);
	}

	@Override
	public long globalCount(R root) {
		return count(root);
	}

	/**
	 * Check if this element belongs to the given root
	 * 
	 * @param element
	 * @param root
	 * @return
	 */
	protected boolean doesItemBelongToRoot(L element, R root) {
		return element != null && Objects.equals(rootGetter().apply(element), root);
	}

	/**
	 * A function used to retrieve the root of the element
	 * @return
	 */
	public abstract Function<L, R> rootGetter();
}
