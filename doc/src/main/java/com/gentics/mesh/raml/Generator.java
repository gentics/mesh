package com.gentics.mesh.raml;

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
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.rest.common.response.AbstractListResponse;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.response.PagingMetaInfo;
import com.gentics.mesh.core.rest.group.request.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.request.GroupUpdateRequest;
import com.gentics.mesh.core.rest.group.response.GroupListResponse;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.node.request.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.request.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.response.NodeListResponse;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.core.rest.project.request.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.project.response.ProjectListResponse;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.rest.role.request.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.request.RoleUpdateRequest;
import com.gentics.mesh.core.rest.role.response.RoleListResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaListResponse;
import com.gentics.mesh.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.core.rest.schema.response.SchemaResponse;
import com.gentics.mesh.core.rest.tag.request.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.request.TagUpdateRequest;
import com.gentics.mesh.core.rest.tag.response.TagListResponse;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.core.rest.user.request.UserCreateRequest;
import com.gentics.mesh.core.rest.user.request.UserUpdateRequest;
import com.gentics.mesh.core.rest.user.response.UserListResponse;
import com.gentics.mesh.core.rest.user.response.UserResponse;
import com.gentics.mesh.util.JsonUtils;

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
		project.setRootNodeUuid(getUUID());
		write(project);

		ProjectResponse project2 = new ProjectResponse();
		project2.setName("Dummy Project (Mobile)");
		project2.setPerms("READ", "UPDATE", "DELETE", "CREATE");
		project2.setUuid(getUUID());
		project2.setRootNodeUuid(getUUID());

		ProjectListResponse projectList = new ProjectListResponse();
		projectList.getData().add(project);
		projectList.getData().add(project2);
		setPaging(projectList, 1, 10, 2, 20);
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
		setPaging(roleList, 1, 10, 2, 20);
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

		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setSchemaName("tag");
		schemaReference.setUuid(getUUID());

		String lang = "en";
		TagResponse tag = new TagResponse();
		tag.setUuid(getUUID());
		tag.addProperty("name", "Name for language tag de");
		tag.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		tag.setSchema(schemaReference);
		write(tag);

		TagUpdateRequest tagUpdate = new TagUpdateRequest();
		tagUpdate.setUuid(getUUID());
		write(tagUpdate);

		TagCreateRequest tagCreate = new TagCreateRequest();
		tagCreate.setSchema(schemaReference);
		tagCreate.setTagUuid(getUUID());
		write(tagCreate);

		TagResponse tag2 = new TagResponse();
		tag2.setUuid(getUUID());
		tag2.addProperty("name", "Name for language tag en");
		tag2.setSchema(schemaReference);
		tag2.setPermissions("READ", "CREATE");

		TagListResponse tagList = new TagListResponse();
		tagList.getData().add(tag);
		tagList.getData().add(tag2);
		setPaging(tagList, 1, 10, 2, 20);
		write(tagList);
	}

	public void setPaging(AbstractListResponse<? extends AbstractRestModel> response, long currentPage, long pageCount, long perPage, long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);
	}

	private void schemaJson() throws JsonGenerationException, JsonMappingException, IOException {
		SchemaResponse schema = new SchemaResponse();
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

		SchemaResponse schema2 = new SchemaResponse();
		schema2.setUuid(getUUID());
		schema2.setDescription("Description of the schema2");
		schema2.setName("extended-content-2");

		// TODO properties

		ObjectSchemaListResponse schemaList = new ObjectSchemaListResponse();
		schemaList.getData().add(schema);
		schemaList.getData().add(schema2);
		setPaging(schemaList, 1, 10, 2, 20);
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

		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setSchemaName("content");
		schemaReference.setUuid(getUUID());

		String lang = "de";
		NodeResponse content = new NodeResponse();
		content.setUuid(getUUID());
		content.setCreator(getUser());
		content.addProperty("name", "Name for language tag de-DE");
		content.addProperty("filename", "dummy-content.de.html");
		content.addProperty("teaser", "Dummy teaser for de-DE");
		content.addProperty("content", "Content for language tag de-DE");
		content.setSchema(schemaReference);
		content.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		write(content);

		NodeUpdateRequest contentUpdate = new NodeUpdateRequest();
		contentUpdate.setUuid(getUUID());
		contentUpdate.addProperty("filename", "index-renamed.en.html");
		write(contentUpdate);

		lang = "en";
		NodeCreateRequest contentCreate = new NodeCreateRequest();
		contentCreate.setParentNodeUuid(getUUID());
		contentCreate.addProperty("filename", "index.en.html");
		contentCreate.addProperty("content", "English content");
		contentCreate.addProperty("title", "English title");
		contentCreate.addProperty("teaser", "English teaser");
		contentCreate.addProperty("name", "English name");

		contentCreate.setSchema(schemaReference);
		write(contentCreate);

		NodeResponse content2 = new NodeResponse();
		content2.setUuid(getUUID());
		content2.setCreator(getUser());
		lang = "en";
		content2.addProperty("name", "Name for language tag en");
		content2.addProperty("filename", "dummy-content.en.html");
		content2.addProperty("teaser", "Dummy teaser for en");
		content2.addProperty("content", "Content for language tag en");
		content2.setSchema(schemaReference);
		content2.setPermissions("READ", "CREATE");

		NodeListResponse list = new NodeListResponse();
		list.getData().add(content);
		list.getData().add(content2);
		setPaging(list, 1, 10, 2, 20);
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
		setPaging(groupList, 1, 10, 2, 20);
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
		setPaging(userList, 1, 10, 2, 20);
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
