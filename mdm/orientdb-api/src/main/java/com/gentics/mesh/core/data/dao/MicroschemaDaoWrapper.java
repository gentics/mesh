package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.result.Result;

/**
 * DAO for {@link HibMicroschema} operations.
 */
public interface MicroschemaDaoWrapper extends MicroschemaDao, DaoWrapper<HibMicroschema>, DaoTransformable<HibMicroschema, MicroschemaResponse> {

	/**
	 * Return all draft contents which reference the microschema version.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	Result<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
		String branchUuid);
}
