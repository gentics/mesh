package com.gentics.mesh.core.data.generic;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.Dao;

/**
 * @deprecated The functionality moved into {@link Dao} - remove this class hierarchy.
 * @see PermissionProperties
 */
@Singleton
public class PermissionPropertiesImpl implements PermissionProperties {

	@Inject
	public PermissionPropertiesImpl() {
	}
}
