package com.gentics.mesh.hibernate.data;

import java.util.Arrays;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.PermissionRoots;

/**
 * Type of root permission enum
 */
public enum PermissionType {
	PROJECT(PermissionRoots.PROJECTS),
	USER(PermissionRoots.USERS),
	GROUP(PermissionRoots.GROUPS),
	ROLE(PermissionRoots.ROLES),
	MICROSCHEMA(PermissionRoots.MICROSCHEMAS),
	SCHEMA(PermissionRoots.SCHEMAS),
	BRANCH(PermissionRoots.BRANCHES),
	TAG_FAMILY(PermissionRoots.TAG_FAMILIES),
	NODE(PermissionRoots.NODES),
	MESH(PermissionRoots.MESH);

	private final String url;

	PermissionType(String url) {
		this.url = url;
	}

	/**
	 * Convert a url path to a PermissionType (e.g. "schemas" -> PermissionType.SCHEMA)
	 * @param urlStringPath
	 * @return
	 */
	public static PermissionType fromUrlStringPath(String urlStringPath) {
		return Arrays.stream(PermissionType.values())
			.filter(value -> value.url.equals(urlStringPath))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("Don't have a permission type for the provided string: " + urlStringPath));
	}

	public ElementType getElementType() {
		switch (url) {
		case PermissionRoots.PROJECTS:
			return ElementType.PROJECT;
		case PermissionRoots.USERS:
			return ElementType.USER;
		case PermissionRoots.GROUPS:
			return ElementType.GROUP;
		case PermissionRoots.ROLES:
			return ElementType.ROLE;
		case PermissionRoots.MICROSCHEMAS:
			return ElementType.MICROSCHEMA;
		case PermissionRoots.SCHEMAS:
			return ElementType.SCHEMA;
		case PermissionRoots.BRANCHES:
			return ElementType.BRANCH;
		case PermissionRoots.TAG_FAMILIES:
			return ElementType.TAGFAMILY;
		case PermissionRoots.NODES:
			return ElementType.NODE;
		}
		return null;
	}
}
