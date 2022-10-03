package com.gentics.mesh.test.definition;

import com.gentics.mesh.core.rest.error.GenericRestException;

public interface CrudEndpointTestCases {

	// Create
	void testCreate() throws Exception;

	void testCreateReadDelete() throws Exception;

	void testCreateWithNoPerm() throws Exception;

	void testCreateWithUuid() throws Exception;

	@Deprecated
	/**
	 * @deprecated Not valid, if dup UUIDs allowed across the entity types
	 * @throws Exception
	 */
	void testCreateWithDuplicateUuid() throws Exception;

	// Read
	void testReadByUUID() throws Exception;

	void testReadByUuidWithRolePerms();

	void testReadByUUIDWithMissingPermission() throws Exception;

	void testReadMultiple() throws Exception;

	void testPermissionResponse();

	// Update
	void testUpdate() throws Exception;

	void testUpdateByUUIDWithoutPerm() throws Exception;

	void testUpdateWithBogusUuid() throws GenericRestException, Exception;

	// Delete
	void testDeleteByUUID() throws Exception;

	void testDeleteByUUIDWithNoPermission() throws Exception;

	/**
	 * Test reading role permissions
	 * @throws Exception
	 */
	void testReadRolePermissions() throws Exception;

	/**
	 * Test reading role permissions without permission on the object itself
	 * @throws Exception
	 */
	void testReadRolePermissionWithoutPermission() throws Exception;

	/**
	 * Test reading role permissions without permission on all roles
	 * @throws Exception
	 */
	void testReadRolePermissionWithoutPermissionOnRole() throws Exception;
}
