package com.gentics.mesh.raml;

import static com.gentics.mesh.util.FieldUtil.createStringField;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagFieldContainer;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.UUIDUtil;

public class Generator {

	private File outputDir;

	public static String getUUID() {
		return UUIDUtil.randomUUID();
	}

	private void writeJsonSchema(Class<?> clazz) throws IOException {
		File file = new File(outputDir, clazz.getSimpleName() + ".schema.json");
		ObjectMapper m = new ObjectMapper();
		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
		m.acceptJsonFormatVisitor(m.constructType(clazz), visitor);
		JsonSchema jsonSchema = visitor.finalSchema();
		m.writerWithDefaultPrettyPrinter().writeValue(file, jsonSchema);
	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		new Generator().start();
	}

	public void start() throws IOException {
		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "target" + File.separator + "site" + File.separator + "docs" + File.separator + "raml";
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

		genericResponseJson();
	}

	private void genericResponseJson() throws JsonGenerationException, JsonMappingException, IOException {
		GenericMessageResponse message = new GenericMessageResponse();
		message.setMessage("I18n message");
		write(message);
	}

	private void projectJson() throws JsonGenerationException, JsonMappingException, IOException {
		ProjectResponse project = new ProjectResponse();
		project.setUuid(getUUID());
		project.setName("Dummy Project");
		project.setCreated(getTimestamp());
		project.setCreator(getUserReference());
		project.setEdited(getTimestamp());
		project.setEditor(getUserReference());
		project.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		project.setRootNodeUuid(getUUID());
		write(project);

		ProjectResponse project2 = new ProjectResponse();
		project2.setUuid(getUUID());
		project2.setName("Dummy Project (Mobile)");
		project2.setCreated(getTimestamp());
		project2.setCreator(getUserReference());
		project2.setEdited(getTimestamp());
		project2.setEditor(getUserReference());
		project2.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		project2.setRootNodeUuid(getUUID());

		ProjectListResponse projectList = new ProjectListResponse();
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
		role.setUuid(getUUID());
		role.setCreated(getTimestamp());
		role.setCreator(getUserReference());
		role.setEdited(getTimestamp());
		role.setEditor(getUserReference());
		role.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		write(role);

		RoleResponse role2 = new RoleResponse();
		role2.setName("Reader role");
		role2.setCreated(getTimestamp());
		role2.setCreator(getUserReference());
		role2.setEdited(getTimestamp());
		role2.setEditor(getUserReference());
		role2.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		role2.setUuid(getUUID());

		RoleListResponse roleList = new RoleListResponse();
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
	}

	private void tagJson() throws JsonGenerationException, JsonMappingException, IOException {

		TagFamilyReference tagFamilyReference = new TagFamilyReference();
		tagFamilyReference.setName("colors");
		tagFamilyReference.setUuid(getUUID());

		TagResponse tag = new TagResponse();
		tag.setUuid(getUUID());
		tag.setCreated(getTimestamp());
		tag.setCreator(getUserReference());
		tag.setEdited(getTimestamp());
		tag.setEditor(getUserReference());
		tag.getFields().setName("tagName");
		tag.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		tag.setTagFamilyReference(tagFamilyReference);
		write(tag);

		TagUpdateRequest tagUpdate = new TagUpdateRequest();
		write(tagUpdate);

		TagCreateRequest tagCreate = new TagCreateRequest();
		tagCreate.setTagFamilyReference(tagFamilyReference);
		write(tagCreate);

		TagResponse tag2 = new TagResponse();
		tag2.setUuid(getUUID());
		tag2.setCreated(getTimestamp());
		tag2.setCreator(getUserReference());
		tag2.setEdited(getTimestamp());
		tag2.setEditor(getUserReference());
		tag2.getFields().setName("Name for language tag en");
		tag2.setTagFamilyReference(tagFamilyReference);
		tag2.setPermissions("READ", "CREATE");

		TagListResponse tagList = new TagListResponse();
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

	public void setPaging(AbstractListResponse<?> response, long currentPage, long pageCount, long perPage, long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);
	}

	private void microschemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		MicroschemaResponse response = new MicroschemaResponse();
		response.setUuid(getUUID());
		write(response);
	}

	private void schemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		write(getSchemaResponse());
		write(getSchemaCreateRequest());
		write(getSchemaUpdateRequest());
		write(getSchemaListResponse());
	}

	private SchemaListResponse getSchemaListResponse() throws JsonGenerationException, JsonMappingException, IOException {
		SchemaListResponse schemaList = new SchemaListResponse();
		schemaList.getData().add(getSchemaResponse());
		schemaList.getData().add(getSchemaResponse());
		setPaging(schemaList, 1, 10, 2, 20);
		return schemaList;
	}

	private SchemaUpdateRequest getSchemaUpdateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		SchemaUpdateRequest schemaUpdate = new SchemaUpdateRequest();
		// TODO should i allow changing the name?
		schemaUpdate.setName("extended-content");
		schemaUpdate.setDescription("New description");
		return schemaUpdate;
	}

	private SchemaCreateRequest getSchemaCreateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		SchemaCreateRequest schemaUpdateRequest = new SchemaCreateRequest();
		schemaUpdateRequest.setFolder(true);
		schemaUpdateRequest.setBinary(true);
		schemaUpdateRequest.setDescription("Some description text");
		schemaUpdateRequest.setDisplayField("name");
		schemaUpdateRequest.setName("video-schema");
		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		schemaUpdateRequest.addField(nameFieldSchema);
		return schemaUpdateRequest;
	}

	private SchemaResponse getSchemaResponse() throws JsonGenerationException, JsonMappingException, IOException {
		SchemaResponse schema = new SchemaResponse();
		schema.setUuid(getUUID());
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
		numberFieldSchema.setMin(2);
		numberFieldSchema.setMax(10);
		numberFieldSchema.setStep(0.5F);
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

		MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		microschemaFieldSchema.setName("microschema");
		schema.addField(microschemaFieldSchema);
		return schema;
	}

	private NodeResponse getNodeResponse1() throws JsonGenerationException, JsonMappingException, IOException {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(getUUID());
		nodeResponse.setParentNodeUuid(getUUID());
		nodeResponse.setCreator(getUserReference());
		nodeResponse.getFields().put("name", createStringField("Name for language tag de-DE"));
		nodeResponse.getFields().put("filename", createStringField("dummy-content.de.html"));
		nodeResponse.getFields().put("teaser", createStringField("Dummy teaser for de-DE"));
		nodeResponse.getFields().put("content", createStringField("Content for language tag de-DE"));
		nodeResponse.setSchema(getSchemaReference("content"));
		nodeResponse.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		return nodeResponse;
	}

	private NodeResponse getNodeResponse2() throws JsonGenerationException, JsonMappingException, IOException {
		NodeResponse nodeResponse = new NodeResponse();
		nodeResponse.setUuid(getUUID());
		nodeResponse.setParentNodeUuid(getUUID());
		nodeResponse.setCreator(getUserReference());
		nodeResponse.setCreated(getTimestamp());
		nodeResponse.setEdited(getTimestamp());
		nodeResponse.setEditor(getUserReference());
		nodeResponse.getFields().put("name", createStringField("Name for language tag en"));
		nodeResponse.getFields().put("filename", createStringField("dummy-content.en.html"));
		nodeResponse.getFields().put("teaser", createStringField("Dummy teaser for en"));
		nodeResponse.getFields().put("content", createStringField("Content for language tag en"));
		nodeResponse.setSchema(getSchemaReference("content"));
		nodeResponse.setPermissions("READ", "CREATE");
		return nodeResponse;
	}

	private NodeCreateRequest getNodeCreateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		NodeCreateRequest contentCreate = new NodeCreateRequest();
		contentCreate.setParentNodeUuid(getUUID());
		contentCreate.setLanguage("en");
		Map<String, Field> fields = contentCreate.getFields();
		fields.put("name", createStringField("English name"));
		fields.put("filename", createStringField("index.en.html"));
		fields.put("content", createStringField("English content"));
		fields.put("title", createStringField("English title"));
		fields.put("teaser", createStringField("English teaser"));

		contentCreate.setSchema(getSchemaReference("content"));
		return contentCreate;
	}

	private NodeUpdateRequest getNodeUpdateRequest() throws JsonGenerationException, JsonMappingException, IOException {
		NodeUpdateRequest nodeUpdate = new NodeUpdateRequest();
		nodeUpdate.setLanguage("en");
		nodeUpdate.getFields().put("filename", createStringField("index-renamed.en.html"));
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
		schemaReference.setUuid(getUUID());
		return schemaReference;
	}

	private void nodeJson() throws JsonGenerationException, JsonMappingException, IOException {
		write(getNodeResponse1());
		write(getNodeCreateRequest());
		write(getNodeListResponse());
		write(getNodeUpdateRequest());
	}

	private void groupJson() throws JsonGenerationException, JsonMappingException, IOException {

		GroupResponse group = new GroupResponse();
		group.setUuid(getUUID());
		group.setCreated(getTimestamp());
		group.setCreator(getUserReference());
		group.setEdited(getTimestamp());
		group.setEditor(getUserReference());
		group.setName("Admin Group");
		group.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		write(group);

		GroupResponse group2 = new GroupResponse();
		group2.setUuid(getUUID());
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

		UserResponse user2 = new UserResponse();
		user2.setUuid(getUUID());
		user2.setCreated(getTimestamp());
		user2.setCreator(getUserReference());
		user2.setEdited(getTimestamp());
		user2.setEditor(getUserReference());
		user2.setUsername("jroe");
		user2.setFirstname("Jane");
		user2.setLastname("Roe");
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.addGroup("super-editors");
		user2.addGroup("editors");

		UserListResponse userList = new UserListResponse();
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
		write(userUpdate);

		UserCreateRequest userCreate = new UserCreateRequest();
		userCreate.setUsername("jdoe42");
		userCreate.setPassword("iesiech0eewinioghaRa");
		userCreate.setFirstname("Joe");
		userCreate.setLastname("Doe");
		userCreate.setEmailAddress("j.doe@nowhere.com");
		write(userCreate);

	}

	private UserReference getUserReference() {
		UserReference reference = new UserReference();
		reference.setUuid(getUUID());
		reference.setName("jdoe42");
		return reference;
	}

	private UserResponse getUser() {
		UserResponse user = new UserResponse();
		user.setUuid(getUUID());
		user.setCreated(getTimestamp());
		user.setCreator(getUserReference());
		user.setEdited(getTimestamp());
		user.setEditor(getUserReference());
		user.setUsername("jdoe42");
		user.setFirstname("Joe");
		user.setLastname("Doe");
		NodeReference reference = new NodeReference();
		reference.setProjectName("dummy");
		reference.setUuid(getUUID());
		user.setNodeReference(reference);
		user.setEmailAddress("j.doe@nowhere.com");
		user.addGroup("editors");
		user.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		return user;
	}

	private void write(Object object) throws JsonGenerationException, JsonMappingException, IOException {
		write(object, JsonUtil.getMapper());
	}

	private void write(Object object, ObjectMapper mapper) throws JsonGenerationException, JsonMappingException, IOException {
		File file = new File(outputDir, object.getClass().getSimpleName() + ".example.json");
		System.out.println("Write {" + object.getClass().getSimpleName() + "} to {" + file.getAbsolutePath() + "}");
		mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
	}
}
