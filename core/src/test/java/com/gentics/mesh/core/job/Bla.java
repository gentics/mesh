package com.gentics.mesh.core.job;

import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;

public class Bla {
	private final MeshRestClient client;
	private final String schemaUuid = "54c511567f054e418511567f056e4178"; // intranet_content
	private final String projectName = "Intranet_Neu_Master";

	public Bla() {
		this.client = MeshRestClient.create("localhost", 8080, false);
	}

	public void start() {
		client.setLogin("admin", "admin");
		client.login().blockingGet();

		System.out.println("Updating schema...");
		SchemaResponse schema = updateSchema();
		System.out.println("Assigning to branches...");
		assignSchemaToBranches(schema);
	}

	private void assignSchemaToBranches(SchemaResponse schema) {
		BranchListResponse branches = client.findBranches(projectName).blockingGet();
		branches.getData()
//			.stream().limit(1)
			.forEach(branch -> {
			System.out.println(String.format("Assingning to %s", branch.getName()));
			client.assignBranchSchemaVersions(projectName, branch.getUuid(), schema.toReference()).blockingAwait();
		});
	}

	private SchemaResponse updateSchema() {
		SchemaResponse schema = client.findSchemaByUuid(schemaUuid).blockingGet();
		SchemaUpdateRequest request = schema.toUpdateRequest();
		request.setDescription(RandomStringUtils.randomAlphabetic(10));
		client.updateSchema(schemaUuid, request, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)).blockingAwait();
		return client.findSchemaByUuid(schemaUuid).blockingGet();
	}

	public static void main(String[] args) {
		new Bla().start();
	}
}
