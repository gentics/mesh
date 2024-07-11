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
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.hibernate.annotations.QueryHints;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
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

/**
 * User DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class UserDaoImpl extends AbstractHibDaoGlobal<User, UserResponse, HibUserImpl> implements PersistingUserDao {
	private final Database db;
	
	@Inject
	public UserDaoImpl(DaoHelper<User, HibUserImpl> daoHelper, HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper, 
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
	public User inheritRolePermissions(User user, BaseElement source, BaseElement target) {
		return inheritRolePermissions(user, source, Collections.singleton(target));
	}

	@Override
	public User inheritRolePermissions(User user, BaseElement source, Collection<? extends BaseElement> targets) {
		List<HibPermissionImpl> sourcePermissions = em().createQuery("from permission" +
								" where element = :source",
						HibPermissionImpl.class)
				.setParameter("source", source.getId()).getResultList();

		for (BaseElement target : targets) {
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
	public User findByUsername(String username) {
		return HibernateTx.get().data().mesh().userNameCache().get(username, name -> {
			return firstOrNull(currentTransaction.getEntityManager().createQuery("from user where name = :name", HibUserImpl.class).setHint(QueryHints.CACHEABLE, true)
					.setParameter("name", username)
					.setHint(QueryHints.CACHEABLE, true));
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
	public User addGroup(User user, Group group) {
		((HibGroupImpl) group).addUser(user);
		return user;
	}

	@Override
	public Iterable<? extends Role> getRoles(User user) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.createQuery("select distinct r" +
					" from user u" +
					" join u.groups g" +
					" join g.roles r" +
					" where u = :user",
				HibRoleImpl.class)
			.setParameter("user", user)
			.setHint(QueryHints.CACHEABLE, true)
			.getResultList();
	}

	@Override
	public Result<? extends Group> getGroups(User user) {
		EntityManager em = currentTransaction.getEntityManager();
		return new TraversalResult<>(
				em.createQuery("select g" +
					" from user u" +
					" join u.groups g" +
					" where u = :user",
				HibGroupImpl.class)
			.setParameter("user", user)
			.getResultStream());
	}

	@Override
	public Page<? extends Role> getRolesViaShortcut(User fromUser, User authUser, PagingParameters pagingInfo) {
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
	public Page<? extends Group> getGroups(User fromUser, User authUser, PagingParameters pagingInfo) {
		CriteriaQuery<HibGroupImpl> query = cb().createQuery(HibGroupImpl.class);
		Metamodel metamodel = em().getMetamodel();
		EntityType<HibUserImpl> rootMetamodel = metamodel.entity(HibUserImpl.class);
		Root<HibUserImpl> root = query.from(HibUserImpl.class);
		Join<HibUserImpl, HibGroupImpl> resultJoin = root.join(rootMetamodel.getSet("groups", HibGroupImpl.class));

		query.select(resultJoin).where(cb().equal(root.get("dbUuid"), fromUser.getId()));
		daoHelper.addPermissionRestriction(query, resultJoin, authUser, InternalPermission.READ_PERM);

		return daoHelper.getResultPage(query, pagingInfo);
	}

	private MeshAuthUser toMeshAuthUser(User user) {
		return MeshAuthUserImpl.create(
			db,
			user
		);
	}

	@Override
	public EnumSet<InternalPermission> getPermissionsForElementId(User user, Object elementId) {
		EnumSet<InternalPermission> permissions = EnumSet.noneOf(InternalPermission.class);
		currentTransaction.getEntityManager().createQuery(
				"select p from permission p join p.role r join r.groups g join g.users u where u = :user and p.element = :element",
				HibPermissionImpl.class).setParameter("user", user).setParameter("element", elementId)
				.getResultStream().forEach(hibPerm -> {
					for(InternalPermission p : InternalPermission.values()) {
						if(hibPerm.hasPermission(p)) {
							permissions.add(p);
						}
					}
				});

		return permissions;
	}

	@Override
	public void preparePermissionsForElementIds(User user, Collection<Object> elementIds) {
		PermissionCache permissionCache = Tx.get().permissionCache();

		elementIds.stream().forEach(elementId -> {
			permissionCache.invalidate(user.getId(), elementId);
		});
		SplittingUtils.splitAndConsume(elementIds, HibernateUtil.inQueriesLimitForSplitting(1), slice -> currentTransaction.getEntityManager()
				.createQuery(
					"select p from permission p join p.role r join r.groups g join g.users u where u = :user and p.element in :element",
					HibPermissionImpl.class).setParameter("user", user).setParameter("element", slice)
						.getResultStream().forEach(hibPerm -> {
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
	public User beforeDeletedFromDatabase(User element) {
		HibUserImpl user = (HibUserImpl) element;
		user.removeGroups();
		return user;
	}
}
