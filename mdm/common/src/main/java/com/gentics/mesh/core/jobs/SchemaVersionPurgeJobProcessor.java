package com.gentics.mesh.core.jobs;

import javax.inject.Inject;

import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public class SchemaVersionPurgeJobProcessor extends ContentVersionPurgeJobProcessor<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion, SchemaModel> {

	@Inject
	public SchemaVersionPurgeJobProcessor(Database db, PersistingSchemaDao schemaDao, PersistingProjectDao projectDao, PersistingBranchDao branchDao) {
		super(db, schemaDao, projectDao, branchDao);
	}
}
