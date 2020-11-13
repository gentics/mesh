package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;

public interface HibMicroschema extends HibFieldSchemaElement<MicroschemaResponse, MicroschemaVersionModel, HibMicroschema, HibMicroschemaVersion>, HibBucketableElement {

	/**
	 * @deprecated Use {@link MicroschemaDaoWrapper} instead
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	@Deprecated
	MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags);

	HibMicroschemaVersion getLatestVersion();

	void setLatestVersion(HibMicroschemaVersion version);

	HibMicroschemaVersion findVersionByRev(String version);

	MicroschemaReference transformToReference();

	void deleteElement();

	Result<? extends HibRole> getRolesWithPerm(InternalPermission perm);


}
