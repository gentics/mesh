package com.gentics.cailun.raml;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.gentics.cailun.core.rest.request.RestUserCreateRequest;
import com.gentics.cailun.core.rest.request.RestUserUpdateRequest;
import com.gentics.cailun.core.rest.response.RestBinaryFile;
import com.gentics.cailun.core.rest.response.RestGenericContent;
import com.gentics.cailun.core.rest.response.RestGroup;
import com.gentics.cailun.core.rest.response.RestGroupList;
import com.gentics.cailun.core.rest.response.RestObjectSchema;
import com.gentics.cailun.core.rest.response.RestObjectSchemaList;
import com.gentics.cailun.core.rest.response.RestProject;
import com.gentics.cailun.core.rest.response.RestProjectList;
import com.gentics.cailun.core.rest.response.RestRole;
import com.gentics.cailun.core.rest.response.RestRoleList;
import com.gentics.cailun.core.rest.response.RestTag;
import com.gentics.cailun.core.rest.response.RestUserList;
import com.gentics.cailun.core.rest.response.RestUserResponse;
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

		RestUserResponse user = new RestUserResponse();
		user.setUuid(getUUID());
		user.setUsername("jdoe42");
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEmailAddress("j.doe@nowhere.com");
		user.addGroup("editors");
		write(user);

		RestUserResponse user2 = new RestUserResponse();
		user2.setUuid(getUUID());
		user2.setUsername("jroe");
		user2.setFirstname("Jane");
		user2.setLastname("Roe");
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.addGroup("super-editors");
		user2.addGroup("editors");

		RestUserUpdateRequest userUpdate = new RestUserUpdateRequest();
		userUpdate.setUsername("jdoe42");
		userUpdate.setPassword("iesiech0eewinioghaRa");
		userUpdate.setFirstname("Joe");
		userUpdate.setLastname("Doe");
		userUpdate.setEmailAddress("j.doe@nowhere.com");
		userUpdate.addGroup("admins");
		userUpdate.addGroup("editors");
		write(userUpdate);

		RestUserCreateRequest userCreate = new RestUserCreateRequest();
		userCreate.setUsername("jdoe42");
		userCreate.setPassword("iesiech0eewinioghaRa");
		userCreate.setFirstname("Joe");
		userCreate.setLastname("Doe");
		userCreate.setEmailAddress("j.doe@nowhere.com");
		userCreate.addGroup("admins");
		userCreate.addGroup("editors");
		write(userCreate);

		RestUserList userList = new RestUserList();
		userList.addUser(user);
		userList.addUser(user2);
		RestModelPagingHelper.setPaging(userList, "/users", 1, 10, 2, 20);
		write(userList);

		RestGroup group = new RestGroup();
		group.setUuid(getUUID());
		group.setName("Admin Group");
		write(group);

		RestGroup group2 = new RestGroup();
		group2.setUuid(getUUID());
		group2.setName("Editor Group");

		RestGroupList groupList = new RestGroupList();
		groupList.addGroup(group);
		groupList.addGroup(group2);
		RestModelPagingHelper.setPaging(groupList, "/groups", 1, 10, 2, 20);
		write(groupList);

		RestRole role = new RestRole();
		role.setName("Admin role");
		role.setUuid(getUUID());
		write(role);

		RestRole role2 = new RestRole();
		role2.setName("Reader role");
		role2.setUuid(getUUID());

		RestRoleList roleList = new RestRoleList();
		roleList.addRole(role);
		roleList.addRole(role2);
		RestModelPagingHelper.setPaging(roleList, "/roles", 1, 10, 2, 20);
		write(roleList);

		RestProject project = new RestProject();
		project.setName("Dummy Project");
		project.setUuid(getUUID());
		write(project);

		RestProject project2 = new RestProject();
		project2.setName("Dummy Project (Mobile)");
		project2.setUuid(getUUID());

		RestProjectList projectList = new RestProjectList();
		projectList.addProject(project);
		projectList.addProject(project2);
		RestModelPagingHelper.setPaging(projectList, "/projects", 1, 10, 2, 20);
		write(projectList);

		RestGenericContent content = new RestGenericContent();
		content.setUuid(getUUID());
		content.setAuthor(user);
		content.setLanguageTag("de-DE");
		content.addProperty("name", "Name for language tag de-DE");
		content.addProperty("filename", "dummy-content.de.html");
		content.addProperty("teaser", "Dummy teaser for de-DE");
		content.addProperty("content", "Content for language tag de-DE");
		write(content);

		RestTag tag = new RestTag();
		tag.setUuid(getUUID());
		write(tag);

		RestBinaryFile file = new RestBinaryFile();
		file.setUuid(getUUID());
		file.setFilename("some_binary_file.dat");
		write(file);

		RestObjectSchema schema = new RestObjectSchema();
		schema.setUuid(getUUID());
		schema.setDescription("Description of the schema");
		schema.setName("extended-content");
		// TODO properties
		write(schema);

		RestObjectSchema schema2 = new RestObjectSchema();
		schema2.setUuid(getUUID());
		schema2.setDescription("Description of the schema2");
		schema2.setName("extended-content-2");
		// TODO properties

		RestObjectSchemaList schemaList = new RestObjectSchemaList();
		schemaList.addSchema(schema);
		schemaList.addSchema(schema2);
		RestModelPagingHelper.setPaging(projectList, "/projects", 1, 10, 2, 20);
		write(schemaList);

	}

	private void write(Object object) throws com.fasterxml.jackson.core.JsonGenerationException, JsonMappingException, IOException {
		File file = new File(outputDir, object.getClass().getSimpleName() + ".example.json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
		writeJsonSchema(object.getClass());

	}
}
