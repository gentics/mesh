package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;

public interface HibMicroschema
	extends HibFieldSchemaElement<MicroschemaResponse, MicroschemaVersionModel, HibMicroschema, HibMicroschemaVersion>, HibBucketableElement {

	/**
	 * @deprecated Use {@link MicroschemaDaoWrapper} instead
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	@Deprecated
	MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags);

	/**
	 * Return the latest version.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getLatestVersion();

	/**
	 * Update the latest version reference.
	 * 
	 * @param version
	 */
	void setLatestVersion(HibMicroschemaVersion version);

	/**
	 * Find the version of the microschema.
	 * 
	 * @param version
	 * @return
	 */
	HibMicroschemaVersion findVersionByRev(String version);

	/**
	 * Transform the microschema to a reference POJO.
	 * 
	 * @return
	 */
	MicroschemaReference transformToReference();

	/**
	 * Delete the microschema.
	 */
	void deleteElement();

	/**
	 * Load all roles with the given permission that are listed for the microschema.
	 * 
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(InternalPermission perm);

}
