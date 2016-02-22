package com.gentics.mesh.generator;

import static com.gentics.mesh.util.FieldUtil.createBooleanField;
import static com.gentics.mesh.util.FieldUtil.createDateField;
import static com.gentics.mesh.util.FieldUtil.createHtmlField;
import static com.gentics.mesh.util.FieldUtil.createMicronodeField;
import static com.gentics.mesh.util.FieldUtil.createMicronodeListField;
import static com.gentics.mesh.util.FieldUtil.createNewMicronodeField;
import static com.gentics.mesh.util.FieldUtil.createNodeField;
import static com.gentics.mesh.util.FieldUtil.createNodeListField;
import static com.gentics.mesh.util.FieldUtil.createNumberField;
import static com.gentics.mesh.util.FieldUtil.createNumberListField;
import static com.gentics.mesh.util.FieldUtil.createStringField;
import static com.gentics.mesh.util.FieldUtil.createStringListField;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
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
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Example generator for the RAML Documentation. This generator will use the rest model POJOs and populate them with fake data to generate example json
 * responses.
 */
public class RAMLExampleGenerator extends AbstractGenerator {

	public static void main(String[] args) throws IOException {
		new RAMLExampleGenerator().run();
	}

	public void run() throws IOException {
		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "src" + File.separator + "main" + File.separator + "raml";
		}
		File baseDir = new File(baseDirProp);
		outputDir = new File(baseDir, "json");
		System.out.println("Writing files to  {" + outputDir.getAbsolutePath() + "}");
		outputDir.mkdirs();
		// Raml raml = new Raml();

		createJson();

		// Raml raml = new Raml();
		// Resource r = new Resource();
		// r.setDescription("jow");
		// raml.getResources().put("test", r);
		//
		// raml.setTitle("test1234");
		//
		// // modify the raml object
		//
		// RamlEmitter emitter = new RamlEmitter();
		// String dumpFromRaml = emitter.dump(raml);
		// System.out.println(dumpFromRaml);

	}

	// private File baseDir = new File("target", "raml2html");
	//
	// @Before
	// public void setup() throws IOException {
	// FileUtils.deleteDirectory(baseDir);
	// }

	// @Test
	// public void testGenerator() throws IOException {
	// System.setProperty("baseDir", baseDir.getAbsolutePath());
	// File jsonDir = new File(baseDir, "json");
	// assertTrue(jsonDir.exists());
	// assertTrue(jsonDir.listFiles().length != 0);
	// }

	private void createJson() throws IOException {

		userJson();
		groupJson();
		roleJson();
		nodeJson();
		tagJson();
		tagFamilyJson();
		schemaJson();
		microschemaJson();
		projectJson();
		searchStatusJson();

		genericResponseJson();
		loginRequest();

		transformRequest();

		demoExamples();
	}

	private void demoExamples() throws JsonGenerationException, JsonMappingException, IOException {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(randomUUID());
		nodeCreateRequest.setSchema(new SchemaReference().setName("vehicle"));
		nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString("DeLorean DMC-12"));
		nodeCreateRequest.getFields().put("description", new HtmlFieldImpl().setHTML(
				"The DeLorean DMC-12 is a sports car manufactured by John DeLorean's DeLorean Motor Company for the American market from 1981–83."));
		write(nodeCreateRequest, "demo.NodeCreateRequest.json");

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setSchema(new SchemaReference().setName("vehicle"));
		nodeUpdateRequest.getFields().put("weight", new NumberFieldImpl().setNumber(1230));
		write(nodeUpdateRequest, "demo.NodeUpdateRequest.json");
	}

	private void searchStatusJson() throws JsonGenerationException, JsonMappingException, IOException {
		SearchStatusResponse status = new SearchStatusResponse();
		status.setBatchCount(42);
		write(status);
	}

	private void genericResponseJson() throws JsonGenerationException, JsonMappingException, IOException {
		GenericMessageResponse message = new GenericMessageResponse();
		message.setMessage("I18n message");
		write(message);
	}

	private void projectJson() throws JsonGenerationException, JsonMappingException, IOException {
		ProjectResponse project = new ProjectResponse();
		project.setUuid(randomUUID());
		project.setName("Dummy Project");
		project.setCreated(getTimestamp());
		project.setCreator(getUserReference());
		project.setEdited(getTimestamp());
		project.setEditor(getUserReference());
		project.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		project.setRootNodeUuid(randomUUID());
		write(project);

		ProjectResponse project2 = new ProjectResponse();
		project2.setUuid(randomUUID());
		project2.setName("Dummy Project (Mobile)");
		project2.setCreated(getTimestamp());
		project2.setCreator(getUserReference());
		project2.setEdited(getTimestamp());
		project2.setEditor(getUserReference());
		project2.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		project2.setRootNodeUuid(randomUUID());

		ListResponse<ProjectResponse> projectList = new ListResponse<>();
		projectList.getData().add(project);
		projectList.getData().add(project2);
		setPaging(projectList, 1, 10, 2, 20);
		write(projectList);

		ProjectUpdateRequest projectUpdate = new ProjectUpdateRequest();
		projectUpdate.setName("Renamed project");
		write(projectUpdate);

		ProjectCreateRequest projectCreate = new ProjectCreateRequest();
		projectCreate.setName("New project");
		write(projectCreate);

	}

	private void roleJson() throws JsonGenerationException, JsonMappingException, IOException {
		RoleResponse role = new RoleResponse();
		role.setName("Admin role");
		role.setUuid(randomUUID());
		role.setCreated(getTimestamp());
		role.setCreator(getUserReference());
		role.setEdited(getTimestamp());
		role.setEditor(getUserReference());
		role.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		List<GroupReference> groups = new ArrayList<>();
		groups.add(new GroupReference().setName("editors").setUuid(randomUUID()));
		groups.add(new GroupReference().setName("guests").setUuid(randomUUID()));
		role.setGroups(groups);
		write(role);

		RoleResponse role2 = new RoleResponse();
		role2.setName("Reader role");
		role2.setCreated(getTimestamp());
		role2.setCreator(getUserReference());
		role2.setEdited(getTimestamp());
		role2.setEditor(getUserReference());
		role2.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		role2.setUuid(randomUUID());

		ListResponse<RoleResponse> roleList = new ListResponse<>();
		roleList.getData().add(role);
		roleList.getData().add(role2);
		setPaging(roleList, 1, 10, 2, 20);
		write(roleList);

		RoleUpdateRequest roleUpdate = new RoleUpdateRequest();
		roleUpdate.setName("New name");
		write(roleUpdate);

		RoleCreateRequest roleCreate = new RoleCreateRequest();
		roleCreate.setName("super editors");
		write(roleCreate);

		RolePermissionRequest rolePermission = new RolePermissionRequest();
		rolePermission.setRecursive(false);
		rolePermission.getPermissions().add("create");
		rolePermission.getPermissions().add("read");
		rolePermission.getPermissions().add("update");
		rolePermission.getPermissions().add("delete");
		write(rolePermission);

		RolePermissionResponse rolePermissionResponse = new RolePermissionResponse();
		rolePermissionResponse.getPermissions().add("create");
		rolePermissionResponse.getPermissions().add("read");
		rolePermissionResponse.getPermissions().add("update");
		rolePermissionResponse.getPermissions().add("delete");
		write(rolePermissionResponse);
	}

	private void tagJson() throws JsonGenerationException, JsonMappingException, IOException {

		TagFamilyReference tagFamilyReference = new TagFamilyReference();
		tagFamilyReference.setName("colors");
		tagFamilyReference.setUuid(randomUUID());

		TagResponse tag = new TagResponse();
		tag.setUuid(randomUUID());
		tag.setCreated(getTimestamp());
		tag.setCreator(getUserReference());
		tag.setEdited(getTimestamp());
		tag.setEditor(getUserReference());
		tag.getFields().setName("tagName");
		tag.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		tag.setTagFamily(tagFamilyReference);
		write(tag);

		TagUpdateRequest tagUpdate = new TagUpdateRequest();
		write(tagUpdate);

		TagCreateRequest tagCreate = new TagCreateRequest();
		//tagCreate.setTagFamily(tagFamilyReference);
		write(tagCreate);

		TagResponse tag2 = new TagResponse();
		tag2.setUuid(randomUUID());
		tag2.setCreated(getTimestamp());
		tag2.setCreator(getUserReference());
		tag2.setEdited(getTimestamp());
		tag2.setEditor(getUserReference());
		tag2.getFields().setName("Name for language tag en");
		tag2.setTagFamily(tagFamilyReference);
		tag2.setPermissions("READ", "CREATE");

		ListResponse<TagResponse> tagList = new ListResponse<>();
		tagList.getData().add(tag);
		tagList.getData().add(tag2);
		setPaging(tagList, 1, 10, 2, 20);
		write(tagList);
	}

	private void tagFamilyJson() throws JsonGenerationException, JsonMappingException, IOException {
		TagFamilyResponse response = new TagFamilyResponse();
		response.setPermissions("READ", "CREATE", "UPDATE");
		response.setName("Colors");
		response.setEdited(getTimestamp());
		response.setEditor(getUserReference());
		response.setCreated(getTimestamp());
		response.setCreator(getUserReference());
		write(response);

		TagFamilyListResponse tagFamilyListResponse = new TagFamilyListResponse();
		tagFamilyListResponse.getData().add(response);
		setPaging(tagFamilyListResponse, 1, 10, 2, 20);
		write(tagFamilyListResponse);

		TagFamilyUpdateRequest updateRequest = new TagFamilyUpdateRequest();
		updateRequest.setName("Nicer colors");
		write(updateRequest);

		TagFamilyCreateRequest createRequest = new TagFamilyCreateRequest();
		createRequest.setName("Colors");
		write(createRequest);
	}

	private long getTimestamp() {
		return new Date().getTime();
	}

	public void setPaging(ListResponse<?> response, long currentPage, long pageCount, long perPage, long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);
	}

	private void microschemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		write(getMicroschema());
		write(getMicroschemaCreateRequest());
		write(getMicroschema());
		write(getMicroschemaListResponse());
	}

	private void schemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		write(getSchema());
		write(getSchemaCreateRequest());
		write(getSchemaUpdateRequest());
		write(getSchemaListResponse());
		write(getSchemaChangesListModel());
	}

	private void loginRequest() throws JsonGenerationException, JsonMappingException, IOException {
		LoginRequest request = new LoginRequest();
		request.setUsername("admin");
		request.setPassword("finger");
		write(request);
	}

	private Microschema getMicroschema() {
		Microschema microschema = new MicroschemaModel();
		microschema.setName("geolocation");
		microschema.setDescription("Microschema for Geolocations");
		microschema.setUuid(UUIDUtil.randomUUID());

		NumberFieldSchema longitudeFieldSchema = new NumberFieldSchemaImpl();
		longitudeFieldSchema.setName("longitude");
		longitudeFieldSchema.setLabel("Longitude");
		longitudeFieldSchema.setRequired(true);
		//		longitudeFieldSchema.setMin(-180);
		//		longitudeFieldSchema.setMax(180);
		microschema.addField(longitudeFieldSchema);

		NumberFieldSchema latitudeFieldSchema = new NumberFieldSchemaImpl();
		latitudeFieldSchema.setName("latitude");
		latitudeFieldSchema.setLabel("Latitude");
		latitudeFieldSchema.setRequired(true);
		//		latitudeFieldSchema.setMin(-90);
		//		latitudeFieldSchema.setMax(90);
		microschema.addField(latitudeFieldSchema);

		microschema.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		microschema.validate();
		return microschema;
	}

	private MicroschemaListResponse getMicroschemaListResponse() {
		MicroschemaListResponse microschemaList = new MicroschemaListResponse();
		microschemaList.getData().add(getMicroschema());
		microschemaList.getData().add(getMicroschema());
		setPaging(microschemaList, 1, 10, 2, 20);
		return microschemaList;
	}

	private Microschema getMicroschemaCreateRequest() {
		Microschema createRequest = new MicroschemaModel();
		createRequest.setName("geolocation");
		createRequest.setDescription("Microschema for Geolocations");
		NumberFieldSchema longitudeFieldSchema = new NumberFieldSchemaImpl();
		longitudeFieldSchema.setName("longitude");
		longitudeFieldSchema.setLabel("Longitude");
		longitudeFieldSchema.setRequired(true);
		//		longitudeFieldSchema.setMin(-180);
		//		longitudeFieldSchema.setMax(180);
		createRequest.addField(longitudeFieldSchema);

		NumberFieldSchema latitudeFieldSchema = new NumberFieldSchemaImpl();
		latitudeFieldSchema.setName("latitude");
		latitudeFieldSchema.setLabel("Latitude");
		latitudeFieldSchema.setRequired(true);
		//		latitudeFieldSchema.setMin(-90);
		//		latitudeFieldSchema.setMax(90);
		createRequest.addField(latitudeFieldSchema);

		return createRequest;
	}

	private SchemaListResponse getSchemaListResponse() throws JsonGenerationException, JsonMappingException, IOException {
		SchemaListResponse schemaList = new SchemaListResponse();
		schemaList.getData().add(getSchema());
		schemaList.getData().add(getSchema());
		setPaging(schemaList, 1, 10, 2, 20);
		return schemaList;
	}

	private SchemaChangesListModel getSchemaChangesListModel() {
		SchemaChangesListModel model = new SchemaChangesListModel();
		// Add field
		SchemaChangeModel addFieldChange = SchemaChangeModel.createAddFieldChange("listFieldToBeAddedField", "list");
		addFieldChange.setProperty(SchemaChangeModel.LIST_TYPE_KEY, "html");
		model.getChanges().add(addFieldChange);

		// Change field type
		model.getChanges().add(SchemaChangeModel.createChangeFieldTypeChange("fieldToBeUpdated", "string"));

		// Remove field
		model.getChanges().add(SchemaChangeModel.createRemoveFieldChange("fieldToBeRemoved"));

		// Update field
		SchemaChangeModel updateFieldChange = SchemaChangeModel.createUpdateFieldChange("fieldToBeUpdated");
		updateFieldChange.setProperty(SchemaChangeModel.LABEL_KEY, "newLabel");
		model.getChanges().add(updateFieldChange);

		// Update schema
		SchemaChangeModel updateSchemaChange = SchemaChangeModel.createUpdateSchemaChange();
		updateFieldChange.setProperty(SchemaChangeModel.DISPLAY_FIELD_NAME_KEY, "newDisplayField");
		model.getChanges().add(updateSchemaChange);

		return model;
	}

	private Schema getSchemaUpdateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		Schema schemaUpdate = new SchemaModel();
		// TODO should i allow changing the name?
		schemaUpdate.setName("extended-content");
		schemaUpdate.setDescription("New description");
		return schemaUpdate;
	}

	private Schema getSchemaCreateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		Schema schemaUpdateRequest = new SchemaModel();
		schemaUpdateRequest.setContainer(true);
		schemaUpdateRequest.setDescription("Some description text");
		schemaUpdateRequest.setDisplayField("name");
		schemaUpdateRequest.setSegmentField("name");
		schemaUpdateRequest.setName("video-schema");
		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		schemaUpdateRequest.addField(nameFieldSchema);
		return schemaUpdateRequest;
	}

	private Schema getSchema() throws JsonGenerationException, JsonMappingException, IOException {
		Schema schema = new SchemaModel();
		schema.setUuid(randomUUID());
		schema.setName("Example Schema");
		schema.setSegmentField("name");
		schema.setDisplayField("name");
		// schema.setDescription("Description of the schema");
		// schema.setName("extended-content");
		schema.setPermissions("READ", "UPDATE", "DELETE", "CREATE");

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		schema.addField(nameFieldSchema);

		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName("number");
		numberFieldSchema.setLabel("Number");
		//		numberFieldSchema.setMin(2);
		//		numberFieldSchema.setMax(10);
		//		numberFieldSchema.setStep(0.5F);
		schema.addField(numberFieldSchema);

		HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName("html");
		htmlFieldSchema.setLabel("Teaser");
		schema.addField(htmlFieldSchema);

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setAllowedSchemas(new String[] { "content", "video" });
		listFieldSchema.setMin(1);
		listFieldSchema.setMax(10);
		listFieldSchema.setLabel("List of nodes");
		listFieldSchema.setName("Nodes");
		listFieldSchema.setListType("node");
		listFieldSchema.setName("list");
		schema.addField(listFieldSchema);

		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setAllowedSchemas(new String[] { "content", "video", "image" });
		nodeFieldSchema.setName("node");
		schema.addField(nodeFieldSchema);

		MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
		micronodeFieldSchema.setName("location");
		micronodeFieldSchema.setLabel("Location");
		micronodeFieldSchema.setAllowedMicroSchemas(new String[] { "geolocation" });
		schema.addField(micronodeFieldSchema);

		ListFieldSchemaImpl micronodeListFieldSchema = new ListFieldSchemaImpl();
		micronodeListFieldSchema.setName("locationlist");
		micronodeListFieldSchema.setLabel("List of Locations");
		micronodeListFieldSchema.setListType("micronode");
		micronodeListFieldSchema.setAllowedSchemas(new String[] { "geolocation" });
		schema.addField(micronodeListFieldSchema);
		schema.validate();
		return schema;
	}

	private NavigationResponse getNavigationResponse() throws JsonGenerationException, JsonMappingException, IOException {
		NavigationResponse response = new NavigationResponse();

		NavigationElement root = new NavigationElement();
		String rootUuid = randomUUID();

		// Level 0
		NodeResponse rootElement = getNodeResponse1();
		rootElement.setUuid(rootUuid);
		root.setUuid(rootUuid);
		root.setNode(rootElement);
		root.setChildren(new ArrayList<>());

		// Level 1
		NavigationElement navElement = new NavigationElement();
		String navElementUuid = randomUUID();
		NodeResponse navElementNode = getNodeResponse1();
		navElementNode.setUuid(navElementUuid);
		navElement.setUuid(navElementUuid);
		navElement.setNode(navElementNode);
		root.getChildren().add(navElement);

		response.setRoot(root);
		return response;
	}

	private NodeResponse getNodeResponse1() throws JsonGenerationException, JsonMappingException, IOException {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(randomUUID());
		NodeReferenceImpl parentNodeReference = new NodeReferenceImpl();
		parentNodeReference.setUuid(randomUUID());
		parentNodeReference.setDisplayName("parentNodeDisplayName");
		nodeResponse.setParentNode(parentNodeReference);
		nodeResponse.setCreated(getTimestamp());
		nodeResponse.setEdited(getTimestamp());
		nodeResponse.setCreator(getUserReference());
		nodeResponse.setPublished(true);
		nodeResponse.setPath("/api/v1/yourProject/webroot/Images");
		nodeResponse.setAvailableLanguages(Arrays.asList("en", "de"));
		HashMap<String, String> languagePaths = new HashMap<>();
		languagePaths.put("en", "/api/v1/yourProject/webroot/Images");
		languagePaths.put("de", "/api/v1/yourProject/webroot/Bilder");
		nodeResponse.setLanguagePaths(languagePaths);
		nodeResponse.getChildrenInfo().put("blogpost", new NodeChildrenInfo().setCount(1).setSchemaUuid(randomUUID()));
		nodeResponse.getChildrenInfo().put("folder", new NodeChildrenInfo().setCount(5).setSchemaUuid(randomUUID()));

		Map<String, Field> fields = nodeResponse.getFields();
		fields.put("name-stringField", createStringField("Name for language tag de-DE"));
		fields.put("filename-stringField", createStringField("dummy-content.de.html"));
		fields.put("teaser-stringField", createStringField("Dummy teaser for de-DE"));
		fields.put("content-htmlField", createHtmlField("Content for language tag de-DE"));
		fields.put("relatedProduct-nodeField", createNodeField(randomUUID()));
		fields.put("price-numberField", createNumberField(100.1));
		fields.put("enabled-booleanField", createBooleanField(true));
		fields.put("release-dateField", createDateField(System.currentTimeMillis() / 1000));
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

	private Field createBinaryField() {
		BinaryField binaryField = new BinaryFieldImpl();
		binaryField.setFileName("flower.jpg");
		binaryField.setDpi(200);
		binaryField.setFileSize(95365);
		binaryField.setWidth(800);
		binaryField.setHeight(600);
		binaryField.setMimeType("image/jpeg");
		binaryField.setSha512sum(
				"ec582eb760034dd91d5fd33656c0b56f082b7365d32e2a139dd9c87ebc192bff3525f32ff4c4137463a31cad020ac19e6e356508db2b90e32d737b6d725e14c1");
		return binaryField;
	}

	private NodeResponse getNodeResponse2() throws JsonGenerationException, JsonMappingException, IOException {
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

		Map<String, Field> fields = nodeResponse.getFields();
		fields.put("name", createStringField("Name for language tag en"));
		fields.put("filename", createStringField("dummy-content.en.html"));
		fields.put("teaser", createStringField("Dummy teaser for en"));
		fields.put("content", createStringField("Content for language tag en"));

		nodeResponse.setPermissions("READ", "CREATE");
		return nodeResponse;
	}

	private NodeCreateRequest getNodeCreateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		NodeCreateRequest contentCreate = new NodeCreateRequest();
		contentCreate.setParentNodeUuid(randomUUID());
		contentCreate.setLanguage("en");
		contentCreate.setPublished(true);
		contentCreate.setSchema(getSchemaReference("content"));

		Map<String, Field> fields = contentCreate.getFields();
		fields.put("name", createStringField("English name"));
		fields.put("filename", createStringField("index.en.html"));
		fields.put("content", createStringField("English content"));
		fields.put("title", createStringField("English title"));
		fields.put("teaser", createStringField("English teaser"));
		fields.put("relatedProduct-nodeField", createNodeField(randomUUID()));
		fields.put("price-numberField", createNumberField(100.1));
		fields.put("enabled-booleanField", createBooleanField(true));
		fields.put("release-dateField", createDateField(System.currentTimeMillis() / 1000));
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

	private NodeUpdateRequest getNodeUpdateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		NodeUpdateRequest nodeUpdate = new NodeUpdateRequest();
		nodeUpdate.setLanguage("en");
		nodeUpdate.setPublished(true);
		nodeUpdate.setSchema(getSchemaReference("content"));

		Map<String, Field> fields = nodeUpdate.getFields();
		fields.put("filename", createStringField("index-renamed.en.html"));
		fields.put("relatedProduct-nodeField", createNodeField(randomUUID()));
		fields.put("price-numberField", createNumberField(100.1));
		fields.put("enabled-booleanField", createBooleanField(true));
		fields.put("release-dateField", createDateField(System.currentTimeMillis() / 1000));
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

	private NodeListResponse getNodeListResponse() throws JsonGenerationException, JsonMappingException, IOException {
		NodeListResponse list = new NodeListResponse();
		list.getData().add(getNodeResponse1());
		list.getData().add(getNodeResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

	private SchemaReference getSchemaReference(String name) {
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName(name);
		schemaReference.setUuid(randomUUID());
		return schemaReference;
	}

	private BinaryFieldTransformRequest getBinaryFieldTransformRequest() {
		BinaryFieldTransformRequest request = new BinaryFieldTransformRequest();
		request.setHeight(200);
		request.setWidth(100);
		request.setCropx(50);
		request.setCropy(20);
		request.setCropw(170);
		request.setCroph(150);
		return request;
	}

	private void transformRequest() throws JsonGenerationException, JsonMappingException, IOException {
		write(getBinaryFieldTransformRequest());
	}

	private void nodeJson() throws JsonGenerationException, JsonMappingException, IOException {
		write(getNodeResponse1());
		write(getNodeCreateRequest());
		write(getNodeListResponse());
		write(getNodeUpdateRequest());
		write(getNavigationResponse());
	}

	private void groupJson() throws JsonGenerationException, JsonMappingException, IOException {
		GroupResponse group = new GroupResponse();
		group.setUuid(randomUUID());
		group.setCreated(getTimestamp());
		group.setCreator(getUserReference());
		group.setEdited(getTimestamp());
		group.setEditor(getUserReference());
		group.setName("Admin Group");
		group.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		group.getRoles().add(new RoleReference().setName("admin").setUuid(randomUUID()));
		write(group);

		GroupResponse group2 = new GroupResponse();
		group2.setUuid(randomUUID());
		group2.setName("Editor Group");
		group2.setPermissions("READ", "UPDATE", "DELETE", "CREATE");

		GroupListResponse groupList = new GroupListResponse();
		groupList.getData().add(group);
		groupList.getData().add(group2);
		setPaging(groupList, 1, 10, 2, 20);
		write(groupList);

		GroupUpdateRequest groupUpdate = new GroupUpdateRequest();
		write(groupUpdate);

		GroupCreateRequest groupCreate = new GroupCreateRequest();
		groupCreate.setName("new group");
		write(groupCreate);
	}

	private void userJson() throws JsonGenerationException, JsonMappingException, IOException {
		UserResponse user = getUser();
		write(user);

		UserResponse user2 = getUser();
		user2.setUsername("jroe");
		user2.setFirstname("Jane");
		user2.setLastname("Roe");
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.getGroups().add(new GroupReference().setName("super-editors").setUuid(randomUUID()));
		user2.getGroups().add(new GroupReference().setName("editors").setUuid(randomUUID()));
		user2.setEnabled(true);

		ListResponse<UserResponse> userList = new ListResponse<>();
		userList.getData().add(user);
		userList.getData().add(user2);
		setPaging(userList, 1, 10, 2, 20);
		write(userList);

		UserUpdateRequest userUpdate = new UserUpdateRequest();
		userUpdate.setUsername("jdoe42");
		userUpdate.setPassword("iesiech0eewinioghaRa");
		userUpdate.setFirstname("Joe");
		userUpdate.setLastname("Doe");
		userUpdate.setEmailAddress("j.doe@nowhere.com");

		userUpdate.setNodeReference(user2.getNodeReference());
		write(userUpdate);

		UserPermissionResponse userPermResponse = new UserPermissionResponse();
		userPermResponse.getPermissions().add("create");
		userPermResponse.getPermissions().add("read");
		userPermResponse.getPermissions().add("update");
		userPermResponse.getPermissions().add("delete");
		write(userPermResponse);

		UserCreateRequest userCreate = new UserCreateRequest();
		userCreate.setUsername("jdoe42");
		userCreate.setPassword("iesiech0eewinioghaRa");
		userCreate.setFirstname("Joe");
		userCreate.setLastname("Doe");
		userCreate.setEmailAddress("j.doe@nowhere.com");
		userCreate.setGroupUuid(randomUUID());
		userCreate.setNodeReference(user2.getNodeReference());
		write(userCreate);
	}

	private UserReference getUserReference() {
		UserReference reference = new UserReference();
		reference.setUuid(randomUUID());
		reference.setName("jdoe42");
		return reference;
	}

	private UserResponse getUser() {
		UserResponse user = new UserResponse();
		user.setUuid(randomUUID());
		user.setCreated(getTimestamp());
		user.setCreator(getUserReference());
		user.setEdited(getTimestamp());
		user.setEditor(getUserReference());
		user.setUsername("jdoe42");
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEnabled(true);

		NodeReferenceImpl reference = new NodeReferenceImpl();
		reference.setProjectName("dummy");
		reference.setUuid(randomUUID());
		user.setNodeReference(reference);
		user.setEmailAddress("j.doe@nowhere.com");
		user.getGroups().add(new GroupReference().setName("editors").setUuid(randomUUID()));
		user.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		return user;
	}

	private void write(Object object) throws JsonGenerationException, JsonMappingException, IOException {
		File file = new File(outputDir, object.getClass().getSimpleName() + ".example.json");
		write(object, JsonUtil.getMapper(), file);
	}

	private void write(Object object, String filename) throws JsonGenerationException, JsonMappingException, IOException {
		write(object, JsonUtil.getMapper(), new File(outputDir, filename));
	}

	/**
	 * Write the example to disk.
	 * 
	 * @param object
	 *            Object to be transformed to json
	 * @param mapper
	 *            ObjectMapper to be used
	 * @param file
	 *            Outputfile
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void write(Object object, ObjectMapper mapper, File file) throws JsonGenerationException, JsonMappingException, IOException {
		System.out.println("Writing {" + object.getClass().getSimpleName() + "} to {" + file.getAbsolutePath() + "}");
		mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
	}
}
