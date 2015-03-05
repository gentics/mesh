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
import com.gentics.cailun.core.rest.response.RestUser;
import com.gentics.cailun.core.rest.response.RestUserList;

public class Generator {

	private static final RandomBasedGenerator uuidGenerator = Generators.randomBasedGenerator();
	private ObjectMapper mapper = new ObjectMapper();
	private File baseDir;

	public static String getUUID() {

		final UUID uuid = uuidGenerator.generate();
		final StringBuilder sb = new StringBuilder();
		sb.append(Long.toHexString(uuid.getMostSignificantBits())).append(Long.toHexString(uuid.getLeastSignificantBits()));
		return sb.toString();
	}

	private void writeJsonSchema(Class<?> clazz) throws IOException {
		File file = new File(baseDir, clazz.getSimpleName() + ".schema.json");
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
		baseDir = new File("target", "output");
		baseDir.mkdirs();
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

		RestUser user = new RestUser();
		user.setUuid(getUUID());
		user.setUsername("jdoe42");
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEmailAddress("j.doe@nowhere.com");
		user.addGroup("admins");
		user.addGroup("editors");
		write(user);

		RestUserList userList = new RestUserList();
		userList.addUser(user);
		write(userList);

		RestGroup group = new RestGroup();
		write(group);

		RestGroupList groupList = new RestGroupList();
		groupList.addGroup(group);
		write(groupList);

		RestRole role = new RestRole();
		role.setName("Admin role");
		role.setUuid(getUUID());
		write(role);

		RestRoleList roleList = new RestRoleList();
		roleList.addRole(role);
		write(roleList);

		RestProject project = new RestProject();
		project.setName("Dummy Project");
		project.setUuid(getUUID());
		write(project);

		RestProjectList projectList = new RestProjectList();
		projectList.addProject(project);
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

		RestObjectSchemaList schemaList = new RestObjectSchemaList();
		schemaList.addSchema(schema);
		write(schemaList);

	}

	private void write(Object object) throws com.fasterxml.jackson.core.JsonGenerationException, JsonMappingException, IOException {
		File file = new File(baseDir, object.getClass().getSimpleName() + ".example.json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
		writeJsonSchema(object.getClass());

	}
}
