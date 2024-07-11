package com.gentics.mesh.hibernate.data.domain.keys;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.gentics.mesh.core.data.role.Role;

/**
 * Private key for the permission entity. 
 * 
 * @author plyhun
 *
 */
public class HibPermissionPK implements Serializable {

	private static final long serialVersionUID = 3933032028249172278L;
	
	private UUID element;
	private Role role;

	public HibPermissionPK() {
	}

	public HibPermissionPK(UUID element, Role role) {
		this.element = element;
		this.role = role;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HibPermissionPK that = (HibPermissionPK) o;
		return element.equals(that.element) &&
			role.equals(that.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(element, role);
	}
}
