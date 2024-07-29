package com.gentics.mesh.example;

import static com.gentics.mesh.FieldUtil.createBooleanField;
import static com.gentics.mesh.FieldUtil.createDateField;
import static com.gentics.mesh.FieldUtil.createHtmlField;
import static com.gentics.mesh.FieldUtil.createMicronodeField;
import static com.gentics.mesh.FieldUtil.createMicronodeListField;
import static com.gentics.mesh.FieldUtil.createNewMicronodeField;
import static com.gentics.mesh.FieldUtil.createNodeField;
import static com.gentics.mesh.FieldUtil.createNodeListField;
import static com.gentics.mesh.FieldUtil.createNumberField;
import static com.gentics.mesh.FieldUtil.createNumberListField;
import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.FieldUtil.createStringListField;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.NODE_AUTOMOBILES_CATEGEORY_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_ROOT_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_GREEN_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_RED_UUID;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;
import static com.gentics.mesh.example.ExampleUuids.UUID_2;
import static com.gentics.mesh.example.ExampleUuids.UUID_3;
import static com.gentics.mesh.example.ExampleUuids.UUID_4;
import static com.gentics.mesh.example.ExampleUuids.UUID_5;
import static com.gentics.mesh.example.ExampleUuids.UUID_6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.FormParameter;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageRect;
import com.gentics.mesh.util.Tuple;

public class NodeExamples extends AbstractExamples {

	public NodeResponse getNodeResponseWithAllFields() {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(NODE_DELOREAN_UUID);
		NodeReference parentNodeReference = new NodeReference();
		parentNodeReference.setUuid(NODE_ROOT_UUID);
		parentNodeReference.setDisplayName("parentNodeDisplayName");
		nodeResponse.setParentNode(parentNodeReference);
		nodeResponse.setCreated(createOldTimestamp());
		nodeResponse.setEdited(createNewTimestamp());
		nodeResponse.setCreator(createUserReference());
		nodeResponse.setTags(Arrays.asList(new TagReference().setName("red").setUuid(TAG_RED_UUID).setTagFamily("colors")));
		nodeResponse.setPath(MeshVersion.CURRENT_API_BASE_PATH + "/yourProject/webroot/Images");
		Map<String, PublishStatusModel> languageInfo = new HashMap<>();

		languageInfo.put("de", new PublishStatusModel().setVersion("1.0").setPublished(true).setPublishDate(createOldTimestamp()).setPublisher(
			createUserReference()));
		languageInfo.put("en", new PublishStatusModel().setVersion("1.1").setPublished(false).setPublishDate(createOldTimestamp()).setPublisher(
			createUserReference()));

		nodeResponse.setAvailableLanguages(languageInfo);
		HashMap<String, String> languagePaths = new HashMap<>();
		languagePaths.put("en", MeshVersion.CURRENT_API_BASE_PATH + "/yourProject/webroot/Images");
		languagePaths.put("de", MeshVersion.CURRENT_API_BASE_PATH + "/yourProject/webroot/Bilder");
		nodeResponse.setLanguagePaths(languagePaths);
		nodeResponse.setChildrenInfo(new HashMap<>());
		nodeResponse.getChildrenInfo().put("blogpost", new NodeChildrenInfo().setCount(1).setSchemaUuid(UUID_2));
		nodeResponse.getChildrenInfo().put("folder", new NodeChildrenInfo().setCount(5).setSchemaUuid(UUID_3));

		FieldMap fields = new FieldMapImpl();
		fields.put("name", createStringField("Name for language tag de-DE"));
		fields.put("filename", createStringField("dummy-content.de.html"));
		fields.put("teaser", createStringField("Dummy teaser for de-DE"));
		fields.put("content", createHtmlField("Content for language tag de-DE"));
		fields.put("relatedProduct", createNodeField(UUID_1));
		fields.put("price", createNumberField(100.1));
		fields.put("enabled", createBooleanField(true));
		fields.put("branch", createDateField(createOldTimestamp()));
		fields.put("categories", createNodeListField(UUID_4, UUID_5, UUID_6));
		fields.put("names", createStringListField("Jack", "Joe", "Mary", "Tom"));
		fields.put("categoryIds", createNumberListField(1, 42, 133, 7));
		fields.put("binary", createBinaryField());
		fields.put("location", createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)), Tuple.tuple(
			"longitude", createNumberField(16.373063840833))));
		fields.put("locations", createMicronodeListField(createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(
			48.208330230278)), Tuple.tuple("longitude", createNumberField(16.373063840833))), createMicronodeField("geolocation", Tuple.tuple(
				"latitude", createNumberField(48.137222)), Tuple.tuple("longitude", createNumberField(11.575556)))));
		nodeResponse.setFields(fields);

		nodeResponse.setSchema(getSchemaReference("content"));
		nodeResponse.setPermissions(READ, UPDATE, DELETE, CREATE);

		// breadcrumb
		List<NodeReference> breadcrumb = new ArrayList<>();
		// breadcrumb.add(new NodeReferenceImpl().setDisplayName("/").setPath("/").setUuid(randomUUID()));
		breadcrumb.add(new NodeReference().setDisplayName("news").setPath("/news").setUuid(NODE_DELOREAN_UUID));
		breadcrumb.add(new NodeReference().setDisplayName("2015").setPath("/automobiles/delorean-dmc-12").setUuid(NODE_AUTOMOBILES_CATEGEORY_UUID));
		nodeResponse.setBreadcrumb(breadcrumb);

		// tags
		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("red").setUuid(TAG_RED_UUID).setTagFamily("colors"));
		tags.add(new TagReference().setName("green").setUuid(TAG_GREEN_UUID).setTagFamily("colors"));

		tags.add(new TagReference().setName("car").setUuid(UUID_1));
		tags.add(new TagReference().setName("ship").setUuid(UUID_2));
		nodeResponse.setTags(tags);

		return nodeResponse;
	}

	public static Field createBinaryField() {
		BinaryField binaryField = new BinaryFieldImpl();
		binaryField.setFileName("flower.jpg");
		binaryField.setDominantColor("#22a7f0");
		binaryField.setFocalPoint(0.1f, 0.2f);
		binaryField.setFileSize(95365);
		binaryField.setWidth(800);
		binaryField.setHeight(600);
		binaryField.setMimeType("image/jpeg");
		binaryField.setSha512sum(
			"ec582eb760034dd91d5fd33656c0b56f082b7365d32e2a139dd9c87ebc192bff3525f32ff4c4137463a31cad020ac19e6e356508db2b90e32d737b6d725e14c1");
		return binaryField;
	}

	public NavigationResponse getNavigationResponse() {
		NavigationResponse response = new NavigationResponse();
		String rootUuid = NODE_ROOT_UUID;

		// Level 0
		NodeResponse rootElement = getNodeResponseWithAllFields();
		rootElement.setUuid(rootUuid);
		response.setUuid(rootUuid);
		response.setNode(rootElement);
		response.setChildren(new ArrayList<>());

		// Level 1
		NavigationElement navElement = new NavigationElement();
		String navElementUuid = UUID_1;
		NodeResponse navElementNode = getNodeResponseWithAllFields();
		navElementNode.setUuid(navElementUuid);
		navElement.setUuid(navElementUuid);
		navElement.setNode(navElementNode);
		response.getChildren().add(navElement);

		return response;
	}

	public NodeResponse getNodeResponse2() {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(UUID_1);
		nodeResponse.setSchema(getSchemaReference("content"));

		NodeReference parentNodeReference = new NodeReference();
		parentNodeReference.setUuid(NODE_ROOT_UUID);
		parentNodeReference.setDisplayName("parentNodeDisplayName");

		nodeResponse.setParentNode(parentNodeReference);
		nodeResponse.setCreator(createUserReference());
		nodeResponse.setCreated(createOldTimestamp());
		nodeResponse.setEdited(createNewTimestamp());
		nodeResponse.setEditor(createUserReference());

		FieldMap fields = new FieldMapImpl();
		fields.put("name", createStringField("Name for language tag en"));
		fields.put("filename", createStringField("dummy-content.en.html"));
		fields.put("teaser", createStringField("Dummy teaser for en"));
		fields.put("content", createStringField("Content for language tag en"));
		nodeResponse.setFields(fields);

		nodeResponse.setPermissions(READ, CREATE);

		// breadcrumb
		List<NodeReference> breadcrumb = new ArrayList<>();
		// breadcrumb.add(new NodeReferenceImpl().setDisplayName("/").setPath("/").setUuid(randomUUID()));
		breadcrumb.add(new NodeReference().setDisplayName("Automobiles").setPath("/automobiles").setUuid(NODE_AUTOMOBILES_CATEGEORY_UUID));
		breadcrumb.add(new NodeReference().setDisplayName("DeLorean DMC-12").setPath("/automobiles/delorean-dmc-12").setUuid(NODE_DELOREAN_UUID));
		nodeResponse.setBreadcrumb(breadcrumb);

		// tags
		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("red").setUuid(TAG_RED_UUID).setTagFamily("colors"));
		tags.add(new TagReference().setName("green").setUuid(TAG_GREEN_UUID).setTagFamily("colors"));
		tags.add(new TagReference().setName("car").setUuid(UUID_1).setTagFamily("vehicles"));
		tags.add(new TagReference().setName("ship").setUuid(UUID_2).setTagFamily("vehicles"));
		nodeResponse.setTags(tags);

		return nodeResponse;
	}

	public NodeCreateRequest getNodeCreateRequest2() {
		NodeCreateRequest contentCreate = new NodeCreateRequest();
		contentCreate.setParentNodeUuid(UUID_1);
		contentCreate.setLanguage("en");
		contentCreate.setSchema(getSchemaReference("content"));

		FieldMap fields = contentCreate.getFields();
		fields.put("name", createStringField("English name"));
		fields.put("filename", createStringField("index.en.html"));
		fields.put("content", createStringField("English content"));
		fields.put("title", createStringField("English title"));
		fields.put("teaser", createStringField("English teaser"));
		fields.put("relatedProduct", createNodeField(NODE_AUTOMOBILES_CATEGEORY_UUID));
		fields.put("price", createNumberField(100.1));
		fields.put("enabled", createBooleanField(true));
		fields.put("branch", createDateField(createOldTimestamp()));
		fields.put("categories", createNodeListField(UUID_2, UUID_3, UUID_4));
		fields.put("names", createStringListField("Jack", "Joe", "Mary", "Tom"));
		fields.put("categoryIds", createNumberListField(1, 42, 133, 7));
		fields.put("location", createNewMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)), Tuple.tuple(
			"longitude", createNumberField(16.373063840833))));
		fields.put("locations", createMicronodeListField(createNewMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(
			48.208330230278)), Tuple.tuple("longitude", createNumberField(16.373063840833))), createNewMicronodeField("geolocation", Tuple.tuple(
				"latitude", createNumberField(48.137222)), Tuple.tuple("longitude", createNumberField(11.575556)))));

		return contentCreate;
	}

	public NodeCreateRequest getNodeCreateRequest() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(NODE_AUTOMOBILES_CATEGEORY_UUID);
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("vehicle"));
		nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString("DeLorean DMC-12"));
		nodeCreateRequest.getFields().put("description", new HtmlFieldImpl().setHTML(
			"The DeLorean DMC-12 is a sports car manufactured by John DeLorean's DeLorean Motor Company for the American market from 1981–83."));
		return nodeCreateRequest;
	}

	public NodeUpdateRequest getNodeUpdateRequest2() {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("1.0");
		nodeUpdateRequest.getFields().put("weight", new NumberFieldImpl().setNumber(1230));
		return nodeUpdateRequest;
	}

	public BinaryFieldTransformRequest getBinaryFieldTransformRequest() {
		BinaryFieldTransformRequest request = new BinaryFieldTransformRequest();
		request.setHeight(200);
		request.setWidth(100);
		request.setCropRect(50, 20, 150, 170);
		request.setLanguage("en");
		request.setVersion("1.0");
		request.setFocalPoint(new FocalPoint(0.3f,0.6f));
		return request;
	}

	public NodeUpdateRequest getNodeUpdateRequest() {
		NodeUpdateRequest nodeUpdate = new NodeUpdateRequest();
		nodeUpdate.setLanguage("en");
		nodeUpdate.setVersion("1.0");

		FieldMap fields = nodeUpdate.getFields();
		fields.put("filename", createStringField("index-renamed.en.html"));
		fields.put("relatedProduct-", createNodeField(UUID_1));
		fields.put("price", createNumberField(100.1));
		fields.put("enabled", createBooleanField(true));
		fields.put("branch", createDateField(createOldTimestamp()));
		fields.put("categories", createNodeListField(UUID_2, UUID_3, UUID_4));
		fields.put("image", createBinaryField());
		fields.put("names", createStringListField("Jack", "Joe", "Mary", "Tom"));
		fields.put("categoryIds", createNumberListField(1, 42, 133, 7));
		fields.put("location", createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)), Tuple.tuple(
			"longitude", createNumberField(16.373063840833))));
		fields.put("locations", createMicronodeListField(createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(
			48.208330230278)), Tuple.tuple("longitude", createNumberField(16.373063840833))), createMicronodeField("geolocation", Tuple.tuple(
				"latitude", createNumberField(48.137222)), Tuple.tuple("longitude", createNumberField(11.575556)))));

		return nodeUpdate;
	}

	public NodeListResponse getNodeListResponse() {
		NodeListResponse list = new NodeListResponse();
		list.getData().add(getNodeResponseWithAllFields());
		list.getData().add(getNodeResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

	public Map<String, List<FormParameter>> getExampleBinaryUploadFormParameters() {
		Map<String, List<FormParameter>> parameters = new HashMap<>();
		FormParameter versionParameter = new FormParameter();
		versionParameter.setExample("1.0");
		versionParameter.setType(ParamType.STRING);
		versionParameter.setDescription("Version of the node which should be updated. This information is used to determine conflicting updates.");
		versionParameter.setRequired(true);
		parameters.put("version", Arrays.asList(versionParameter));

		FormParameter languageParameter = new FormParameter();
		languageParameter.setExample("en");
		languageParameter.setType(ParamType.STRING);
		languageParameter.setDescription("Language of the node content which contains the binary field which should be updated.");
		languageParameter.setRequired(true);
		parameters.put("language", Arrays.asList(languageParameter));

		FormParameter binaryParameter = new FormParameter();
		binaryParameter.setDescription("Single binary file part.");
		binaryParameter.setRequired(true);
		binaryParameter.setType(ParamType.FILE);
		parameters.put("binary", Arrays.asList(binaryParameter));
		return parameters;
	}

	public Map<String, List<FormParameter>> getExampleBinaryCheckCallbackParameters() {
		Map<String, List<FormParameter>> parameters = new HashMap<>();
		FormParameter statusParameter = new FormParameter();
		statusParameter.setExample("DENIED");
		statusParameter.setType(ParamType.STRING);
		statusParameter.setDescription("The result of the binary check. One of ACCEPTED or DENIED.");
		statusParameter.setRequired(true);
		parameters.put("status", Arrays.asList(statusParameter));

		FormParameter reasonParameter = new FormParameter();
		reasonParameter.setExample("Malware detected");
		reasonParameter.setType(ParamType.STRING);
		reasonParameter.setDescription("The reason why the binary was denied.");
		reasonParameter.setRequired(false);
		parameters.put("reason", Arrays.asList(reasonParameter));

		return parameters;
	}

	public NodeVersionsResponse createVersionsList() {
		NodeVersionsResponse response = new NodeVersionsResponse();
		List<VersionInfo> list = new ArrayList<>();
		list.add(new VersionInfo().setCreated(createNewTimestamp()).setCreator(createUserReference()).setVersion("1.0"));
		list.add(new VersionInfo().setCreated(createNewTimestamp()).setCreator(createUserReference()).setVersion("1.1"));
		list.add(new VersionInfo().setCreated(createNewTimestamp()).setCreator(createUserReference()).setVersion("2.0"));
		Map<String, List<VersionInfo>> versions = new HashMap<>();
		versions.put("en", list);
		versions.put("de", list);
		response.setVersions(versions);
		return response;
	}

	public ImageVariantsResponse createImageVariantsResponse() {
		ImageVariantsResponse imageVariantsResponse = new ImageVariantsResponse();
		List<ImageVariantResponse> variants = new ArrayList<>(3);
		variants.add(new ImageVariantResponse().setHeight(100).setWidth(500).setOrigin(true));
		variants.add(new ImageVariantResponse().setCropMode(CropMode.RECT).setRect(new ImageRect(10, 10, 20, 20)));
		variants.add(new ImageVariantResponse().setHeight(10).setHeight(50));
		imageVariantsResponse.setVariants(variants);
		return imageVariantsResponse;
	}

	public ImageManipulationRequest createImageManipulationRequest() {
		ImageManipulationRequest imageManipulationRequest = new ImageManipulationRequest();
		List<ImageVariantRequest> variants = new ArrayList<>(2);
		variants.add(new ImageVariantRequest().setHeight(100).setWidth("auto"));
		variants.add(new ImageVariantRequest().setFocalPoint(0.3f, 0.3f).setFocalPointZoom(0.6f));
		imageManipulationRequest.setVariants(variants);
		return imageManipulationRequest;
	}
}
