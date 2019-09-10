package com.gentics.mesh.core.field;

import static com.gentics.mesh.test.ClientHelper.call;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.buffer.Buffer;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class NonI18nFieldTest extends AbstractMeshTest {

	@Test
	public void testNonI18n() {

		String parentFolderUuid = tx(() -> project().getBaseNode().getUuid());

		SchemaCreateRequest schemaRequest = new SchemaCreateRequest();
		schemaRequest.setName("testing");
		schemaRequest.addField(FieldUtil.createStringFieldSchema("i18n-name").setTranslatable(true));
		schemaRequest.addField(FieldUtil.createStringFieldSchema("non-i18n-name").setTranslatable(false));
		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaRequest));
		call(() -> client().assignSchemaToProject(projectName(), schemaResponse.getUuid()));

		NodeCreateRequest nodeRequest = new NodeCreateRequest();
		nodeRequest.setSchemaName("testing");
		nodeRequest.setLanguage("en");
		nodeRequest.getFields().put("i18n-name", FieldUtil.createStringField("i18n en name"));
		nodeRequest.getFields().put("non-i18n-name", FieldUtil.createStringField("non-i18n en name"));
		nodeRequest.setParentNodeUuid(parentFolderUuid);
		NodeResponse nodeResponse = call(() -> client().createNode(projectName(), nodeRequest));
		String uuid = nodeResponse.getUuid();

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("de");
		nodeUpdateRequest.getFields().put("i18n-name", FieldUtil.createStringField("i18n de name"));
		call(() -> client().updateNode(projectName(), uuid, nodeUpdateRequest));

		printFields(uuid);

		// Now update one language
		nodeUpdateRequest.getFields().put("non-i18n-name", FieldUtil.createStringField("non-i18n de name new!"));
		call(() -> client().updateNode(projectName(), uuid, nodeUpdateRequest));

		printFields(uuid);

	}

	@Test
	public void testNonI18nBinary() {
		String parentFolderUuid = tx(() -> project().getBaseNode().getUuid());

		SchemaCreateRequest schemaRequest = new SchemaCreateRequest();
		schemaRequest.setName("testing");
		schemaRequest.addField(FieldUtil.createStringFieldSchema("i18n-name").setTranslatable(true));
		schemaRequest.addField(FieldUtil.createStringFieldSchema("non-i18n-name").setTranslatable(false));
		schemaRequest.addField(FieldUtil.createBinaryFieldSchema("image").setTranslatable(false));

		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaRequest));
		call(() -> client().assignSchemaToProject(projectName(), schemaResponse.getUuid()));

		NodeCreateRequest nodeRequest = new NodeCreateRequest();
		nodeRequest.setSchemaName("testing");
		nodeRequest.setLanguage("en");
		nodeRequest.getFields().put("i18n-name", FieldUtil.createStringField("i18n en name"));
		nodeRequest.setParentNodeUuid(parentFolderUuid);
		NodeResponse nodeResponse = call(() -> client().createNode(projectName(), nodeRequest));
		String uuid = nodeResponse.getUuid();

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("de");
		nodeUpdateRequest.getFields().put("i18n-name", FieldUtil.createStringField("i18n de name"));
		call(() -> client().updateNode(projectName(), uuid, nodeUpdateRequest));

		Buffer buffer = Buffer.buffer("text");
		InputStream ins = new ByteArrayInputStream(buffer.getBytes());
		call(() -> client().updateNodeBinaryField(projectName(), uuid, "de", "draft", "image", ins, buffer.length(), "dummy.txt", "text/plain"));

		printFields(uuid);

	}

	private void printFields(String uuid) {
		NodeResponse deNode = call(() -> client().findNodeByUuid(projectName(), uuid, new NodeParametersImpl().setLanguages("de")));
		System.out.println(deNode.getFields().toJson());

		NodeResponse enNode = call(() -> client().findNodeByUuid(projectName(), uuid, new NodeParametersImpl().setLanguages("en")));
		System.out.println(enNode.getFields().toJson());
	}

}
