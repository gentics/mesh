package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.data.PermissionType;
import com.google.common.collect.ImmutableMap;

/**
 * Root permission entity implementation.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "permissionroot")
public class HibPermissionRootImpl extends AbstractHibBaseElement implements Serializable {

	private static final long serialVersionUID = -766477937727737773L;

	@ManyToOne(targetEntity = HibProjectImpl.class, fetch = FetchType.LAZY)
	private Project parent;

	@Enumerated(EnumType.STRING)
	private PermissionType type;

	public Project getParent() {
		return parent;
	}

	public HibPermissionRootImpl setParent(Project parent) {
		this.parent = parent;
		return this;
	}

	public PermissionType getType() {
		return type;
	}

	public HibPermissionRootImpl setType(PermissionType type) {
		this.type = type;
		return this;
	}

	@Transient
	ImmutableMap<PermissionType, Supplier<Result<? extends BaseElement>>> globalDaoMap = ImmutableMap.<PermissionType, Supplier<Result<? extends BaseElement>>>builder()
			.put(PermissionType.PROJECT, () -> Tx.get().projectDao().findAll())
			.put(PermissionType.USER, () -> Tx.get().userDao().findAll())
			.put(PermissionType.GROUP, () -> Tx.get().groupDao().findAll())
			.put(PermissionType.ROLE, () -> Tx.get().roleDao().findAll())
			.put(PermissionType.SCHEMA, () -> Tx.get().schemaDao().findAll())
			.put(PermissionType.MICROSCHEMA, () -> Tx.get().microschemaDao().findAll())
			.build();

	@Transient
	ImmutableMap<PermissionType, Function<Project, Result<? extends BaseElement>>> rootDaoMap = ImmutableMap.<PermissionType, Function<Project, Result<? extends BaseElement>>>builder()
			.put(PermissionType.MICROSCHEMA, (project) -> Tx.get().microschemaDao().findAll(project))
			.put(PermissionType.SCHEMA, (project) -> Tx.get().schemaDao().findAll(project))
			.put(PermissionType.BRANCH, (project) -> Tx.get().branchDao().findAll(project))
			.put(PermissionType.TAG_FAMILY, (project) -> Tx.get().tagFamilyDao().findAll(project))
			.put(PermissionType.NODE, (project) -> Tx.get().nodeDao().findAll(project))
			.build();

	@Override
	public boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		if (PermissionType.MESH.equals(type)) {
			// TODO not clear when this will be called, I've only found an occurrence on tests
			return true;
		}

		if (parent != null) {
			Function<Project, Result<? extends BaseElement>> projectProvider = rootDaoMap.get(type);
			if (projectProvider == null) {
				throw new IllegalArgumentException("Don't know root node for type " + type);
			}

			Supplier<Result<? extends BaseElement>> provider = () -> projectProvider.apply(parent);
			return handleDefaultPermission(authUser, batch, role, permissionsToGrant, permissionsToRevoke, recursive, provider);
		} else {
			Supplier<Result<? extends BaseElement>> provider = globalDaoMap.get(type);
			if (provider == null) {
				throw new IllegalArgumentException("Don't know root node for type " + type);
			}

			return handleDefaultPermission(authUser, batch, role, permissionsToGrant, permissionsToRevoke, recursive, provider);
		}
	}

	public boolean handleDefaultPermission(MeshAuthUser authUser, EventQueueBatch batch, Role role, Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke, boolean recursive, Supplier<Result<? extends BaseElement>> childrenFinder) {
		boolean permissionChanged = false;
		UserDao userDao = Tx.get().userDao();
		if (recursive) {
			for (BaseElement t : childrenFinder.get().stream().filter(e -> userDao.hasPermission(authUser.getDelegate(), this, InternalPermission.READ_PERM)).collect(Collectors.toList())) {
				permissionChanged = t.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = super.applyPermissions(authUser, batch, role, false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}
}
