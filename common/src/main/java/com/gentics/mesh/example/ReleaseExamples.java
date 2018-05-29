package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;

public class ReleaseExamples extends AbstractExamples {

	public BranchSchemaInfo createReleaseSchemaInfo(String schemaName) {
		BranchSchemaInfo info = new BranchSchemaInfo();
		info.setName(schemaName);
		info.setUuid(randomUUID());
		info.setVersion("1.0");
		return info;
	}

	public BranchInfoSchemaList createSchemaReferenceList() {
		BranchInfoSchemaList releaseInfo = new BranchInfoSchemaList();
		releaseInfo.getSchemas().add(createReleaseSchemaInfo("content"));
		releaseInfo.getSchemas().add(createReleaseSchemaInfo("folder"));
		releaseInfo.getSchemas().add(createReleaseSchemaInfo("binary-data"));
		return releaseInfo;
	}

}
