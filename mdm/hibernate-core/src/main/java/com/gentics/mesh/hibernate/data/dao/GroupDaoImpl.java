package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.hibernate.util.HibernateUtil.inQueriesLimitForSplitting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.dao.PersistingGroupDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibGroupImpl;
import com.gentics.mesh.hibernate.data.domain.HibRoleImpl;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.CollectionUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Group DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class GroupDaoImpl extends AbstractHibDaoGlobal<HibGroup, GroupResponse, HibGroupImpl> implements PersistingGroupDao {
	
	@Inject
	public GroupDaoImpl(CurrentTransaction currentTransaction, DaoHelper<HibGroup, HibGroupImpl> daoHelper, 
			CommonDaoHelper commonDaoHelper, HibPermissionRoots permissionRoots, EventFactory eventFactory, Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public void addUserPersisting(HibGroup group, HibUser user) {
		((HibGroupImpl) group).addUser(user);
	}

	@Override
	public void removeUserPersisting(HibGroup group, HibUser user) {
		((HibGroupImpl) group).removeUser(user);
	}

	@Override
	public void addRolePersisting(HibGroup group, HibRole role) {
		((HibRoleImpl) role).addGroup(group);
	}

	@Override
	public void removeRolePersisting(HibGroup group, HibRole role) {
		((HibGroupImpl) group).removeRole(role);
	}

	@Override
	public long countUsers(HibGroup group) {
		return em().createQuery("select count(*) from group g inner join g.users u where g.dbUuid = :groupUuid", Long.class)
			.setParameter("groupUuid", group.getId())
			.getSingleResult();
	}

	@Override
	public Result<? extends HibUser> getUsers(HibGroup group) {
		return new TraversalResult<>(((HibGroupImpl) group).getUsers());
	}

	@Override
	public Result<? extends HibUser> getUsers(HibGroup group, Bucket bucket) {
		return new TraversalResult<>(em().createQuery("select u from group g inner join g.users u where g.dbUuid = :groupUuid and u.bucketTracking.bucketId >= :start and u.bucketTracking.bucketId <= :end", HibUserImpl.class)
			.setParameter("groupUuid", group.getId())
			.setParameter("start", bucket.start())
			.setParameter("end", bucket.end())
			.getResultStream());
	}

	@Override
	public Result<? extends HibRole> getRoles(HibGroup group) {
		return new TraversalResult<>(
				em().createQuery("select r from group g inner join g.roles r where g.dbUuid = :groupUuid", HibRoleImpl.class)
				.setParameter("groupUuid", group.getId())
				.getResultStream());
	}

	@Override
	public Map<HibGroup, Collection<? extends HibRole>> getRoles(Collection<HibGroup> groups) {
		List<UUID> groupsUuids = groups.stream().map(HibGroup::getId).map(UUID.class::cast).collect(Collectors.toList());

		Map<HibGroup, Collection<? extends HibRole>> result = new HashMap<>();
		result.putAll(SplittingUtils.splitAndMergeInMapOfLists(groupsUuids, inQueriesLimitForSplitting(1), (uuids) -> {
			@SuppressWarnings("unchecked")
			List<Object[]> resultList = em().createNamedQuery("group.findrolesforgroups")
					.setParameter("groupUuids", uuids)
					.getResultList();

			return resultList.stream()
					.map(tuples -> Pair.of((HibGroup)tuples[0], (HibRole)tuples[1]))
					.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		}));
		return CollectionUtil.addFallbackValueForMissingKeys(result, groups, new ArrayList<>());
	}

	@Override
	public boolean hasUser(HibGroup group, HibUser user) {
		return em().createQuery("select u from group g inner join g.users u where u.dbUuid = :userUuid and g.dbUuid = :groupUuid")
				.setParameter("userUuid", user.getId())
				.setParameter("groupUuid", group.getId())
				.getResultStream()
				.findFirst().isPresent();
	}

	@Override
	public boolean hasRole(HibGroup group, HibRole role) {
		return ((HibGroupImpl) group).getRoles().anyMatch(r -> r.getId().equals(role.getId()));
	}

	@Override
	public Page<? extends HibRole> getRoles(HibGroup group, HibUser requestUser, PagingParameters pagingInfo) {
		CriteriaBuilder cb = cb();
		CriteriaQuery<HibRoleImpl> query = cb.createQuery(HibRoleImpl.class);
		Root<HibRoleImpl> root = query.from(HibRoleImpl.class);
		query.select(root).where(cb.isMember(group, root.get("groups")));
		daoHelper.addPermissionRestriction(query, root, requestUser, READ_PERM);
		return daoHelper.getResultPage(null, query, pagingInfo, null, null);
	}

	@Override
	public Page<? extends HibUser> getVisibleUsers(HibGroup group, HibUser requestUser, PagingParameters pagingInfo) {
		CriteriaBuilder cb = cb();
		CriteriaQuery<HibUserImpl> query = cb.createQuery(HibUserImpl.class);
		Root<HibUserImpl> root = query.from(HibUserImpl.class);
		query.select(root).where(cb.isMember(group, root.get("groups")));
		daoHelper.addPermissionRestriction(query, root, requestUser, READ_PERM);

		return daoHelper.getResultPage(null, query, pagingInfo, null, null);
	}

	@Override
	public HibGroup beforeDeletedFromDatabase(HibGroup element) {
		HibGroupImpl group = (HibGroupImpl) element;
		group.removeRoles();
		group.removeUsers();
		return group;
	}

	@Override
	public HibGroup findByName(String name) {
		return HibernateTx.get().data().mesh().groupNameCache().get(name, gName -> {
			return super.findByName(gName);
		});
	}

	@Override
	public Stream<Pair<String, HibGroup>> findByNames(Collection<String> names) {
		return SplittingUtils.splitAndMergeInList(names, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			return em().createQuery("select g from group g where g.name in :names", HibGroup.class)
				.setParameter("names", slice)
				.getResultStream()
				.map(t -> Pair.of(t.getName(), t))
				.collect(Collectors.toList());
		}).stream();
	}
}
