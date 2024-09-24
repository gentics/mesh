package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.hibernate.data.domain.keys.HibPermissionPK;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;

/**
 * Permission entity implementation for Gentics Mesh. This implementation considers a single common permission entity for all Mesh entities being subject to the permissioned access.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "permission")
@IdClass(HibPermissionPK.class)
public class HibPermissionImpl implements Serializable {

	private static final long serialVersionUID = -9111023282956320512L;

	@Id
	@Column
	private UUID element;

	@Id
	@ManyToOne(targetEntity = HibRoleImpl.class, fetch = FetchType.LAZY, optional = false)
	private HibRole role;

	@Enumerated(EnumType.STRING)
	private ElementType type;

	private boolean createPerm;
	private boolean readPerm;
	private boolean updatePerm;
	private boolean deletePerm;
	private boolean publishPerm;
	private boolean readPublishedPerm;

	public boolean isCreatePerm() {
		return createPerm;
	}

	public HibPermissionImpl setCreatePerm(boolean createPerm) {
		this.createPerm = createPerm;
		return this;
	}

	public boolean isReadPerm() {
		return readPerm;
	}

	public HibPermissionImpl setReadPerm(boolean readPerm) {
		this.readPerm = readPerm;
		return this;
	}

	public boolean isUpdatePerm() {
		return updatePerm;
	}

	public HibPermissionImpl setUpdatePerm(boolean updatePerm) {
		this.updatePerm = updatePerm;
		return this;
	}

	public boolean isDeletePerm() {
		return deletePerm;
	}

	public HibPermissionImpl setDeletePerm(boolean deletePerm) {
		this.deletePerm = deletePerm;
		return this;
	}

	public boolean isPublishPerm() {
		return publishPerm;
	}

	public HibPermissionImpl setPublishPerm(boolean publishPerm) {
		this.publishPerm = publishPerm;
		return this;
	}

	public boolean isReadPublishedPerm() {
		return readPublishedPerm;
	}

	public HibPermissionImpl setReadPublishedPerm(boolean readPublishedPerm) {
		this.readPublishedPerm = readPublishedPerm;
		return this;
	}

	public boolean hasPermission(InternalPermission permission) {
		switch (permission) {
			case CREATE_PERM:
				return createPerm;
			case READ_PERM:
				return readPerm;
			case UPDATE_PERM:
				return updatePerm;
			case DELETE_PERM:
				return deletePerm;
			case PUBLISH_PERM:
				return publishPerm;
			case READ_PUBLISHED_PERM:
				return readPublishedPerm;
			default:
				throw new RuntimeException("Unknown permission " + permission.name());
		}
	}

	public HibPermissionImpl setPermission(InternalPermission permission, boolean allowed) {
		switch (permission) {
			case CREATE_PERM:
				setCreatePerm(allowed);
				break;
			case READ_PERM:
				setReadPerm(allowed);
				break;
			case UPDATE_PERM:
				setUpdatePerm(allowed);
				break;
			case DELETE_PERM:
				setDeletePerm(allowed);
				break;
			case PUBLISH_PERM:
				setPublishPerm(allowed);
				break;
			case READ_PUBLISHED_PERM:
				setReadPublishedPerm(allowed);
				break;
		}
		return this;
	}

	public UUID getElement() {
		return element;
	}

	public void setElement(UUID element) {
		this.element = element;
	}

	public HibRole getRole() {
		return role;
	}

	public void setRole(HibRole role) {
		this.role = role;
	}

	public ElementType getType() {
		return type;
	}

	public void setType(ElementType type) {
		this.type = type;
	}

	public PermissionInfo getPermissionInfoWithoutPublish() {
		return new PermissionInfo()
			.setCreate(createPerm)
			.setRead(readPerm)
			.setUpdate(updatePerm)
			.setDelete(deletePerm);
	}

	public PermissionInfo getPermissionInfo() {
		return new PermissionInfo()
			.setCreate(createPerm)
			.setRead(readPerm)
			.setUpdate(updatePerm)
			.setDelete(deletePerm)
			.setReadPublished(readPublishedPerm)
			.setPublish(publishPerm);
	}

	public static String getColumnName(InternalPermission permission) {
		switch (permission) {
			case CREATE_PERM:
				return "createPerm";
			case READ_PERM:
				return "readPerm";
			case UPDATE_PERM:
				return "updatePerm";
			case DELETE_PERM:
				return "deletePerm";
			case READ_PUBLISHED_PERM:
				return "readPublishedPerm";
			case PUBLISH_PERM:
				return "publishPerm";
			default:
				throw new RuntimeException("Unknown permission " + permission.name());
		}
	}

	/**
	 * Grants the passed permissions. All other permissions remain the same.
	 * @param permissions
	 * @return
	 */
	public void grantPermissions(InternalPermission... permissions) {
		for (InternalPermission permission : permissions) {
			setPermission(permission, true);
		}
	}

	/**
	 * Gets all permissions that are granted.
	 * @return
	 */
	public Set<InternalPermission> getPermissions() {
		return Stream.of(InternalPermission.values())
			.filter(this::hasPermission)
			.collect(Collectors.toSet());
	}

}
