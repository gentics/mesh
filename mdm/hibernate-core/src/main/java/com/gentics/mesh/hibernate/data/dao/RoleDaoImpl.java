package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.hibernate.util.HibernateUtil.inQueriesLimitForSplitting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PersistingRoleDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibGroupImpl;
import com.gentics.mesh.hibernate.data.domain.HibPermissionImpl;
import com.gentics.mesh.hibernate.data.domain.HibRoleImpl;
import com.gentics.mesh.hibernate.data.domain.keys.HibPermissionPK;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.hibernate.util.TypeInfoUtil;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.CollectionUtil;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Role DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class RoleDaoImpl extends AbstractHibDaoGlobal<HibRole, RoleResponse, HibRoleImpl> implements PersistingRoleDao {
	
	@Inject
	public RoleDaoImpl(DaoHelper<HibRole, HibRoleImpl> daoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory, Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public boolean grantRolePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions) {
		Optional<HibPermissionImpl> existingPermission = getEntityPermission(role, element);
		if (existingPermission.isPresent()) {
			if (requiresPermissionChange(existingPermission.get(), permissions)) {
				existingPermission.get().grantPermissions(permissions);
				return true;
			}

			return false;
		} else {
			HibPermissionImpl perm = new HibPermissionImpl();
			perm.setRole(role);
			perm.setElement(UUIDUtil.toJavaUuid(element.getUuid()));
			perm.setType(TypeInfoUtil.getType(element));
			perm.grantPermissions(permissions);

			em().merge(perm);
			return true;
		}
	}

	@Override
	public boolean grantRolePermissions(Set<HibRole> roles, HibBaseElement element, boolean exclusive,
			InternalPermission... permissions) {
		boolean permissionGranted = false;

		// if we need to set exclusive permissions, we need to load existing permissions on all roles, otherwise only on the roles that we want to change
		Set<HibPermissionImpl> existingSet = exclusive ? getEntityPermission(element) : getEntityPermission(roles, element);
		Map<HibRole, HibPermissionImpl> existingMap = existingSet.stream()
				.collect(Collectors.toMap(HibPermissionImpl::getRole, Function.identity()));

		for (HibRole role : roles) {
			Optional<HibPermissionImpl> existingPermission = Optional.ofNullable(existingMap.get(role));

			if (existingPermission.isPresent()) {
				if (requiresPermissionChange(existingPermission.get(), permissions)) {
					existingPermission.get().grantPermissions(permissions);
					permissionGranted = true;
				}
			} else {
				HibPermissionImpl perm = new HibPermissionImpl();
				perm.setRole(role);
				perm.setElement(UUIDUtil.toJavaUuid(element.getUuid()));
				perm.setType(TypeInfoUtil.getType(element));
				perm.grantPermissions(permissions);

				em().merge(perm);
				permissionGranted = true;
			}
		}

		if (exclusive) {
			existingMap.keySet().removeAll(roles);
			for (HibPermissionImpl perm : existingMap.values()) {
				for (InternalPermission p : permissions) {
					if (perm.hasPermission(p)) {
						perm.setPermission(p, false);
						permissionGranted = true;
					}
				}
			}
		}

		return permissionGranted;
	}

	@Override
	public boolean grantRolePermissionsWithUuids(Set<String> roleUuids, HibBaseElement element, boolean exclusive,
			InternalPermission... permissions) {
		Set<HibRole> roles = findByUuids(roleUuids).map(Pair::getRight).filter(role -> role != null).collect(Collectors.toSet());

		return grantRolePermissions(roles, element, exclusive, permissions);
	}

	private boolean requiresPermissionChange(HibPermissionImpl existingPermission, InternalPermission... permissions) {
		boolean changed = false;
		for (InternalPermission permission : permissions) {
			changed = !existingPermission.getPermissionInfo().get(permission.getRestPerm()) || changed;
		}
		return changed;
	}

	@Override
	public boolean revokeRolePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions) {
		boolean permissionRevoked = false;
		Optional<HibPermissionImpl> toUpdate = getEntityPermission(role, element);
		if (toUpdate.isEmpty()) {
			return false;
		}

		HibPermissionImpl perm = toUpdate.get();
		for (InternalPermission permission : permissions) {
			perm.setPermission(permission, false);
			permissionRevoked = true;
		}

		em().merge(perm);

		return permissionRevoked;
	}

	@Override
	public boolean revokeRolePermissions(Set<HibRole> roles, HibBaseElement element,
			InternalPermission... permissions) {
		boolean permissionRevoked = false;
		Set<HibPermissionImpl> existingSet = getEntityPermission(roles, element);
		Map<HibRole, HibPermissionImpl> existingMap = existingSet.stream()
				.collect(Collectors.toMap(HibPermissionImpl::getRole, Function.identity()));

		for (HibRole role : roles) {
			Optional<HibPermissionImpl> existingPermission = Optional.ofNullable(existingMap.get(role));

			if (!existingPermission.isEmpty()) {
				HibPermissionImpl perm = existingPermission.get();
				for (InternalPermission permission : permissions) {
					if (perm.hasPermission(permission)) {
						perm.setPermission(permission, false);
						permissionRevoked = true;
					}
				}

				em().merge(perm);
			}
		}

		return permissionRevoked;
	}

	@Override
	public boolean revokeRolePermissionsWithUuids(Set<String> roleUuids, HibBaseElement element,
			InternalPermission... permissions) {
		Set<HibRole> roles = findByUuids(roleUuids).map(Pair::getRight).filter(role -> role != null).collect(Collectors.toSet());
		return revokeRolePermissions(roles, element, permissions);
	}

	@Override
	public Set<InternalPermission> getPermissions(HibRole role, HibBaseElement element) {
		return getEntityPermission(role, element)
			.map(HibPermissionImpl::getPermissions)
			.orElse(Collections.emptySet());
	}

	@Override
	public Map<HibRole, Set<InternalPermission>> getPermissions(Set<HibRole> roles, HibBaseElement element) {
		if (CollectionUtils.isEmpty(roles)) {
			return Collections.emptyMap();
		}
		Map<HibRole, Set<InternalPermission>> permissionMap = new HashMap<>();
		for (HibRole role : roles) {
			permissionMap.put(role, new HashSet<>());
		}

		boolean includePublishPermissions = element.hasPublishPermissions();
		for (HibPermissionImpl perm : getEntityPermission(roles, element)) {
			HibRole role = perm.getRole();
			permissionMap.computeIfAbsent(role, key -> new HashSet<>()).addAll(perm.getPermissions());
			if (!includePublishPermissions) {
				permissionMap.get(role).removeAll(
						Arrays.asList(InternalPermission.PUBLISH_PERM, InternalPermission.READ_PUBLISHED_PERM));
			}
		}

		return permissionMap;
	}

	@Override
	public boolean hasPermission(HibRole role, InternalPermission permission, HibBaseElement element) {
		return getEntityPermission(role, element)
			.map(perm -> perm.hasPermission(permission))
			.orElse(false);
	}

	private Optional<HibPermissionImpl> getEntityPermission(HibRole role, HibBaseElement element) {
		HibPermissionPK primaryKey = new HibPermissionPK((UUID) element.getId(), role);
		return Optional.ofNullable(em().find(HibPermissionImpl.class, primaryKey));
	}

	private Set<HibPermissionImpl> getEntityPermission(HibBaseElement element) {
		return em()
			.createQuery("select p from permission p where element = :element", HibPermissionImpl.class)
			.setParameter("element", UUIDUtil.toJavaUuid(element.getUuid()))
			.getResultStream()
			.collect(Collectors.toSet());
	}

	private Set<HibPermissionImpl> getEntityPermission(Set<HibRole> roles, HibBaseElement element) {
		Set<HibPermissionImpl> permSet = new HashSet<>(getEntityPermission(element));
		permSet.removeIf(perm -> !roles.contains(perm.getRole()));
		return permSet;
	}

	@Override
	public void addRole(HibRole role) {
		em().persist(role);
	}

	@Override
	public void removeRole(HibRole role) {
		em().remove(role);
	}

	@Override
	public Result<? extends HibGroup> getGroups(HibRole role) {
		return new TraversalResult<>(role.getGroups());
	}

	@Override
	public Map<HibRole, Collection<? extends HibGroup>> getGroups(Collection<HibRole> roles) {
		List<UUID> rolesUuids = roles.stream().map(HibRole::getId).map(UUID.class::cast).collect(Collectors.toList());

		Map<HibRole, Collection<? extends HibGroup>> result = new HashMap<>();
		result.putAll(SplittingUtils.splitAndMergeInMapOfLists(rolesUuids, inQueriesLimitForSplitting(1), (uuids) -> {
			@SuppressWarnings("unchecked")
			List<Object[]> resultList = em().createNamedQuery("role.findgroupsforroles")
					.setParameter("roleUuids", uuids)
					.getResultList();

			return resultList.stream()
					.map(tuples -> Pair.of((HibRole)tuples[0], (HibGroup)tuples[1]))
					.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		}));
		return CollectionUtil.addFallbackValueForMissingKeys(result, roles, new ArrayList<>());
	}

	@Override
	public Page<? extends HibGroup> getGroups(HibRole role, HibUser authUser, PagingParameters pagingInfo) {
		CriteriaQuery<HibGroupImpl> query = cb().createQuery(HibGroupImpl.class);
		Metamodel metamodel = em().getMetamodel();
		EntityType<HibRoleImpl> rootMetamodel = metamodel.entity(HibRoleImpl.class);
		Root<HibRoleImpl> root = query.from(HibRoleImpl.class);
		Join<HibRoleImpl, HibGroupImpl> resultJoin = root.join(rootMetamodel.getSet("groups", HibGroupImpl.class));

		query.select(resultJoin).where(cb().equal(root.get("dbUuid"), role.getId()));
		daoHelper.addPermissionRestriction(query, resultJoin, authUser, InternalPermission.READ_PERM);

		return daoHelper.getResultPage(query, pagingInfo);
	}

	@Override
	public Set<String> getRoleUuidsForPerm(HibBaseElement element, InternalPermission permission) {
		return em()
			.createQuery("select p.role.dbUuid" +
					" from permission p" +
					" where element = :element" +
					" and p." + HibPermissionImpl.getColumnName(permission) + " is true",
				UUID.class)
			.setParameter("element", UUIDUtil.toJavaUuid(element.getUuid()))
			.getResultStream()
			.map(UUIDUtil::toShortUuid)
			.collect(Collectors.toSet());
	}

	@Override
	public HibRole beforeDeletedFromDatabase(HibRole element) {
		HibRoleImpl role = (HibRoleImpl) element;
		role.removeGroups();
		return role;
	}

	@Override
	public HibRole findByName(String name) {
		return HibernateTx.get().data().mesh().roleNameCache().get(name, roleName -> {
			return super.findByName(roleName);
		});
	}

	@Override
	public Stream<Pair<String, HibRole>> findByNames(Collection<String> names) {
		return SplittingUtils.splitAndMergeInList(names, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			return em().createQuery("select r from role r where r.name in :names", HibRole.class)
				.setParameter("names", slice)
				.getResultStream()
				.map(t -> Pair.of(t.getName(), t))
				.collect(Collectors.toList());
		}).stream();
	}

	@Override
	public Set<Triple<String, String, Object>> findAll(InternalActionContext ac) {
		List<Tuple> resultList = null;
		HibUser user = ac.getUser();
		if (user.isAdmin()) {
			resultList = em().createNamedQuery("role.findAllForAdmin", Tuple.class).getResultList();
		} else {
			List<HibRole> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			resultList = SplittingUtils.splitAndMergeInList(roles, HibernateUtil.inQueriesLimitForSplitting(0), slice -> em().createNamedQuery("role.findAll", Tuple.class)
					.setParameter("roles", slice)
					.getResultList());
		}
		return resultList.stream().map(tuple -> {
			Triple<String, String, Object> triple = Triple.of(UUIDUtil.toShortUuid(tuple.get("uuid", UUID.class)), tuple.get("name", String.class), tuple.get("uuid", UUID.class));
			return triple;
		}).collect(Collectors.toSet());
	}
}
