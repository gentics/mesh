package com.gentics.mesh.example;

import static com.gentics.mesh.example.ExampleUuids.BRANCH_UUID;

import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaListModel;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;

public class BranchExamples extends AbstractExamples {

	public BranchSchemaInfo createBranchSchemaInfo(String schemaName) {
		BranchSchemaInfo info = new BranchSchemaInfo();
		info.setName(schemaName);
		info.setUuid(BRANCH_UUID);
		info.setVersion("1.0");
		return info;
	}

	public BranchInfoSchemaListModel createSchemaReferenceList() {
		BranchInfoSchemaListModel branchInfo = new BranchInfoSchemaListModel();
		branchInfo.getSchemas().add(createBranchSchemaInfo("content"));
		branchInfo.getSchemas().add(createBranchSchemaInfo("folder"));
		branchInfo.getSchemas().add(createBranchSchemaInfo("binary-data"));
		return branchInfo;
	}

}
