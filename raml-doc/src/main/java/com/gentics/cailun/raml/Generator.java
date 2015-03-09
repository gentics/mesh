package com.gentics.cailun.raml;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.gentics.cailun.core.data.model.PropertyType;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.content.request.ContentUpdateRequest;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.file.response.RestBinaryFile;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.core.rest.group.response.GroupListResponse;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectListResponse;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.core.rest.role.request.RoleCreateRequest;
import com.gentics.cailun.core.rest.role.request.RoleUpdateRequest;
import com.gentics.cailun.core.rest.role.response.RoleListResponse;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaListResponse;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.cailun.core.rest.tag.request.TagCreateRequest;
import com.gentics.cailun.core.rest.tag.request.TagUpdateRequest;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserListResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.util.RestModelPagingHelper;

public class Generator {

	private static final RandomBasedGenerator uuidGenerator = Generators.randomBasedGenerator();
	private ObjectMapper mapper = new ObjectMapper();
	private File outputDir;

	public static String getUUID() {

		final UUID uuid = uuidGenerator.generate();
		final StringBuilder sb = new StringBuilder();
		sb.append(Long.toHexString(uuid.getMostSignificantBits())).append(Long.toHexString(uuid.getLeastSignificantBits()));
		return sb.toString();
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

	private void start() throws IOException {
		File baseDir = new File("target", "raml2html");
		outputDir = new File(baseDir, "json");
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
		contentJson();
		tagJson();
		fileJson();
		schemaJson();
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
		project.setName("Dummy Project");
		project.setUuid(getUUID());
		write(project);

		ProjectResponse project2 = new ProjectResponse();
		project2.setName("Dummy Project (Mobile)");
		project2.setUuid(getUUID());

		ProjectListResponse projectList = new ProjectListResponse();
		projectList.addProject(project);
		projectList.addProject(project2);
		RestModelPagingHelper.setPaging(projectList, "/projects", 1, 10, 2, 20);
		write(projectList);

		ProjectUpdateRequest projectUpdate = new ProjectUpdateRequest();
		projectUpdate.setUuid(getUUID());
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
		write(role);

		RoleResponse role2 = new RoleResponse();
		role2.setName("Reader role");
		role2.setUuid(getUUID());

		RoleListResponse roleList = new RoleListResponse();
		roleList.addRole(role);
		roleList.addRole(role2);
		RestModelPagingHelper.setPaging(roleList, "/roles", 1, 10, 2, 20);
		write(roleList);

		RoleUpdateRequest roleUpdate = new RoleUpdateRequest();
		roleUpdate.setUuid(getUUID());
		roleUpdate.setName("New name");
		write(roleUpdate);

		RoleCreateRequest roleCreate = new RoleCreateRequest();
		roleCreate.setName("super editors");
		write(roleCreate);
	}

	private void tagJson() throws JsonGenerationException, JsonMappingException, IOException {

		TagResponse tag = new TagResponse();
		tag.setUuid(getUUID());
		write(tag);

		TagUpdateRequest tagUpdate = new TagUpdateRequest();
		tagUpdate.setUuid(getUUID());
		write(tagUpdate);

		TagCreateRequest tagCreate = new TagCreateRequest();
		write(tagCreate);
	}

	private void schemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectSchemaResponse schema = new ObjectSchemaResponse();
		schema.setUuid(getUUID());
		schema.setDescription("Description of the schema");
		schema.setName("extended-content");
		PropertyTypeSchemaResponse prop = new PropertyTypeSchemaResponse();
		prop.setDescription("Html Content");
		prop.setKey("content");
		prop.setType(PropertyType.HTML.name());
		schema.getPropertyTypeSchemas().add(prop);
		write(schema);

		ObjectSchemaResponse schema2 = new ObjectSchemaResponse();
		schema2.setUuid(getUUID());
		schema2.setDescription("Description of the schema2");
		schema2.setName("extended-content-2");

		// TODO properties

		ObjectSchemaListResponse schemaList = new ObjectSchemaListResponse();
		schemaList.addSchema(schema);
		schemaList.addSchema(schema2);
		RestModelPagingHelper.setPaging(schemaList, "/projects", 1, 10, 2, 20);
		write(schemaList);

		ObjectSchemaUpdateRequest schemaUpdate = new ObjectSchemaUpdateRequest();
		schemaUpdate.setUuid(getUUID());
		// TODO should i allow changing the name?
		schemaUpdate.setName("extended-content");
		schemaUpdate.setDescription("New description");
		write(schemaUpdate);

		ObjectSchemaCreateRequest schemaCreate = new ObjectSchemaCreateRequest();
		schemaCreate.setName("extended-content");
		schemaCreate.setDescription("Just a dummy ");
		write(schemaCreate);
	}

	private void fileJson() throws JsonGenerationException, JsonMappingException, IOException {
		RestBinaryFile file = new RestBinaryFile();
		file.setUuid(getUUID());
		file.setFilename("some_binary_file.dat");
		write(file);
	}

	private void contentJson() throws JsonGenerationException, JsonMappingException, IOException {
		ContentResponse content = new ContentResponse();
		content.setUuid(getUUID());
		content.setAuthor(getUser());
		content.setLanguageTag("de-DE");
		content.addProperty("name", "Name for language tag de-DE");
		content.addProperty("filename", "dummy-content.de.html");
		content.addProperty("teaser", "Dummy teaser for de-DE");
		content.addProperty("content", "Content for language tag de-DE");
		write(content);

		ContentUpdateRequest contentUpdate = new ContentUpdateRequest();
		contentUpdate.setUuid(getUUID());
		// TODO handle other parameters
		write(contentUpdate);

		ContentCreateRequest contentCreate = new ContentCreateRequest();
		contentCreate.setAuthor(getUser());
		contentCreate.setType("content");
		contentCreate.setLanguageTag("en-US");
		write(contentCreate);

	}

	private void groupJson() throws JsonGenerationException, JsonMappingException, IOException {

		GroupResponse group = new GroupResponse();
		group.setUuid(getUUID());
		group.setName("Admin Group");
		write(group);

		GroupResponse group2 = new GroupResponse();
		group2.setUuid(getUUID());
		group2.setName("Editor Group");

		GroupListResponse groupList = new GroupListResponse();
		groupList.addGroup(group);
		groupList.addGroup(group2);
		RestModelPagingHelper.setPaging(groupList, "/groups", 1, 10, 2, 20);
		write(groupList);

		GroupUpdateRequest groupUpdate = new GroupUpdateRequest();
		groupUpdate.setUuid(getUUID());
		write(groupUpdate);

		GroupCreateRequest groupCreate = new GroupCreateRequest();
		groupCreate.setName("new group");
		groupCreate.getRoles().add("admin");
		groupCreate.getRoles().add("editors");
		// TODO handle other fields
		write(groupCreate);
	}

	private void userJson() throws JsonGenerationException, JsonMappingException, IOException {
		UserResponse user = getUser();
		write(user);

		UserResponse user2 = new UserResponse();
		user2.setUuid(getUUID());
		user2.setUsername("jroe");
		user2.setFirstname("Jane");
		user2.setLastname("Roe");
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.addGroup("super-editors");
		user2.addGroup("editors");

		UserListResponse userList = new UserListResponse();
		userList.addUser(user);
		userList.addUser(user2);
		RestModelPagingHelper.setPaging(userList, "/users", 1, 10, 2, 20);
		write(userList);

		UserUpdateRequest userUpdate = new UserUpdateRequest();
		userUpdate.setUuid(getUUID());
		userUpdate.setUsername("jdoe42");
		userUpdate.setPassword("iesiech0eewinioghaRa");
		userUpdate.setFirstname("Joe");
		userUpdate.setLastname("Doe");
		userUpdate.setEmailAddress("j.doe@nowhere.com");
		userUpdate.addGroup("admins");
		userUpdate.addGroup("editors");
		write(userUpdate);

		UserCreateRequest userCreate = new UserCreateRequest();
		userCreate.setUsername("jdoe42");
		userCreate.setPassword("iesiech0eewinioghaRa");
		userCreate.setFirstname("Joe");
		userCreate.setLastname("Doe");
		userCreate.setEmailAddress("j.doe@nowhere.com");
		userCreate.addGroup("admins");
		userCreate.addGroup("editors");
		write(userCreate);

	}

	private UserResponse getUser() {
		UserResponse user = new UserResponse();
		user.setUuid(getUUID());
		user.setUsername("jdoe42");
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEmailAddress("j.doe@nowhere.com");
		user.addGroup("editors");
		return user;
	}

	private void write(Object object) throws JsonGenerationException, JsonMappingException, IOException {
		File file = new File(outputDir, object.getClass().getSimpleName() + ".example.json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
		writeJsonSchema(object.getClass());
	}
}
