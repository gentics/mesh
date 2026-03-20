package com.gentics.mesh.core.jobs;

import javax.inject.Inject;

import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * Purge microschema versions processor
 */
public class MicroschemaVersionPurgeJobProcessor extends ContentVersionPurgeJobProcessor<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion, MicroschemaModel> {

	@Inject
	public MicroschemaVersionPurgeJobProcessor(Database db, PersistingMicroschemaDao containerDao,	PersistingProjectDao projectDao, PersistingBranchDao branchDao) {
		super(db, containerDao, projectDao, branchDao);
	}
}
