package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.ContentSetupType.NO_PUBLISH_SELECT_DRAFT;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.ContentSetupType.PUBLISH_SELECT_DRAFT;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.TestField.NODE_INVERTED_UUID;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.TestField.NODE_INVERTED_UUID_REFERENCED_BY;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.TestField.NODE_UUID;
import static com.gentics.mesh.core.graphql.GraphQLNodePermissionTest.TestField.NODE_UUID_REFERENCED_BY;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This test asserts the node permissions at all places where nodes are being used in GraphQL. The variations of this test are used to validate cases for the
 * parameters:
 * <ul>
 * <li>Permissions on node (None, READ, READ_PUBLISHED)</li>
 * <li>Content Setup (variations of publishing and content selection)</li>
 * </ul>
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLNodePermissionTest extends AbstractMeshTest {

	private PermissionScenario perm;
	private ContentSetupType setup;

	private JsonObject jsonResponse;

	public static final String queryName = "node/node-permissions";
	public static final String NODE_1A_UUID = "b14469c742544efab294e0331232b9e0";
	public static final String NODE_1B_UUID = "02e3982b6ad14c1fa4a0b5998a02ff92";
	public static final String NODE_1C_UUID = "13c5981c51994d54998dd88b0c484720";
	public static final String SCHEMA_NAME = "test";

	public GraphQLNodePermissionTest(PermissionScenario perm, ContentSetupType setup) {
		this.perm = perm;
		this.setup = setup;
	}

	/**
	 * permission: Permission to be granted on the nodes
	 * 
	 * published: Whether to publish the node under test
	 * 
	 * type: The type used to load the node in GraphQL
	 * 
	 * expected: The expected test result
	 * 
	 * @return
	 */
	@Parameters(name = "permission={0},content={1}")
	public static Collection<Object[]> paramData() {
		List<Object[]> data = new ArrayList<>();
		// Generate permutations
		for (PermissionScenario perm : PermissionScenario.values()) {
			for (ContentSetupType content : ContentSetupType.values()) {
				data.add(new Object[] { perm, content });
			}
		}
		return data;
	}

	@Test
	public void testPermissions() throws IOException {
		setupContents();
		applyVariations();

		// Now execute the query and assert it
		GraphQLRequest request = new GraphQLRequest();
		JsonObject vars = new JsonObject();
		ContainerType selectedType = (setup == PUBLISH_SELECT_DRAFT || setup == NO_PUBLISH_SELECT_DRAFT) ? DRAFT : PUBLISHED;
		vars.put("type", selectedType.getHumanCode());
		vars.put("invType", selectedType == DRAFT ? PUBLISHED.getHumanCode() : DRAFT.getHumanCode());

		request.setVariables(vars);
		request.setQuery(getGraphQLQuery(queryName));
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		this.jsonResponse = new JsonObject(response.toJson());
		System.out.println(jsonResponse.encodePrettily());

		for (TestField field : TestField.values()) {
			switch (field) {
			case NODE_UUID_FIELDS:
				// Case is covered by NODE_UUID
				break;

			case NODE_UUID_PARENT:
			case NODE_UUID_CHILD:
			case NODE_UUID_INVERTED:
			case NODE_UUID_FIELDS_NODE:
				assertNode(field, NODE_UUID);
				break;

			case NODE_UUID_BREADCRUMB:
			case NODE_UUID_CHILDREN:
			case NODE_UUID_LANGUAGES:
			case NODE_UUID_REFERENCED_BY:
			case NODE_UUID_FIELDS_NODE_LIST:
				assertNodeList(field, NODE_UUID);
				break;

			case NODES:
			case TAG_NODES:
			case TAGS_NODES:
			case SCHEMA_NODES:
			case NODE_UUIDS:
				assertNodeList(field, null);
				break;

			case NODE_PATH:
			case NODE_UUID:
			case ROOT_NODE:
			case PROJECT_ROOT_NODE:
			case ME_NODE_REFERENCE:
				assertNode(field, null);
				break;

			case NODE_UUID_NOLANG:
			case NODE_INVERTED_UUID_NOLANG:
				assertNoLang(field);
				break;

			case NODE_INVERTED_UUID:
			case NODE_INVERTED_UUID_CHILD:
			case NODE_INVERTED_UUID_PARENT:
			case NODE_INVERTED_UUID_FIELDS_NODE:
				assertNode(field, NODE_INVERTED_UUID);
				break;

			case NODE_INVERTED_UUID_CHILDREN:
			case NODE_INVERTED_UUID_LANGUAGES:
			case NODE_INVERTED_UUID_BREADCRUMB:
			case NODE_INVERTED_UUID_REFERENCED_BY:
			case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
				assertNodeList(field, NODE_INVERTED_UUID);
				break;
			}

		}
	}

	private void assertNodeList(TestField field, TestField subField) {
		String subElementPath = (field == NODE_UUID_REFERENCED_BY || field == NODE_INVERTED_UUID_REFERENCED_BY) ? "[0].node" : "[0]";
		switch (perm) {
		case NO_PERM:
			switch (setup) {
			case NO_PUBLISH_SELECT_PUBLISHED:
				switch (field) {
				case NODE_UUID_FIELDS_NODE:
				case NODE_UUID_FIELDS_NODE_LIST:
					expectNode(subField, null,
						"For node fields the node which contains the fields should not contain any content since it was never published.");
					break;
				case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
					expectEmpty(field, "The list should be empty since no nodes were published");
					break;
				case NODE_INVERTED_UUID_FIELDS_NODE:
					expectHardPermFailure(field, "The user has no permission to read the node reference node.");
					break;
				default:
					expectEmpty(field, "The collection field should be empty since we are selecting published nodes without publishing.");
					break;
				}
				break;
			case NO_PUBLISH_SELECT_DRAFT:
				if (field.hasReadOnlyPerm) {
					expectSize(field, 1, "We have read perm on the collection and thus should see the elements");
					expectNode(field + subElementPath, "0.2", "The list should contain a draft element.");
				} else {
					switch (field) {
					case NODE_INVERTED_UUID_FIELDS_NODE:
					case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
						expectNode(subField, null,
							"The type is inverted and thus we select the published content which can't be loaded since it was not created.");
						break;
					default:
						if (field.listsTestNode()) {
							expectSize(field, 1, "The collection field should list the node under test.");
							expectNode(field + subElementPath, "0.2", "The list should contain the node under test as a draft.");
						} else {
							expectSize(field, 0, "The list should be empty since we have no permissions on the nodes.");
						}
						break;
					}
				}
				break;
			case PUBLISH_SELECT_DRAFT:
				if (field.hasReadOnlyPerm) {
					expectSize(field, 1, "We have read perm on the collection and thus should see the elements");
					expectNode(field + subElementPath, "1.1", "The entry should be a draft");
				} else {
					if (field.listsTestNode()) {
						expectSize(field, 1, "The collection field should list the node under test.");
						expectNode(field + subElementPath, "1.1", "The entry should be a draft");
					} else {
						expectSize(field, 0, "The list should be empty since we have no permissions on the nodes.");
					}
				}
				break;
			case PUBLISH_SELECT_PUBLISHED:
				if (field.hasReadOnlyPerm) {
					expectSize(field, 1, "We have read perm on the collection and thus should see the elements");
					expectNode(field + subElementPath, "1.0", "The entry should be published");
				} else {
					if (field.listsTestNode()) {
						expectSize(field, 1, "The collection field should list the node under test.");
						expectNode(field + subElementPath, "1.0", "The entry should be published");
					} else {
						expectSize(field, 0, "The list should be empty since we have no permissions on the nodes.");
					}
				}
				break;
			}
			break;

		case ONLY_READ:
			switch (setup) {
			case NO_PUBLISH_SELECT_DRAFT:
				switch (field) {
				case NODE_UUID_BREADCRUMB:
				case NODE_INVERTED_UUID_BREADCRUMB:
					expectNode(field + "[0]", "1.1",
						"The first entry of the breadcrumb should be the root node of the project which was previously published.");
					expectNode(field + "[1]", "0.2",
						"The second entry of the breadcrumb should be the node under test with draft version since we don't publish.");
					break;
				case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
					expectNode(subField, null, "The node should be loaded without content (due to inverted type). We thus can't check fields.");
					break;
				default:
					expectNode(field + subElementPath, "0.2", "The element should be a draft node");
					break;
				}
				break;
			case NO_PUBLISH_SELECT_PUBLISHED:
				switch (field) {
				case NODE_UUID_FIELDS_NODE_LIST:
					expectNode(subField, null,
						"For node lists fields the whole fields should be null since we never publish the content of the node.");
					break;
				default:
					expectEmpty(field, "The list should be empty since we are selecting published nodes without publishing");
					break;
				}
				break;
			case PUBLISH_SELECT_DRAFT:
				expectNode(field + subElementPath, "1.1", "The element should be a draft node.");
				break;
			case PUBLISH_SELECT_PUBLISHED:
				expectNode(field + subElementPath, "1.0", "The element should be a published node.");
				break;
			}
			break;

		case ONLY_READ_PUBLISHED:
			switch (setup) {
			case NO_PUBLISH_SELECT_DRAFT:
				switch (field) {
				case NODE_UUID_FIELDS_NODE_LIST:
					expectSoftPermFailure(subField, "The user has no permission to read the node");
					break;
				case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
					expectNode(subField, null, "The node should be loaded without content since there is no published content");
					break;
				default:
					expectEmpty(field, "The list should be empty since we have no permissions to read drafts.");
					break;
				}
				break;
			case NO_PUBLISH_SELECT_PUBLISHED:
				switch (field) {
				case NODE_UUID_FIELDS_NODE_LIST:
					expectNode(subField, null, "The node which contains the fields was never published.");
					break;
				case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
					expectSoftPermFailure(subField, "The user has no permission to read the draft content of the node that contains the fields.");
					break;
				default:
					expectEmpty(field, "The list should be empty since we are selecting published nodes without publishing");
					break;
				}
				break;
			case PUBLISH_SELECT_DRAFT:
				switch (field) {
				case NODE_UUID_FIELDS_NODE_LIST:
					expectSoftPermFailure(subField,
						"The user has no permission to read the draft content of the node that contains the fields. The fields thus can't be asserted.");
					break;
				default:
					expectEmpty(field, "The list should be empty since we have no permission to read drafts.");
					break;
				}
				break;
			case PUBLISH_SELECT_PUBLISHED:
				switch (field) {
				case NODE_INVERTED_UUID_FIELDS_NODE_LIST:
					expectSoftPermFailure(subField,
						"The user has no permission to load the draft fields of the node that contains the fields. Fields thus can't be asserted");
					break;
				default:
					expectNode(field + subElementPath, "1.0", "The entry should be published.");
					break;
				}
				break;
			}
			break;
		}
	}

	private void assertNoLang(TestField field) {
		expectNode(field, null, "The node should be loadable since we have the perms but it should not have any content.");
	}

	private void assertNode(TestField field, TestField subField) {
		boolean inverted = field.isInverted();
		boolean hasPerm = field.hasReadOnlyPerm();
		switch (perm) {
		case NO_PERM:
			switch (setup) {
			case PUBLISH_SELECT_DRAFT:
				if (hasPerm) {
					String expected = inverted ? "1.0" : "1.1";
					expectNode(field, expected, "The node should ");
				} else {
					expectHardPermFailure(field, "The user has no permission to read the node");
				}
				break;
			case PUBLISH_SELECT_PUBLISHED:
				if (hasPerm) {
					String expected = inverted ? "1.1" : "1.0";
					expectNode(field, expected, "The node should be accessible.");
				} else {
					expectHardPermFailure(field, "The user has no permission to read the node");
				}
				break;
			case NO_PUBLISH_SELECT_DRAFT:
				if (hasPerm) {
					String expected = inverted ? null : "0.2";
					expectNode(field, expected, "The node should be accessible.");
				} else {
					switch (field) {
					case NODE_INVERTED_UUID_FIELDS_NODE:
						expectNode(subField, null,
							"The node should be loaded without content since the type is inverted. We thus can't check fields.");
						break;
					default:
						expectHardPermFailure(field, "The user has no permission to read the node");
						break;
					}
				}
				break;
			case NO_PUBLISH_SELECT_PUBLISHED:
				switch (field) {
				case NODE_INVERTED_UUID_FIELDS_NODE:
					expectNode(subField, "0.2", "The draft node should be loaded since the inverted selector was used.");
					break;
				case NODE_UUID_INVERTED:
				case NODE_INVERTED_UUID:
					expectNode(field, "0.2", "The draft node should be loaded since the inverted selector was used.");
					break;
				case NODE_UUID_FIELDS_NODE:
					expectNode(subField, null, "The node should be loaded without content and thus we can't check the field.");
					break;
				default:
					if (inverted) {
						expectNode(field, "0.2", "The draft node should be loaded due to the inverted selector.");
					} else {
						if (hasPerm) {
							expectNode(field, null, "The node should be loaded without content since no publish content exists.");
						} else {
							if (field.nullOnWrongType) {
								expectNull(field, "The field should be null when requesting the non existing content.");
							} else {
								expectHardPermFailure(field, "The user has no permission to read the node");
							}
						}
					}
					break;
				}
				break;
			}
			break;
		case ONLY_READ: // CHECKED
			switch (setup) {
			case PUBLISH_SELECT_DRAFT:
				String expected = inverted ? "1.0" : "1.1";
				expectNode(field, expected, "The node with content should be loaded.");
				break;
			case PUBLISH_SELECT_PUBLISHED:
				String expected1 = inverted ? "1.1" : "1.0";
				expectNode(field, expected1, "The node with content should be loaded.");
				break;
			case NO_PUBLISH_SELECT_DRAFT:
				String expected2 = inverted ? null : "0.2";
				if (field.isPrePublished()) {
					expected2 = inverted ? null : "1.1";
				}
				switch (field) {
				case NODE_INVERTED_UUID_FIELDS_NODE:
					expectNode(subField, null, "The node should be loaded without contents (due to inverted type). We thus can't check the fields.");
					break;
				default:
					expectNode(field, expected2, "The node with should be loaded.");
					break;
				}
				break;
			case NO_PUBLISH_SELECT_PUBLISHED:
				if (subField != null) {
					if (inverted) {
						expectNode(field, "0.2", "The draft of the field should be loaded due to inverted selection.");
					} else {
						if (subField.isInverted()) {
							if (field.isInverted()) {
								expectNode(field, "0.2", "The node should be loaded without content since the node was not published.");
							} else {
								if (field.nullOnWrongType) {
									expectNull(field,
										"The field should be null since this is the default behaviour for the field when requesting the wrong type.");
								} else {
									expectNode(field, null, "Node should be loaded without content");
								}
							}
						} else {
							expectNode(subField, null, "The node should be loaded without content since the node was not published.");
						}
					}
					if (field.isNullOnWrongType()) {
						expectNull(field, "The field should be null when requesting a non existing content type");
					} else {
						if (inverted) {
							expectNode(field, "0.2", "The draft node should be loaded due to the inverted type.");
						} else {
							switch (field) {
							case NODE_UUID_FIELDS_NODE:
								expectNode(subField, null,
									"For node fields the node itself should not have content since it was never published. Thus we can't assert the fields since there are none.");
								break;
							default:
								expectNode(field, null, "The node should be loaded without content since a non existing type was selected.");
								break;
							}
						}
					}
				} else {
					if (field.isNullOnWrongType()) {
						expectNull(field, "The field should be null since it is expected that it fails with bogus types in this way.");
					} else {
						expectNode(field, null, "The node should be loaded without content since a non existing type was selected.");
					}
				}
				break;
			}
			break;
		case ONLY_READ_PUBLISHED:
			switch (setup) {
			case PUBLISH_SELECT_DRAFT:
				if (inverted) {
					expectNode(field, "1.0", "The published content should be loaded due to the inverted type.");
				} else {
					switch (field) {
					case NODE_UUID_FIELDS_NODE:
						expectSoftPermFailure(subField, "The user has no permission to read the draft content");
						break;
					default:
						expectSoftPermFailure(field, "The user has no permission to read the draft content");
						break;
					}
				}
				break;
			case PUBLISH_SELECT_PUBLISHED:
				if (inverted && subField != null) {
					expectNode(field, null,
						"The node should be loaded without content since the inverted type would select the draft content and the user is lacking perms to load drafts");
					expectSoftPermFailure(field, "The user has no permission to read the draft content");
				} else {
					switch (field) {
					case NODE_INVERTED_UUID_FIELDS_NODE:
						expectSoftPermFailure(subField, "The user has no permission to read the draft content");
						break;
					default:
						String expected = inverted ? "1.1" : "1.0";
						expectNode(field, expected, "");
						break;
					}
				}
				break;
			case NO_PUBLISH_SELECT_DRAFT:
				if (inverted) {
					expectNode(field, null,
						"The node should be loaded without content since the node was not published and the inverted type would select the published content");
				} else {
					switch (field) {
					case NODE_UUID_FIELDS_NODE:
						expectSoftPermFailure(subField, "The user has no permission to read the draft content");
						break;
					case NODE_INVERTED_UUID_FIELDS_NODE:
						expectNode(subField, null, "The node should be loaded with no content since there is no published content.");
						break;
					default:
						expectSoftPermFailure(field, "The user has no permission to read the draft content");
						break;
					}
				}
				break;
			case NO_PUBLISH_SELECT_PUBLISHED:
				if (field.isNullOnWrongType()) {
					expectNull(field, "The field should be null because this is the desired state when specifying a wrong type.");
				} else {
					switch (field) {
					case NODE_UUID_FIELDS_NODE:
						expectNode(subField, null, "The node should be loaded without content. We can't check the fields in this case.");
						break;
					case NODE_INVERTED_UUID_FIELDS_NODE:
						expectSoftPermFailure(subField, "The user has no permission to read the draft content.");
						break;
					default:
						expectNode(field, null, "The node should be loaded without content since we never published the node");
						break;
					}
				}
				break;
			}
			break;
		}
	}

	private void expectEmpty(TestField field, String msg) {
		expectEmpty(field.path(), msg);
	}

	private void expectEmpty(String path, String msg) {
		expectSize(path, 0, msg);
	}

	private void expectSize(TestField field, int size, String msg) {
		expectSize(field.path(), size, msg);
	}

	private void expectSize(String path, int size, String msg) {
		assertThat(jsonResponse).compliesTo("$.data." + path + ".length()=" + size,
			"The array should have {" + size + "} elements. " + msg);
	}

	private void expectNull(TestField field, String msg) {
		expectNull(field.path(), msg);
	}

	private void expectNull(String path, String msg) {
		assertThat(jsonResponse).compliesTo("$.data." + path + "=<is-null>", msg);
	}

	private void expectSoftPermFailure(TestField field, String msg) {
		expectSoftPermFailure(field.path(), msg);
	}

	/**
	 * Expect a perm failure for the node path that would not fail data retrieval for the node. Only the content should not be listed. We check this by
	 * validating that the sub element "version" is null.
	 * 
	 * @param path
	 * @param msg
	 */
	private void expectSoftPermFailure(String path, String msg) {
		assertThat(jsonResponse).as(msg).compliesTo("$.data." + path + ".version=<is-null>",
			"The field for path {" + path + "} should be null due to missing perms.");
		assertThatPermFailureExists(path);
	}

	private void expectHardPermFailure(TestField field, String msg) {
		expectHardPermFailure(field.path(), msg);
	}

	/**
	 * Expect a perm failure for the node path that would fail data retrieval. Thus the whole node should be null.
	 * 
	 * @param path
	 * @parm msg
	 */
	private void expectHardPermFailure(String path, String msg) {
		assertThat(jsonResponse).as(msg).compliesTo("$.data." + path + "=<is-null>",
			"The field for path {" + path + "} should be null due to missing perms.");
		assertThatPermFailureExists(path);
	}

	private void assertThatPermFailureExists(String path) {

		JsonArray errors = jsonResponse.getJsonArray("errors");
		for (int i = 0; i < errors.size(); i++) {
			JsonObject error = errors.getJsonObject(i);
			if (path.equalsIgnoreCase(error.getString("path"))) {
				assertTrue("The error for path {" + path + "} did not contain location information.", error.containsKey("locations"));

				assertEquals("The message of the found error \n{" + error.encodePrettily() + "}", "graphql_error_missing_perm",
					error.getString("message"));
				assertEquals("The type of the found error \n{" + error.encodePrettily() + "} did not match.", "missing_perm",
					error.getString("type"));
				// assertEquals(uuid, error.getString("elementId"))
				assertEquals("The type within the found error \n{" + error.encodePrettily() + "} did not match.", "node",
					error.getString("elementType"));
				return;
			}
		}
		fail("Perm error for path {" + path + "} could not be found.");
	}

	private void expectNode(TestField field, String expectedVersion, String msg) {
		expectNode(field.path(), expectedVersion, msg);
	}

	private void expectNode(String path, String expectedVersion, String msg) {
		// Assert for node without content
		if (expectedVersion == null) {
			assertThat(jsonResponse).as(msg).compliesTo("$.data." + path + ".version=<is-null>")
				.compliesTo("$.data." + path + ".isPublished=<is-null>")
				.compliesTo("$.data." + path + ".isDraft=<is-null>");
		} else {
			// Assert for node with content
			boolean expectPublished = expectedVersion.endsWith(".0");
			assertThat(jsonResponse).as(msg).compliesTo("$.data." + path + ".version=" + expectedVersion)
				.compliesTo("$.data." + path + ".isPublished=" + (expectPublished ? "true" : "false"))
				.compliesTo("$.data." + path + ".isDraft=" + (expectPublished ? "false" : "true"));
		}

	}

	private void applyVariations() {
		// Apply permissions for test run
		RolePermissionRequest permRequest = new RolePermissionRequest();
		PermissionInfo permissionsInfo = permRequest.getPermissions();
		if (perm != null) {
			switch (perm) {
			case ONLY_READ:
				permissionsInfo.set(Permission.READ, true);
				break;
			case ONLY_READ_PUBLISHED:
				permissionsInfo.set(Permission.READ_PUBLISHED, true);
				break;
			case NO_PERM:
				// Not setting any perms
			}
		}
		permissionsInfo.setOthers(false);
		permRequest.setRecursive(true);
		adminCall(
			() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes", permRequest));

		// We can't test the scenario without any permissions. We need to grant perm the
		// node under test
		if (PermissionScenario.NO_PERM == perm) {
			RolePermissionRequest permRequest2 = new RolePermissionRequest();
			permRequest2.setRecursive(false);
			permRequest2.getPermissions().set(Permission.READ, true);
			permRequest2.getPermissions().setOthers(false);
			adminCall(() -> client().updateRolePermissions(roleUuid(),
				"/projects/" + PROJECT_NAME + "/nodes/" + NODE_1B_UUID, permRequest2));
		}
	}

	private void setupContents() {

		List<String> testNodeUuids = new ArrayList<>();

		// Delete all other tags
		tx(() -> {
			for (Tag tag : tags().values()) {
				if (tag.getName().equals("blue")) {
					continue;
				} else {
					tag.delete();
				}
			}
		});

		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		SchemaCreateRequest schemaRequest = new SchemaCreateRequest();
		schemaRequest.setName(SCHEMA_NAME);
		schemaRequest.setContainer(true);
		schemaRequest.setSegmentField("name");
		schemaRequest.addField(FieldUtil.createStringFieldSchema("name"));
		schemaRequest.addField(FieldUtil.createStringFieldSchema("extra"));
		schemaRequest.addField(FieldUtil.createNodeFieldSchema("node"));
		schemaRequest.addField(FieldUtil.createListFieldSchema("nodeList").setListType("node"));
		// TODO test micronodes

		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaRequest));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		// level 1 - A [en]
		NodeCreateRequest level1ANodeCreateRequest = new NodeCreateRequest();
		level1ANodeCreateRequest.setParentNodeUuid(baseNodeUuid);
		level1ANodeCreateRequest.setSchemaName(SCHEMA_NAME);
		level1ANodeCreateRequest.setLanguage("en");
		level1ANodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level1A"));
		call(() -> client().createNode(NODE_1A_UUID, PROJECT_NAME, level1ANodeCreateRequest));
		testNodeUuids.add(NODE_1A_UUID);

		// level 1 - B [en]
		NodeCreateRequest level1BNodeCreateRequest = new NodeCreateRequest();
		level1BNodeCreateRequest.setParentNodeUuid(baseNodeUuid);
		level1BNodeCreateRequest.setSchemaName(SCHEMA_NAME);
		level1BNodeCreateRequest.setLanguage("en");
		level1BNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level1B"));
		level1BNodeCreateRequest.getFields().put("node", FieldUtil.createNodeField(NODE_1A_UUID));
		level1BNodeCreateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_1A_UUID, NODE_1A_UUID));
		call(() -> client().createNode(NODE_1B_UUID, PROJECT_NAME, level1BNodeCreateRequest));
		testNodeUuids.add(NODE_1B_UUID);

		// level 1 - C [en]
		NodeCreateRequest level1CNodeCreateRequest = new NodeCreateRequest();
		level1CNodeCreateRequest.setParentNodeUuid(baseNodeUuid);
		level1CNodeCreateRequest.setSchemaName(SCHEMA_NAME);
		level1CNodeCreateRequest.setLanguage("en");
		level1CNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level1C"));
		level1CNodeCreateRequest.getFields().put("node", FieldUtil.createNodeField(NODE_1B_UUID));
		level1CNodeCreateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_1B_UUID, NODE_1B_UUID));
		call(() -> client().createNode(NODE_1C_UUID, PROJECT_NAME, level1CNodeCreateRequest));
		testNodeUuids.add(NODE_1C_UUID);

		// level 2 [en]
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest subNodeCreateRequest = new NodeCreateRequest();
			subNodeCreateRequest.setParentNodeUuid(NODE_1B_UUID);
			subNodeCreateRequest.setSchemaName(SCHEMA_NAME);
			subNodeCreateRequest.setLanguage("en");
			subNodeCreateRequest.getFields().put("name", FieldUtil.createStringField("level2-" + i));
			subNodeCreateRequest.getFields().put("node", FieldUtil.createNodeField(NODE_1A_UUID));
			subNodeCreateRequest.getFields().put("nodeList", FieldUtil.createNodeListField(NODE_1A_UUID, NODE_1B_UUID));
			NodeResponse level2NodeResponse = call(() -> client().createNode(PROJECT_NAME, subNodeCreateRequest));
			testNodeUuids.add(level2NodeResponse.getUuid());
		}

		// Add node reference to user
		UserResponse user = call(() -> client().me());
		NodeReference nodeRef = new NodeReference();
		nodeRef.setProjectName(PROJECT_NAME);
		nodeRef.setUuid(NODE_1B_UUID);
		call(() -> client().updateUser(user.getUuid(), new UserUpdateRequest().setNodeReference(nodeRef)));

		// Tag nodes
		String tagUuid = tx(() -> tag("blue").getUuid());
		call(() -> client().addTagToNode(PROJECT_NAME, NODE_1B_UUID, tagUuid));
		call(() -> client().addTagToNode(PROJECT_NAME, NODE_1A_UUID, tagUuid));

		// Apply publish flag
		switch (setup) {
		case PUBLISH_SELECT_DRAFT:
		case PUBLISH_SELECT_PUBLISHED:
			call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid,
				new PublishParametersImpl().setRecursive(true)));
			break;

		case NO_PUBLISH_SELECT_DRAFT:
		case NO_PUBLISH_SELECT_PUBLISHED:
			call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid,
				new PublishParametersImpl().setRecursive(true)));
			break;
		}

		// Create drafts for tested data
		for (String uuid : testNodeUuids) {
			NodeUpdateRequest subNodeUpdateRequest = new NodeUpdateRequest();
			subNodeUpdateRequest.setLanguage("en");
			subNodeUpdateRequest.getFields().putString("extra", "DRAFT");
			call(() -> client().updateNode(PROJECT_NAME, uuid, subNodeUpdateRequest));
		}

		NodeUpdateRequest rootNodeUpdateRequest = new NodeUpdateRequest();
		rootNodeUpdateRequest.setLanguage("en");
		rootNodeUpdateRequest.getFields().putString("name", "root");
		call(() -> client().updateNode(PROJECT_NAME, baseNodeUuid, rootNodeUpdateRequest));
	}

	public static boolean FIXED_READ_PERM = true;
	public static boolean NO_FIXED_READ_PERM = false;

	public static boolean INVERTED = true;
	public static boolean NOT_INVERTED = false;

	public static boolean PRE_PUBLISHED = true;
	public static boolean NOT_PRE_PUBLISHED = false;

	public static boolean NULL_ON_WRONG_TYPE = true;
	public static boolean DEFAULT_PARTIAL_NODE_CONTENT = false;

	public static boolean LISTS_TEST_NODE = true;
	public static boolean DEFAULT_EMPTY = false;

	public static enum TestField {

		ME_NODE_REFERENCE("me.nodeReference", FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_UUID("nodePerUuid", FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_UUID_FIELDS("nodePerUuid.fields", FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_UUID_PARENT("nodePerUuid.parent", NO_FIXED_READ_PERM, NOT_INVERTED, PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_UUID_BREADCRUMB("nodePerUuid.breadcrumb", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			LISTS_TEST_NODE),

		NODE_UUID_LANGUAGES("nodePerUuid.languages", FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, LISTS_TEST_NODE),

		NODE_UUID_FIELDS_NODE("nodePerUuid.fields.node", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_UUID_FIELDS_NODE_LIST("nodePerUuid.fields.nodeList", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_UUID_CHILD("nodePerUuid.child", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, NULL_ON_WRONG_TYPE, DEFAULT_EMPTY),

		NODE_UUID_NOLANG("nodePerUuid.nolang", FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		SCHEMA_NODES("schema.nodes.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, LISTS_TEST_NODE),

		ROOT_NODE("rootNode", NO_FIXED_READ_PERM, NOT_INVERTED, PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		TAG_NODES("tag.nodes.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, LISTS_TEST_NODE),

		TAGS_NODES("tags.elements[0].nodes.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			LISTS_TEST_NODE),

		PROJECT_ROOT_NODE("project.rootNode", NO_FIXED_READ_PERM, NOT_INVERTED, PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_UUID_REFERENCED_BY("nodePerUuid.referencedBy.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED,
			DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_UUID_CHILDREN("nodePerUuid.children.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_PATH("nodePerPath", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, NULL_ON_WRONG_TYPE, DEFAULT_EMPTY),

		NODES("nodes.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, LISTS_TEST_NODE),

		NODE_UUIDS("uuidNodes.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, LISTS_TEST_NODE),

		NODE_UUID_INVERTED("nodePerUuid.invNodeType", FIXED_READ_PERM, INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_INVERTED_UUID("nodePerUuidInverted", FIXED_READ_PERM, INVERTED, PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_INVERTED_UUID_NOLANG("nodePerUuidInverted.nolang", FIXED_READ_PERM, INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_INVERTED_UUID_PARENT("nodePerUuidInverted.parent", NO_FIXED_READ_PERM, NOT_INVERTED, PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_INVERTED_UUID_LANGUAGES("nodePerUuidInverted.languages", FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, DEFAULT_PARTIAL_NODE_CONTENT,
			LISTS_TEST_NODE),

		NODE_INVERTED_UUID_CHILD("nodePerUuidInverted.child", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED, NULL_ON_WRONG_TYPE, DEFAULT_EMPTY),

		NODE_INVERTED_UUID_BREADCRUMB("nodePerUuidInverted.breadcrumb", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED,
			DEFAULT_PARTIAL_NODE_CONTENT, LISTS_TEST_NODE),

		NODE_INVERTED_UUID_REFERENCED_BY("nodePerUuidInverted.referencedBy.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED,
			DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY),

		NODE_INVERTED_UUID_CHILDREN("nodePerUuidInverted.children.elements", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED,
			DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_INVERTED_UUID_FIELDS_NODE("nodePerUuidInverted.fields.node", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED,
			DEFAULT_PARTIAL_NODE_CONTENT,
			DEFAULT_EMPTY),

		NODE_INVERTED_UUID_FIELDS_NODE_LIST("nodePerUuidInverted.fields.nodeList", NO_FIXED_READ_PERM, NOT_INVERTED, NOT_PRE_PUBLISHED,
			DEFAULT_PARTIAL_NODE_CONTENT, DEFAULT_EMPTY);

		/**
		 * Path to the field.
		 */
		private String path;

		/**
		 * Flag to indicate whether the field has fixed read permission.
		 */
		private boolean hasReadOnlyPerm;

		/**
		 * Flag to indicate whether the field accessed via an inverted type.
		 */
		private boolean inverted;

		/**
		 * Flag to indicate whether the node for the field was pre published in the {@link TestDataProvider}.
		 */
		private boolean prePublished;

		/**
		 * Flag to indicate whether the field should return null on mismatch types (e.g. true for path based fields).
		 */
		private boolean nullOnWrongType;

		/**
		 * Flag that indicates whether the collection field lists the node under test.
		 */
		private boolean listsTestNode;

		TestField(String path, boolean hasReadOnlyPerm, boolean inverted, boolean prePublished, boolean nullOnWrongType, boolean listsTestNode) {
			this.path = path;
			this.hasReadOnlyPerm = hasReadOnlyPerm;
			this.inverted = inverted;
			this.prePublished = prePublished;
			this.nullOnWrongType = nullOnWrongType;
			this.listsTestNode = listsTestNode;
		}

		boolean isInverted() {
			return inverted;
		}

		String path() {
			return path;
		}

		public boolean hasReadOnlyPerm() {
			return hasReadOnlyPerm;
		}

		public boolean isPrePublished() {
			return prePublished;
		}

		public boolean isNullOnWrongType() {
			return nullOnWrongType;
		}

		public boolean listsTestNode() {
			return listsTestNode;
		}

		@Override
		public String toString() {
			return path();
		}

	}

	public static enum ContentSetupType {

		// Expect draft version (1.1)
		PUBLISH_SELECT_DRAFT,

		// Expect published version (1.0)
		PUBLISH_SELECT_PUBLISHED,

		// Expect draft version (0.2)
		NO_PUBLISH_SELECT_DRAFT,

		// Expect no fields (e.g. collections should be empty)
		NO_PUBLISH_SELECT_PUBLISHED;
	}

	public static enum PermissionScenario {
		/**
		 * Grant no read permissions to most nodes. Note that NODE_1B_UUID still got permissions in order to assert nested fields.
		 */
		NO_PERM,

		/**
		 * Grant only READ permissions to all nodes.
		 */
		ONLY_READ,

		/**
		 * Grant only READ_PUBLISHED permissions to all nodes. Draft contents are thus not load-able.
		 */
		ONLY_READ_PUBLISHED
	}

}
