package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.BuildInfo;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class MeshRootTest extends AbstractMeshTest {

	@Test
	public void testResolvePath() throws InterruptedException {
		try (Tx tx = tx()) {
			// Valid paths
			expectSuccess("projects", tx.data().permissionRoots().project());
			expectSuccess("projects/" + project().getUuid(), project());
			expectSuccess("projects/" + project().getUuid() + "/schemas", toGraph(project()).getSchemaContainerRoot());
			expectSuccess("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid(), schemaContainer("folder"));
			expectSuccess("projects/" + project().getUuid() + "/tagFamilies", toGraph(project()).getTagFamilyRoot());
			expectSuccess("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid(), tagFamily("colors"));
			expectSuccess("projects/" + project().getUuid() + "/nodes", toGraph(project()).getNodeRoot());
			expectSuccess("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid(), folder("2015"));
			expectSuccess("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/tags", tagFamily("colors"));
			expectSuccess("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/tags/" + tag("red").getUuid(),
				tag("red"));

			expectSuccess("users", tx.data().permissionRoots().user());
			expectSuccess("users/" + user().getUuid(), user());

			expectSuccess("roles", tx.data().permissionRoots().role());
			expectSuccess("roles/" + role().getUuid(), role());

			expectSuccess("groups", tx.data().permissionRoots().group());
			expectSuccess("groups/" + group().getUuid(), group());

			expectSuccess("schemas", tx.data().permissionRoots().schema());
			expectSuccess("schemas/" + schemaContainer("folder").getUuid(), schemaContainer("folder"));
			// assertNotNull(resolve("microschemas"));
			// assertNotNull(resolve("microschemas/" + mircoschemas("gallery").getUuid()));

			// Invalid paths
			expectFailure("");
			expectFailure(null);
			expectFailure("bogus");
			expectFailure("/////////");
			expectFailure("/1/2/3/4/5/6/7");
			expectFailure("projects/");
			expectFailure("projects/bogus");
			expectFailure("projects/bogus/bogus");
			expectFailure("projects/" + project().getUuid() + "/bogus");
			expectFailure("projects/" + project().getUuid() + "/tagFamilies/");
			expectFailure("projects/" + project().getUuid() + "/tagFamilies/bogus");
			expectFailure("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/");
			expectFailure("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/bogus");

			expectFailure("projects/" + project().getUuid() + "/nodes/");
			expectFailure("projects/" + project().getUuid() + "/nodes/bogus");
			expectFailure("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid() + "/");
			expectFailure("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid() + "/bogus");

			expectFailure("projects/" + project().getUuid() + "/tags/");
			expectFailure("projects/" + project().getUuid() + "/tags/bogus");
			expectFailure("projects/" + project().getUuid() + "/tags/" + folder("2015").getUuid() + "/");
			expectFailure("projects/" + project().getUuid() + "/tags/" + folder("2015").getUuid() + "/bogus");

			expectFailure("projects/" + project().getUuid() + "/schemas/");
			expectFailure("projects/" + project().getUuid() + "/schemas/bogus");
			expectFailure("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid() + "/");
			expectFailure("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid() + "/bogus");

			expectFailure("users/");
			expectFailure("users/bogus");

			expectFailure("groups/");
			expectFailure("groups/bogus");

			expectFailure("roles/");
			expectFailure("roles/bogus");

			expectFailure("schemas/");
			expectFailure("schemas/bogus");
		}
	}

	//@Test
	public void testCheckVersion() throws IOException {

		// Same version
		setMeshVersions("1.0.0", "1.0.0");
		boot().handleMeshVersion();
		boot().handleMeshVersion();

		// Minor upgrade
		setMeshVersions("1.0.0", "1.0.1");
		boot().handleMeshVersion();
		boot().handleMeshVersion();

		// Same snapshot version
		setMeshVersions("1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT");
		boot().handleMeshVersion();
		boot().handleMeshVersion();

		// Downgrade one bugfix version is allowed if the database rev is the same
		setMeshVersions("1.0.1", "1.0.0");
		setDatabaseRev(db().getDatabaseRevision());
		boot().handleMeshVersion();

		// Downgrade one bugfix version is not allowed if the database rev is different
		setMeshVersions("1.0.1", "1.0.0");
		setDatabaseRev("different");
		expectException(() -> {
			boot().handleMeshVersion();
		});

		// Upgrade from snapshot to branch
		setMeshVersions("1.0.0-SNAPSHOT", "1.0.0");
		expectException(() -> {
			boot().handleMeshVersion();
		});

		// Downgrade to snapshot version
		setMeshVersions("1.0.0", "1.0.0-SNAPSHOT");
		expectException(() -> {
			boot().handleMeshVersion();
		});

		// Upgrade from snapshot to branch - With ignore flag
		System.setProperty("ignoreSnapshotUpgradeCheck", "true");
		setMeshVersions("1.0.0-SNAPSHOT", "1.0.0");
		boot().handleMeshVersion();

	}

	private void setDatabaseRev(String rev) {
		try (Tx tx = tx()) {
			tx.data().meshVersion().setDatabaseRevision(rev);
			tx.success();
		}
	}

	private void setMeshVersions(String graphVersion, String buildVersion) throws IOException {
		MeshVersion.buildInfo.set(new BuildInfo(buildVersion, null));
		assertEquals(buildVersion, Mesh.getPlainVersion());

		try (Tx tx = tx()) {
			tx.data().meshVersion().setMeshVersion(graphVersion);
			tx.success();
		}
	}

	private void expectException(Runnable action) {
		try {
			action.run();
			fail("An exception should have been thrown.");
		} catch (Exception e) {
			String msg = e.getMessage();
			assertTrue("We did not expect the message {" + msg + "}", msg.startsWith("Downgrade not allowed"));
		}
	}

	@Deprecated
	private void expectSuccess(String path, HibBaseElement vertex) throws InterruptedException {
		HibBaseElement resolvedVertex = resolve(path);
		assertNotNull("We expected that the path {" + path + "} could be resolved but resolving failed.", resolvedVertex);
		assertEquals(vertex.getUuid(), resolvedVertex.getUuid());
	}

	private void expectFailure(String path) throws InterruptedException {
		boolean error = false;
		try {
			HibBaseElement vertex = resolve(path);
			assertNull("We expected that the path {" + path + "} can't be resolved successfully but it was.", vertex);
			error = true;
		} catch (Exception e) {
			error = true;
		}
		if (!error) {
			fail("No exception occured but we expected an error while resolving path {" + path + "}");
		}
	}

	private HibBaseElement resolve(String pathToElement) throws InterruptedException {
		return boot().rootResolver().resolvePathToElement(pathToElement);
	}

}
