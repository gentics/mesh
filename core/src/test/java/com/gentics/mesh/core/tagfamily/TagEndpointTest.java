package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagEndpointTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");
			TagFamilyReference reference = tagFamily.transformToReference();
			assertNotNull(reference);
			assertEquals(tagFamily.getUuid(), reference.getUuid());
			assertEquals(tagFamily.getName(), reference.getName());
		}
	}

	@Test
	public void testTagFamilyProject() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");
			assertNotNull(tagFamily.getProject());
			assertEquals(project(), tagFamily.getProject());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			TagFamilyRoot root = meshRoot().getTagFamilyRoot();
			root.findAll(mockActionContext(), new PagingParametersImpl(1, 10));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			TagFamilyRoot root = meshRoot().getTagFamilyRoot();
			List<? extends TagFamily> families = root.findAll();
			assertNotNull(families);
			assertEquals(2, families.size());

			TagFamilyRoot projectTagFamilyRoot = project().getTagFamilyRoot();
			assertNotNull(projectTagFamilyRoot);

			TagFamily projectTagFamily = projectTagFamilyRoot.findByName("colors");
			assertNotNull(projectTagFamily);

			assertNotNull(projectTagFamilyRoot.create("bogus", user()));
			assertEquals(3, projectTagFamilyRoot.computeCount());
			assertEquals(3, root.computeCount());
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			TagFamilyRoot root = project().getTagFamilyRoot();
			long nProjectsBefore = root.computeCount();
			assertNotNull(root.create("test1234556", user()));
			long nProjectsAfter = root.computeCount();
			assertEquals(nProjectsBefore + 1, nProjectsAfter);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			TagFamilyRoot root = meshRoot().getTagFamilyRoot();
			assertNotNull(root);
			assertNotNull(root.findByName("colors"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			TagFamilyRoot root = project().getTagFamilyRoot();
			TagFamily tagFamily = tagFamily("colors");

			TagFamily foundTagFamily = root.findByUuid(tagFamily.getUuid());
			assertNotNull(foundTagFamily);
		}
	}

	@Test
	@Override
	public void testRead() throws IOException {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");
			assertNotNull(tagFamily.getName());
			assertEquals("colors", tagFamily.getName());
			assertNotNull(tagFamily.getEditor());
			assertNotNull(tagFamily.getCreator());
		}
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		try (Tx tx = tx()) {
			TagFamilyRoot root = project().getTagFamilyRoot();
			TagFamily family = root.create("test", user());
			TagFamily family2 = root.findByName(family.getName());
			assertNotNull(family2);
			assertEquals("test", family2.getName());
			assertEquals(family.getUuid(), family2.getUuid());
		}
	}

	@Test
	@Override
	public void testDelete() {
		try (Tx tx = tx()) {
			BulkActionContext context = createBulkContext();
			Map<String, ElementEntry> affectedElements = new HashMap<>();
			try (Tx tx2 = tx()) {
				TagFamily tagFamily = tagFamily("colors");
				affectedElements.put("tagFamily", new ElementEntry(DELETE_ACTION, tagFamily.getUuid()));

				Project project = project();
				Release release = project.getLatestRelease();

				int i = 0;
				Tag redTag = tag("red");
				affectedElements.put("tagFamily.red", new ElementEntry(DELETE_ACTION, redTag.getUuid()));
				// Tagged nodes should be updated
				for (Node node : redTag.getNodes(release)) {
					affectedElements.put("red tagged node " + i,
							new ElementEntry(STORE_ACTION, node.getUuid(), project.getUuid(), release.getUuid(),
									ContainerType.DRAFT, node.getAvailableLanguageNames()));
					i++;
				}

				Tag greenTag = tag("green");
				affectedElements.put("tagFamily.green", new ElementEntry(DELETE_ACTION, greenTag.getUuid()));
				for (Node node : greenTag.getNodes(release)) {
					affectedElements.put("green tagged node " + i,
							new ElementEntry(STORE_ACTION, node.getUuid(), project.getUuid(), release.getUuid(),
									ContainerType.DRAFT, node.getAvailableLanguageNames()));
					i++;
				}

				Tag blueTag = tag("blue");
				affectedElements.put("tagFamily.blue", new ElementEntry(DELETE_ACTION, blueTag.getUuid()));
				for (Node node : blueTag.getNodes(release)) {
					affectedElements.put("blue tagged node " + i,
							new ElementEntry(STORE_ACTION, node.getUuid(), project.getUuid(), release.getUuid(),
									ContainerType.DRAFT, node.getAvailableLanguageNames()));
					i++;
				}

				tagFamily.delete(context);
				tx2.success();
			}
			assertThat(context.batch()).containsEntries(affectedElements);
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");
			tagFamily.setName("new Name");
			assertEquals("new Name", tagFamily.getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().create("newProject", user());
			testPermission(GraphPermission.READ_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().create("newProject", user());
			testPermission(GraphPermission.DELETE_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().create("newProject", user());
			testPermission(GraphPermission.UPDATE_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			TagFamily tagFamily = project().getTagFamilyRoot().create("newProject", user());
			testPermission(GraphPermission.CREATE_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("colors");
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			TagFamilyResponse response = tagFamily.transformToRestSync(ac, 0);
			assertNotNull(response);
			assertEquals(tagFamily.getName(), response.getName());
			assertEquals(tagFamily.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			TagFamilyRoot root = project().getTagFamilyRoot();
			TagFamily tagFamily = root.create("test123", user());
			assertNotNull(tagFamily);
			String uuid = tagFamily.getUuid();
			TagFamily foundTagFamily = root.findByUuid(uuid);
			assertNotNull(foundTagFamily);
			BulkActionContext bac = createBulkContext();
			tagFamily.delete(bac);
			// TODO check for attached nodes
			Project project = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNull(project);
		}

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			TagFamilyRoot root = project().getTagFamilyRoot();
			TagFamily tagFamily = root.create("test123", user());
			assertFalse(user().hasPermission(tagFamily, GraphPermission.CREATE_PERM));
			user().addCRUDPermissionOnRole(root, GraphPermission.CREATE_PERM, tagFamily);
			assertTrue(user().hasPermission(tagFamily, GraphPermission.CREATE_PERM));
		}
	}

}
