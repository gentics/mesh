package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.release.info.ReleaseInfoSchemaList;
import com.gentics.mesh.core.rest.release.info.ReleaseSchemaInfo;

public class ReleaseExamples extends AbstractExamples {

	public ReleaseSchemaInfo createReleaseSchemaInfo(String schemaName) {
		ReleaseSchemaInfo info = new ReleaseSchemaInfo();
		info.setName(schemaName);
		info.setUuid(randomUUID());
		info.setVersion("1.0");
		return info;
	}

	public ReleaseInfoSchemaList createSchemaReferenceList() {
		ReleaseInfoSchemaList releaseInfo = new ReleaseInfoSchemaList();
		releaseInfo.getSchemas().add(createReleaseSchemaInfo("content"));
		releaseInfo.getSchemas().add(createReleaseSchemaInfo("folder"));
		releaseInfo.getSchemas().add(createReleaseSchemaInfo("binary-data"));
		return releaseInfo;
	}

}
