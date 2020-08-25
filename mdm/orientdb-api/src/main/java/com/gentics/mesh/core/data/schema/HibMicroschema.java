package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface HibMicroschema extends HibFieldSchemaElement {

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

	HibMicroschemaVersion findVersionByRev(String version);

	MicroschemaReference transformToReference();

	void deleteElement();

	TraversalResult<? extends HibRole> getRolesWithPerm(InternalPermission perm);

}
