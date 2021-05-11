package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ResolveUrlFieldsTest extends AbstractMeshTest {
	public static final String SCHEMA_NAME = "urlFieldSchema";
	private String parentNodeUuid;

	@Before
	public void setUp() {
		parentNodeUuid = getProject().getRootNode().getUuid();
	}

	@Test
	public void testSingleStringField() {
		addSchema(new StringFieldSchemaImpl().setName("url"));
		NodeResponse referencedNode = addNode(FieldMap.of("url", StringField.of("/some/test/url")));
		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/test/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	@Test
	public void testMultipleStringFields() {
		addSchema(new StringFieldSchemaImpl().setName("url"), new StringFieldSchemaImpl().setName("url2"));
		NodeResponse referencedNode = addNode(FieldMap.of("url2", StringField.of("/some/test/url")));
		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/test/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	@Test
	public void testStringListField() {
		addSchema(new ListFieldSchemaImpl().setListType("string").setName("urls"));
		NodeResponse referencedNode = addNode(FieldMap.of("urls", StringFieldListImpl.of("/some/test/url", "/some/other/url")));
		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/test/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	@Test
	public void testStringListFieldWithEmptyEntry() {
		addSchema(new ListFieldSchemaImpl().setListType("string").setName("urls"));
		NodeResponse referencedNode = addNode(FieldMap.of("urls", StringFieldListImpl.of("", "/some/other/url")));
		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/other/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	@Test
	public void testMultipleStringListFields() {
		addSchema(new ListFieldSchemaImpl().setListType("string").setName("urls"), new ListFieldSchemaImpl().setListType("string").setName("urls2"));
		NodeResponse referencedNode = addNode(FieldMap.of(
			"urls", StringFieldListImpl.of("/some/test/url", "/some/other/url"),
			"urls2", StringFieldListImpl.of("/some/test/url2", "/some/other/url2")
		));
		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/test/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	@Test
	public void testMixedFields() {
		addSchema(new ListFieldSchemaImpl().setListType("string").setName("urls"), new StringFieldSchemaImpl().setName("url"));
		NodeResponse referencedNode = addNode(FieldMap.of(
			"url", StringField.of("/some/test/url")
		));
		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/test/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	/**
	 * Tests a node that has a segment field set, but one of its ascendants misses a segment field.
	 * In that case, a fallback to url fields is expected.
	 */
	@Test
	public void testWithMissingParentSegment() {
		// Create Schema
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		request.setFields(Arrays.asList(
			new StringFieldSchemaImpl().setName("url"),
			new StringFieldSchemaImpl().setName("slug")
		));
		request.setUrlFields("url");
		request.setSegmentField("slug");
		SchemaResponse schemaResponse = client().createSchema(request).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()).blockingAwait();

		// Create parent
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		nodeCreateRequest.setSchemaName("folder");
		NodeResponse parentNode = client().createNode(PROJECT_NAME, nodeCreateRequest).blockingGet();

		NodeResponse referencedNode = addNode(parentNode.getUuid(), FieldMap.of(
			"url", StringField.of("/some/test/url"),
			"slug", StringField.of("testSlug")
		));

		String result = resolveLink(referencedNode);
		assertThat(result).isEqualTo("/some/test/url");
		String resultUuid = client().webroot(PROJECT_NAME, result).blockingGet().getNodeUuid();
		assertThat(referencedNode.getUuid()).isEqualTo(resultUuid);
	}

	private String resolveLink(NodeResponse referencedNode) {
		return client().resolveLinks(
			createLink(referencedNode.getUuid()),
			new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
		).blockingGet();
	}

	private SchemaResponse addSchema(FieldSchema... fieldSchemas) {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		request.setFields(Arrays.asList(fieldSchemas));
		request.setUrlFields(Stream.of(fieldSchemas)
			.map(FieldSchema::getName)
			.collect(Collectors.toList()));
		SchemaResponse schemaResponse = client().createSchema(request).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()).blockingAwait();
		return schemaResponse;
	}

	private NodeResponse addReferencingNode(NodeResponse referencedNode) {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(parentNodeUuid);
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.setLanguage("en");
		request.setFields(FieldMap.of("content", StringField.of(createLink(referencedNode.getUuid()))));
		return client().createNode(PROJECT_NAME, request).blockingGet();
	}

	private NodeResponse addNode(String parentNodeUuid, FieldMap fields) {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(parentNodeUuid);
		request.setSchema(new SchemaReferenceImpl().setName(SCHEMA_NAME));
		request.setLanguage("en");
		request.setFields(fields);
		return client().createNode(PROJECT_NAME, request).blockingGet();
	}

	private NodeResponse addNode(FieldMap fields) {
		return addNode(parentNodeUuid, fields);
	}

	private String createLink(String uuid) {
		return String.format("{{mesh.link('%s')}}", uuid);
	}
}
