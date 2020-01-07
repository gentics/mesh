package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.Tuple;

@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = false)
public class MultiProjectMicronodeMigrationTest extends AbstractMeshTest {

	/**
	 * Test a migration of a microschema which was shared across multiple projects
	 */
	@Test
	public void testSharedMicronodeMigration() {

		int nProjects = 7;

		MicroschemaCreateRequest microschemaReq = new MicroschemaCreateRequest();
		microschemaReq.setName("micro");
		microschemaReq.addField(FieldUtil.createStringFieldSchema("name"));
		MicroschemaResponse microschema = call(() -> client().createMicroschema(microschemaReq));

		SchemaCreateRequest schemaReq = new SchemaCreateRequest();
		schemaReq.setName("testcontent");
		schemaReq.addField(FieldUtil.createMicronodeFieldSchema("microfield").setAllowedMicroSchemas("micro"));
		SchemaResponse schema = call(() -> client().createSchema(schemaReq));

		Map<String, String> projectNodes = new HashMap<>();
		// 1. Create projects
		for (int i = 0; i < nProjects; i++) {
			ProjectCreateRequest projectReq = new ProjectCreateRequest();
			projectReq.setSchemaRef("folder");
			projectReq.setName("test_" + i);
			ProjectResponse project = call(() -> client().createProject(projectReq));

			// 2. Create microschema

			// 3. Link microschema to project
			call(() -> client().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// 5. Link schema to project
			call(() -> client().assignSchemaToProject(project.getName(), schema.getUuid()));
			// 4. Create schema

			// 6. Create nodes in both projects
			NodeCreateRequest nodeReq = new NodeCreateRequest();
			nodeReq.setSchemaName(schema.getName());
			nodeReq.setParentNodeUuid(project.getRootNode().getUuid());
			nodeReq.setLanguage("en");
			Tuple<String, Field> microFields = Tuple.tuple("name", FieldUtil.createStringField("nameValue"));
			nodeReq.getFields().put("microfield", FieldUtil.createMicronodeField("micro", microFields));

			NodeResponse node = call(() -> client().createNode(project.getName(), nodeReq));
			System.out.println("Created node: " + node.getUuid());
			projectNodes.put(project.getUuid(), node.getUuid());
		}

		grantAdminRole();
		waitForJob(() -> {
			MicroschemaUpdateRequest req2 = microschema.toRequest();
			req2.addField(FieldUtil.createStringFieldSchema("name2"));
			call(() -> client().updateMicroschema(microschema.getUuid(), req2));
		});

	}
}
