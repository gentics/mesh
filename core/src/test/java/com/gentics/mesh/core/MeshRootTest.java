package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;

public class MeshRootTest extends AbstractBasicDBTest {

	@Test
	public void testResolvePath() throws InterruptedException {
		// Valid paths
		expectSuccess("projects", meshRoot().getProjectRoot());
		expectSuccess("projects/" + project().getUuid(), project());
		expectSuccess("projects/" + project().getUuid() + "/schemas", project().getSchemaContainerRoot());
		expectSuccess("projects/" + project().getUuid() + "/schemas/" + schemaContainer("folder").getUuid(), schemaContainer("folder"));
		expectSuccess("projects/" + project().getUuid() + "/tagFamilies", project().getTagFamilyRoot());
		expectSuccess("projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid(), tagFamily("colors"));
		expectSuccess("projects/" + project().getUuid() + "/nodes", project().getNodeRoot());
		expectSuccess("projects/" + project().getUuid() + "/nodes/" + folder("2015").getUuid(), folder("2015"));
		expectSuccess("projects/" + project().getUuid() + "/tags", project().getTagRoot());
		expectSuccess("projects/" + project().getUuid() + "/tags/" + tag("red").getUuid(), tag("red"));

		expectSuccess("users", meshRoot().getUserRoot());
		expectSuccess("users/" + user().getUuid(), user());

		expectSuccess("roles", meshRoot().getRoleRoot());
		expectSuccess("roles/" + role().getUuid(), role());

		expectSuccess("groups", meshRoot().getGroupRoot());
		expectSuccess("groups/" + group().getUuid(), group());

		expectSuccess("schemas", meshRoot().getSchemaContainerRoot());
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

	private void expectSuccess(String path, MeshVertex vertex) throws InterruptedException {

		MeshVertex resolvedVertex = resolve(path);
		assertNotNull("We expected that the path {" + path + "} could be resolved but resolving failed.", resolvedVertex);
		assertEquals(vertex.getUuid(), resolvedVertex.getUuid());
	}

	private void expectFailure(String path) throws InterruptedException {
		assertNull("We expected that the path {" + path + "} can't be resolved successfully but it was.", resolve(path));
	}

	private MeshVertex resolve(String pathToElement) throws InterruptedException {
		return MeshRoot.getInstance().resolvePathToElement(pathToElement).toBlocking().first();
	}

}
