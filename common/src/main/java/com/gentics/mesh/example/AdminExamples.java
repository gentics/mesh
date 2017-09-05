package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.migration.MigrationInfo;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatusResponse;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;

public class AdminExamples {

	public MeshStatusResponse createMeshStatusResponse(MeshStatus status) {
		return new MeshStatusResponse().setStatus(status);
	}

	public ClusterStatusResponse createClusterStatusResponse() {
		return new ClusterStatusResponse();
	}

	public MigrationStatusResponse createMigrationStatusResponse() {
		MigrationStatusResponse status = new MigrationStatusResponse();
		MigrationInfo info = new MigrationInfo();
		info.setDone(20);
		info.setTotal(150);
		info.setNodeName("local");
		info.setStatus(MigrationStatus.RUNNING);
		info.setSourceName("content");
		info.setTargetVersion("content");
		info.setSourceVersion("1.0");
		info.setTargetVersion("2.0");
		info.setStartDate("2017-07-25T12:40:00Z");
		info.setType(MigrationType.schema);
		info.setUuid(randomUUID());
		status.getMigrations().add(info);
		return status;
	}

}
