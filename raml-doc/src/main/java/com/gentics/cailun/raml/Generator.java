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
import com.gentics.cailun.core.rest.content.response.ContentListResponse;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
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
import com.gentics.cailun.core.rest.schema.response.SchemaReference;
import com.gentics.cailun.core.rest.tag.request.TagCreateRequest;
import com.gentics.cailun.core.rest.tag.request.TagUpdateRequest;
import com.gentics.cailun.core.rest.tag.response.TagListResponse;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserListResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.util.JsonUtils;
import com.gentics.cailun.util.RestModelPagingHelper;

public class Generator {

	private static final RandomBasedGenerator uuidGenerator = Generators.randomBasedGenerator();
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

	public void start() throws IOException {
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
		project.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		write(project);

		ProjectResponse project2 = new ProjectResponse();
		project2.setName("Dummy Project (Mobile)");
		project2.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		project2.setUuid(getUUID());

		ProjectListResponse projectList = new ProjectListResponse();
		projectList.getData().add(project);
		projectList.getData().add(project2);
		RestModelPagingHelper.setPaging(projectList, 1, 10, 2, 20);
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
		role.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		write(role);

		RoleResponse role2 = new RoleResponse();
		role2.setName("Reader role");
		role2.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		role2.setUuid(getUUID());

		RoleListResponse roleList = new RoleListResponse();
		roleList.getData().add(role);
		roleList.getData().add(role2);
		RestModelPagingHelper.setPaging(roleList, 1, 10, 2, 20);
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

		String lang = "en";
		TagResponse tag = new TagResponse();
		tag.setUuid(getUUID());
		tag.addProperty(lang, "name", "Name for language tag de");
		tag.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		tag.setSchema(new SchemaReference("tag", getUUID()));
		write(tag);

		TagUpdateRequest tagUpdate = new TagUpdateRequest();
		tagUpdate.setUuid(getUUID());
		write(tagUpdate);

		TagCreateRequest tagCreate = new TagCreateRequest();
		tagCreate.setSchemaName("content");
		tagCreate.setTagUuid(getUUID());
		write(tagCreate);

		TagResponse tag2 = new TagResponse();
		tag2.setUuid(getUUID());
		tag2.addProperty("en", "name", "Name for language tag en");
		tag2.setSchema(new SchemaReference("tag", getUUID()));
		tag2.setPerms("READ", "CREATE");

		TagListResponse tagList = new TagListResponse();
		tagList.getData().add(tag);
		tagList.getData().add(tag2);
		RestModelPagingHelper.setPaging(tagList, 1, 10, 2, 20);
		write(tagList);
	}

	private void schemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectSchemaResponse schema = new ObjectSchemaResponse();
		schema.setUuid(getUUID());
		schema.setDescription("Description of the schema");
		schema.setName("extended-content");
		schema.setPerms("READ", "UPDATE", "DELETE", "CREATE");
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
		schemaList.getData().add(schema);
		schemaList.getData().add(schema2);
		RestModelPagingHelper.setPaging(schemaList, 1, 10, 2, 20);
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

	private void contentJson() throws JsonGenerationException, JsonMappingException, IOException {
		String lang = "de";
		ContentResponse content = new ContentResponse();
		content.setUuid(getUUID());
		content.setAuthor(getUser());
		content.addProperty(lang, "name", "Name for language tag de-DE");
		content.addProperty(lang, "filename", "dummy-content.de.html");
		content.addProperty(lang, "teaser", "Dummy teaser for de-DE");
		content.addProperty(lang, "content", "Content for language tag de-DE");
		content.setSchema(new SchemaReference("content", getUUID()));
		content.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		write(content);

		ContentUpdateRequest contentUpdate = new ContentUpdateRequest();
		contentUpdate.setUuid(getUUID());
		contentUpdate.addProperty(lang, "filename", "index-renamed.en.html");
		write(contentUpdate);

		lang = "en";
		ContentCreateRequest contentCreate = new ContentCreateRequest();
		contentCreate.addProperty(lang, "filename", "index.en.html");
		contentCreate.addProperty(lang, "content", "English content");
		contentCreate.addProperty(lang, "title", "English title");
		contentCreate.addProperty(lang, "teaser", "English teaser");
		contentCreate.addProperty(lang, "name", "English name");

		lang = "de";
		contentCreate.addProperty(lang, "filename", "index.de.html");
		contentCreate.addProperty(lang, "content", "Deutscher Inhalt");
		contentCreate.addProperty(lang, "title", "Deutscher Titel");
		contentCreate.addProperty(lang, "teaser", "Deutscher Teaser");
		contentCreate.addProperty(lang, "name", "Deutscher Name");

		contentCreate.setSchemaName("content");
		write(contentCreate);

		ContentResponse content2 = new ContentResponse();
		content2.setUuid(getUUID());
		content2.setAuthor(getUser());
		lang = "en";
		content2.addProperty(lang, "name", "Name for language tag en");
		content2.addProperty(lang, "filename", "dummy-content.en.html");
		content2.addProperty(lang, "teaser", "Dummy teaser for en");
		content2.addProperty(lang, "content", "Content for language tag en");
		content2.setSchema(new SchemaReference("content", getUUID()));
		content2.setPerms("READ", "CREATE");

		ContentListResponse list = new ContentListResponse();
		list.getData().add(content);
		list.getData().add(content2);
		RestModelPagingHelper.setPaging(list, 1, 10, 2, 20);
		write(list);

	}

	private void groupJson() throws JsonGenerationException, JsonMappingException, IOException {

		GroupResponse group = new GroupResponse();
		group.setUuid(getUUID());
		group.setName("Admin Group");
		group.setPerms("READ", "UPDATE", "DELETE", "CREATE");

		write(group);

		GroupResponse group2 = new GroupResponse();
		group2.setUuid(getUUID());
		group2.setName("Editor Group");
		group2.setPerms("READ", "UPDATE", "DELETE", "CREATE");

		GroupListResponse groupList = new GroupListResponse();
		groupList.getData().add(group);
		groupList.getData().add(group2);
		RestModelPagingHelper.setPaging(groupList, 1, 10, 2, 20);
		write(groupList);

		GroupUpdateRequest groupUpdate = new GroupUpdateRequest();
		groupUpdate.setUuid(getUUID());
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
		user2.setUsername("jroe");
		user2.setFirstname("Jane");
		user2.setLastname("Roe");
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.addGroup("super-editors");
		user2.addGroup("editors");

		UserListResponse userList = new UserListResponse();
		userList.getData().add(user);
		userList.getData().add(user2);
		RestModelPagingHelper.setPaging(userList, 1, 10, 2, 20);
		write(userList);

		UserUpdateRequest userUpdate = new UserUpdateRequest();
		userUpdate.setUuid(getUUID());
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

	private UserResponse getUser() {
		UserResponse user = new UserResponse();
		user.setUuid(getUUID());
		user.setUsername("jdoe42");
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEmailAddress("j.doe@nowhere.com");
		user.addGroup("editors");
		user.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		return user;
	}

	private void write(Object object) throws JsonGenerationException, JsonMappingException, IOException {
		File file = new File(outputDir, object.getClass().getSimpleName() + ".example.json");
		ObjectMapper mapper = JsonUtils.getMapper();
		mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
		writeJsonSchema(object.getClass());
	}
}
