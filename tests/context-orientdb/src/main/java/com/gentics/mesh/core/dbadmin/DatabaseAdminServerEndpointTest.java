package com.gentics.mesh.core.dbadmin;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.rest.dbadmin.DatabaseAdminClientConfig;
import com.gentics.mesh.rest.dbadmin.DatabaseAdminRestClient;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true, inMemoryDB = false)
public class DatabaseAdminServerEndpointTest extends AbstractMeshTest {

	private DatabaseAdminRestClient dbAdminClient = null;

	@Before
	public void setupClient() {
		DatabaseAdminClientConfig dbAdminClientConfig = DatabaseAdminClientConfig.builder()
			.withBasePath(CURRENT_API_BASE_PATH)
			.withHost("localhost")
			.withPort(((OrientDBMeshOptions) options()).getStorageOptions().getAdministrationOptions().getPort())
			.build();

		dbAdminClient = DatabaseAdminRestClient.create(dbAdminClientConfig);
	}

	@Test
	public void testLocalClientStartStopDatabase() {
		call(() -> client().webroot(projectName(), "/"));
		call(() -> dbAdminClient.stopDatabase());
		try {
			call(() -> client().webroot(projectName(), "/"));
			fail("findProjects should fail with database turned off");
		} catch (Throwable e) {
		}
		call(() -> dbAdminClient.startDatabase());
		call(() -> client().webroot(projectName(), "/"));	
	}

}
