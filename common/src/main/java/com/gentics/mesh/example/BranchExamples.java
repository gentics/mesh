package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;

public class BranchExamples extends AbstractExamples {

	public BranchSchemaInfo createBranchSchemaInfo(String schemaName) {
		BranchSchemaInfo info = new BranchSchemaInfo();
		info.setName(schemaName);
		info.setUuid(randomUUID());
		info.setVersion("1.0");
		return info;
	}

	public BranchInfoSchemaList createSchemaReferenceList() {
		BranchInfoSchemaList branchInfo = new BranchInfoSchemaList();
		branchInfo.getSchemas().add(createBranchSchemaInfo("content"));
		branchInfo.getSchemas().add(createBranchSchemaInfo("folder"));
		branchInfo.getSchemas().add(createBranchSchemaInfo("binary-data"));
		return branchInfo;
	}

}
