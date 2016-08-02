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
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.util.Tuple;

public class NodeExamples extends AbstractExamples {

	public NodeResponse getNodeResponseWithAllFields() {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(randomUUID());
		NodeReferenceImpl parentNodeReference = new NodeReferenceImpl();
		parentNodeReference.setUuid(randomUUID());
		parentNodeReference.setDisplayName("parentNodeDisplayName");
		nodeResponse.setParentNode(parentNodeReference);
		nodeResponse.setCreated(getTimestamp());
		nodeResponse.setEdited(getTimestamp());
		nodeResponse.setCreator(getUserReference());
		TagFamilyTagGroup tagGroup = new TagFamilyTagGroup().setUuid(randomUUID());
		tagGroup.getItems().add(new TagReference().setName("red").setUuid(randomUUID()));
		nodeResponse.getTags().put("colors",tagGroup);
		nodeResponse.setPath("/api/v1/yourProject/webroot/Images");
		nodeResponse.setAvailableLanguages(Arrays.asList("en", "de"));
		HashMap<String, String> languagePaths = new HashMap<>();
		languagePaths.put("en", "/api/v1/yourProject/webroot/Images");
		languagePaths.put("de", "/api/v1/yourProject/webroot/Bilder");
		nodeResponse.setLanguagePaths(languagePaths);
		nodeResponse.getChildrenInfo().put("blogpost", new NodeChildrenInfo().setCount(1).setSchemaUuid(randomUUID()));
		nodeResponse.getChildrenInfo().put("folder", new NodeChildrenInfo().setCount(5).setSchemaUuid(randomUUID()));

		FieldMap fields = nodeResponse.getFields();
		fields.put("name-stringField", createStringField("Name for language tag de-DE"));
		fields.put("filename-stringField", createStringField("dummy-content.de.html"));
		fields.put("teaser-stringField", createStringField("Dummy teaser for de-DE"));
		fields.put("content-htmlField", createHtmlField("Content for language tag de-DE"));
		fields.put("relatedProduct-nodeField", createNodeField(randomUUID()));
		fields.put("price-numberField", createNumberField(100.1));
		fields.put("enabled-booleanField", createBooleanField(true));
		fields.put("release-dateField", createDateField(getTimestamp()));
		fields.put("categories-nodeListField", createNodeListField(randomUUID(), randomUUID(), randomUUID()));
		fields.put("names-stringListField", createStringListField("Jack", "Joe", "Mary", "Tom"));
		fields.put("categoryIds-numberListField", createNumberListField(1, 42, 133, 7));
		fields.put("binary-binaryField", createBinaryField());
		fields.put("location-micronodeField", createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)),
				Tuple.tuple("longitude", createNumberField(16.373063840833))));
		fields.put("locations-micronodeListField",
				createMicronodeListField(
						createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)),
								Tuple.tuple("longitude", createNumberField(16.373063840833))),
						createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.137222)),
								Tuple.tuple("longitude", createNumberField(11.575556)))));

		nodeResponse.setSchema(getSchemaReference("content"));
		nodeResponse.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		return nodeResponse;
	}

	public static Field createBinaryField() {
		BinaryField binaryField = new BinaryFieldImpl();
		binaryField.setFileName("flower.jpg");
		binaryField.setDominantColor("#22a7f0");
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

		NavigationElement root = new NavigationElement();
		String rootUuid = randomUUID();

		// Level 0
		NodeResponse rootElement = getNodeResponseWithAllFields();
		rootElement.setUuid(rootUuid);
		root.setUuid(rootUuid);
		root.setNode(rootElement);
		root.setChildren(new ArrayList<>());

		// Level 1
		NavigationElement navElement = new NavigationElement();
		String navElementUuid = randomUUID();
		NodeResponse navElementNode = getNodeResponseWithAllFields();
		navElementNode.setUuid(navElementUuid);
		navElement.setUuid(navElementUuid);
		navElement.setNode(navElementNode);
		root.getChildren().add(navElement);

		response.setRoot(root);
		return response;
	}

	public NodeResponse getNodeResponse2() {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(randomUUID());
		nodeResponse.setSchema(getSchemaReference("content"));

		NodeReferenceImpl parentNodeReference = new NodeReferenceImpl();
		parentNodeReference.setUuid(randomUUID());
		parentNodeReference.setDisplayName("parentNodeDisplayName");

		nodeResponse.setParentNode(parentNodeReference);
		nodeResponse.setCreator(getUserReference());
		nodeResponse.setCreated(getTimestamp());
		nodeResponse.setEdited(getTimestamp());
		nodeResponse.setEditor(getUserReference());

		FieldMap fields = nodeResponse.getFields();
		fields.put("name", createStringField("Name for language tag en"));
		fields.put("filename", createStringField("dummy-content.en.html"));
		fields.put("teaser", createStringField("Dummy teaser for en"));
		fields.put("content", createStringField("Content for language tag en"));

		nodeResponse.setPermissions("READ", "CREATE");
		return nodeResponse;
	}

	public NodeCreateRequest getNodeCreateRequest2() {
		NodeCreateRequest contentCreate = new NodeCreateRequest();
		contentCreate.setParentNodeUuid(randomUUID());
		contentCreate.setLanguage("en");
		contentCreate.setSchema(getSchemaReference("content"));

		FieldMap fields = contentCreate.getFields();
		fields.put("name", createStringField("English name"));
		fields.put("filename", createStringField("index.en.html"));
		fields.put("content", createStringField("English content"));
		fields.put("title", createStringField("English title"));
		fields.put("teaser", createStringField("English teaser"));
		fields.put("relatedProduct-nodeField", createNodeField(randomUUID()));
		fields.put("price-numberField", createNumberField(100.1));
		fields.put("enabled-booleanField", createBooleanField(true));
		fields.put("release-dateField", createDateField(getTimestamp()));
		fields.put("categories-nodeListField", createNodeListField(randomUUID(), randomUUID(), randomUUID()));
		fields.put("names-stringListField", createStringListField("Jack", "Joe", "Mary", "Tom"));
		fields.put("categoryIds-numberListField", createNumberListField(1, 42, 133, 7));
		fields.put("location-micronodeField", createNewMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)),
				Tuple.tuple("longitude", createNumberField(16.373063840833))));
		fields.put("locations-micronodeListField",
				createMicronodeListField(
						createNewMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)),
								Tuple.tuple("longitude", createNumberField(16.373063840833))),
						createNewMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.137222)),
								Tuple.tuple("longitude", createNumberField(11.575556)))));

		return contentCreate;
	}

	public NodeCreateRequest getNodeCreateRequest() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(randomUUID());
		nodeCreateRequest.setSchema(new SchemaReference().setName("vehicle"));
		nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString("DeLorean DMC-12"));
		nodeCreateRequest.getFields().put("description", new HtmlFieldImpl().setHTML(
				"The DeLorean DMC-12 is a sports car manufactured by John DeLorean's DeLorean Motor Company for the American market from 1981–83."));
		return nodeCreateRequest;
	}

	public NodeUpdateRequest getNodeUpdateRequest2() {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setSchema(new SchemaReference().setName("vehicle"));
		nodeUpdateRequest.getFields().put("weight", new NumberFieldImpl().setNumber(1230));
		return nodeUpdateRequest;
	}

	public BinaryFieldTransformRequest getBinaryFieldTransformRequest() {
		BinaryFieldTransformRequest request = new BinaryFieldTransformRequest();
		request.setHeight(200);
		request.setWidth(100);
		request.setCropx(50);
		request.setCropy(20);
		request.setCropw(170);
		request.setCroph(150);
		return request;
	}

	public NodeUpdateRequest getNodeUpdateRequest() {
		NodeUpdateRequest nodeUpdate = new NodeUpdateRequest();
		nodeUpdate.setLanguage("en");
		nodeUpdate.setSchema(getSchemaReference("content"));

		FieldMap fields = nodeUpdate.getFields();
		fields.put("filename", createStringField("index-renamed.en.html"));
		fields.put("relatedProduct-nodeField", createNodeField(randomUUID()));
		fields.put("price-numberField", createNumberField(100.1));
		fields.put("enabled-booleanField", createBooleanField(true));
		fields.put("release-dateField", createDateField(getTimestamp()));
		fields.put("categories-nodeListField", createNodeListField(randomUUID(), randomUUID(), randomUUID()));
		fields.put("names-stringListField", createStringListField("Jack", "Joe", "Mary", "Tom"));
		fields.put("categoryIds-numberListField", createNumberListField(1, 42, 133, 7));
		fields.put("location-micronodeField", createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)),
				Tuple.tuple("longitude", createNumberField(16.373063840833))));
		fields.put("locations-micronodeListField",
				createMicronodeListField(
						createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.208330230278)),
								Tuple.tuple("longitude", createNumberField(16.373063840833))),
						createMicronodeField("geolocation", Tuple.tuple("latitude", createNumberField(48.137222)),
								Tuple.tuple("longitude", createNumberField(11.575556)))));

		return nodeUpdate;
	}

	public NodeListResponse getNodeListResponse() {
		NodeListResponse list = new NodeListResponse();
		list.getData().add(getNodeResponseWithAllFields());
		list.getData().add(getNodeResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

}
