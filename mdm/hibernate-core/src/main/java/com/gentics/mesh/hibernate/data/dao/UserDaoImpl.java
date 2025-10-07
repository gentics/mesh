package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.jpa.HibernateHints;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibDatabaseElement;
import com.gentics.mesh.hibernate.data.domain.HibGroupImpl;
import com.gentics.mesh.hibernate.data.domain.HibPermissionImpl;
import com.gentics.mesh.hibernate.data.domain.HibRoleImpl;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;
import com.gentics.mesh.hibernate.data.domain.keys.HibPermissionPK;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

/**
 * User DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class UserDaoImpl extends AbstractHibDaoGlobal<HibUser, UserResponse, HibUserImpl> implements PersistingUserDao {
	private final Database db;
	
	@Inject
	public UserDaoImpl(DaoHelper<HibUser, HibUserImpl> daoHelper, HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper, 
			CurrentTransaction currentTransaction, EventFactory eventFactory, Database db, 
			Lazy<PermissionCache> permissionCache, Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.db = db;
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "username": return "name";
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}

	@Override
	public HibUser inheritRolePermissions(HibUser user, HibBaseElement source, HibBaseElement target) {
		return inheritRolePermissions(user, source, Collections.singleton(target));
	}

	@Override
	public HibUser inheritRolePermissions(HibUser user, HibBaseElement source, Collection<? extends HibBaseElement> targets) {
		List<HibPermissionImpl> sourcePermissions = em().createQuery("from permission" +
								" where element = :source",
						HibPermissionImpl.class)
				.setParameter("source", source.getId()).getResultList();

		for (HibBaseElement target : targets) {
			UUID targetUuid = ((HibDatabaseElement) target).getDbUuid();
			for (HibPermissionImpl sourcePerm : sourcePermissions) {
				HibPermissionImpl newPerm = em().find(HibPermissionImpl.class, new HibPermissionPK(targetUuid, sourcePerm.getRole()));
				if (newPerm == null) {
					newPerm = new HibPermissionImpl();
					newPerm.setElement(targetUuid);
					newPerm.setRole(sourcePerm.getRole());
				}
				newPerm.setType(sourcePerm.getType());
				newPerm.setCreatePerm(sourcePerm.isCreatePerm());
				newPerm.setReadPerm(sourcePerm.isReadPerm());
				newPerm.setUpdatePerm(sourcePerm.isUpdatePerm());
				newPerm.setDeletePerm(sourcePerm.isDeletePerm());
				newPerm.setPublishPerm(sourcePerm.isPublishPerm());
				newPerm.setReadPublishedPerm(sourcePerm.isReadPublishedPerm());
				em().merge(newPerm);
			}
		}

		Tx.get().permissionCache().clear();

		return user;
	}

	@Override
	public HibUser findByUsername(String username) {
		return HibernateTx.get().data().mesh().userNameCache().get(username, name -> {
			return firstOrNull(currentTransaction.getEntityManager().createQuery("from user where name = :name", HibUserImpl.class)
					.setHint(HibernateHints.HINT_CACHEABLE, true)
					.setParameter("name", username));
		});
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return toMeshAuthUser(findByUsername(username));
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		return toMeshAuthUser(findByUuid(userUuid));
	}

	@Override
	public HibUser addGroup(HibUser user, HibGroup group) {
		((HibGroupImpl) group).addUser(user);
		return user;
	}

	@Override
	public Iterable<? extends HibRole> getRoles(HibUser user) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.createQuery("select distinct r" +
					" from user u" +
					" join u.groups g" +
					" join g.roles r" +
					" where u = :user",
				HibRoleImpl.class)
			.setParameter("user", user)
			.setHint(HibernateHints.HINT_CACHEABLE, true)
			.getResultList();
	}

	@Override
	public Result<? extends HibGroup> getGroups(HibUser user) {
		EntityManager em = currentTransaction.getEntityManager();
		return new TraversalResult<>(
				em.createQuery("select g" +
					" from user u" +
					" join u.groups g" +
					" where u = :user",
				HibGroupImpl.class)
			.setParameter("user", user)
			.setHint(HibernateHints.HINT_CACHEABLE, true)
			.getResultList());
	}

	@Override
	public Page<? extends HibRole> getRolesViaShortcut(HibUser fromUser, HibUser authUser, PagingParameters pagingInfo) {
		EntityManager em = currentTransaction.getEntityManager();
		
		CriteriaQuery<HibRoleImpl> query = em.getCriteriaBuilder().createQuery(HibRoleImpl.class);
		Metamodel m = em.getMetamodel();
		EntityType<HibUserImpl> userMm = m.entity(HibUserImpl.class);
		EntityType<HibGroupImpl> groupMm = m.entity(HibGroupImpl.class);
		Root<HibUserImpl> user = query.from(HibUserImpl.class);
		
		Join<HibUserImpl, HibGroupImpl> groups = user.join(userMm.getSet("groups", HibGroupImpl.class));
		daoHelper.addPermissionRestriction(query, groups, authUser, READ_PERM);
		
		Join<HibGroupImpl, HibRoleImpl> roles = groups.join(groupMm.getSet("roles", HibRoleImpl.class));
		daoHelper.addPermissionRestriction(query, roles, authUser, READ_PERM);
		
		query.where(em.getCriteriaBuilder().equal(user.get("uuid"), fromUser.getUuid()));
		
		return daoHelper.getResultPage(query, pagingInfo);
	}

	@Override
	public Page<? extends HibGroup> getGroups(HibUser fromUser, HibUser authUser, PagingParameters pagingInfo) {
		CriteriaQuery<HibGroupImpl> query = cb().createQuery(HibGroupImpl.class);
		Metamodel metamodel = em().getMetamodel();
		EntityType<HibUserImpl> rootMetamodel = metamodel.entity(HibUserImpl.class);
		Root<HibUserImpl> root = query.from(HibUserImpl.class);
		Join<HibUserImpl, HibGroupImpl> resultJoin = root.join(rootMetamodel.getSet("groups", HibGroupImpl.class));

		query.select(resultJoin).where(cb().equal(root.get("dbUuid"), fromUser.getId()));
		daoHelper.addPermissionRestriction(query, resultJoin, authUser, InternalPermission.READ_PERM);

		return daoHelper.getResultPage(query, pagingInfo);
	}

	private MeshAuthUser toMeshAuthUser(HibUser user) {
		return MeshAuthUserImpl.create(
			db,
			user
		);
	}

	@Override
	public EnumSet<InternalPermission> getPermissionsForElementId(HibUser user, Object elementId) {
		EnumSet<InternalPermission> permissions = EnumSet.noneOf(InternalPermission.class);
		currentTransaction.getEntityManager().createQuery(
				"select p from permission p join p.role r join r.groups g join g.users u where u = :user and p.element = :element",
				HibPermissionImpl.class).setParameter("user", user).setParameter("element", elementId)
				.getResultList().forEach(hibPerm -> {
					for(InternalPermission p : InternalPermission.values()) {
						if(hibPerm.hasPermission(p)) {
							permissions.add(p);
						}
					}
				});

		return permissions;
	}

	@Override
	public void preparePermissionsForElementIds(HibUser user, Collection<Object> elementIds) {
		PermissionCache permissionCache = Tx.get().permissionCache();

		elementIds.stream().forEach(elementId -> {
			permissionCache.invalidate(user.getId(), elementId);
		});
		SplittingUtils.splitAndConsume(elementIds, HibernateUtil.inQueriesLimitForSplitting(1), slice -> currentTransaction.getEntityManager()
				.createQuery(
					"select p from permission p join p.role r join r.groups g join g.users u where u = :user and p.element in :element",
					HibPermissionImpl.class).setParameter("user", user).setParameter("element", slice)
						.getResultList().forEach(hibPerm -> {
							EnumSet<InternalPermission> permSet = permissionCache.get(user.getId(), hibPerm.getElement());
							if (permSet == null) {
								permSet = EnumSet.noneOf(InternalPermission.class);
							}
							for (InternalPermission p : InternalPermission.values()) {
								if (hibPerm.hasPermission(p)) {
									permSet.add(p);
								}
							}
							permissionCache.store(user.getId(), permSet, hibPerm.getElement());
						}
					)
				);
	}

	@Override
	public HibUser beforeDeletedFromDatabase(HibUser element) {
		HibUserImpl user = (HibUserImpl) element;
		user.removeGroups();
		return user;
	}
}
