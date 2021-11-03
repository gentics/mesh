package com.gentics.mesh.search.transformer;

import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = false)
public class NodeContainerTransformerTest extends AbstractMeshTest {

	@Test
	public void testNodeTagFamilyTransformer() {
		NodeContainerTransformer transformer = new NodeContainerTransformer(options());
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibBranch branch = project().getLatestBranch();
			HibNodeFieldContainer node = contentDao.getFieldContainer(content("concorde"), english(), branch, PUBLISHED);
			JsonObject document = transformer.toDocument(node, branch.getUuid(), PUBLISHED);
			JsonObject families = document.getJsonObject("tagFamilies");

			HashSet<String> basicNames = new HashSet<>(Arrays.asList("Plane", "Twinjet"));
			HashSet<String> colorNames = new HashSet<>(Arrays.asList("red"));

			JsonArray basicArray = families.getJsonObject("basic").getJsonArray("tags");
			JsonArray colorArray = families.getJsonObject("colors").getJsonArray("tags");

			assertEquals("Incorrect count of basic tags", basicNames.size(), basicArray.size());
			assertEquals("Incorrect count of colors", colorNames.size(), colorArray.size());

			boolean allTagsContained = basicArray.stream().map(obj -> ((JsonObject) obj).getString("name")).allMatch(name -> basicNames.contains(
					name));
			boolean allColorsContained = colorArray.stream().map(obj -> ((JsonObject) obj).getString("name")).allMatch(name -> colorNames.contains(
					name));

			assertTrue("Could not find all basic tags", allTagsContained);
			assertTrue("Could not find all colors", allColorsContained);

			JsonArray roleUuids = document.getJsonArray("_roleUuids");
			assertEquals("The role information was not correctly set", 2, roleUuids.size());
		}
	}
}
