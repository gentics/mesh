package com.gentics.mesh.generator;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ADD_FIELD_AFTER_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DESCRIPTION_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LABEL_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2ServerConfig;
import com.gentics.mesh.util.TokenUtil;

import io.vertx.core.json.JsonObject;

public class ModelExampleGenerator extends AbstractGenerator {

	public ModelExampleGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "models"));
	}

	public void run() throws JsonProcessingException, IOException {
		writeMeshConfig();
		writeChangeExamples();
		writeCustomExamples();
	}

	private void writeCustomExamples() {
		// TODO Auto-generated method stub
	}

	/**
	 * Generate JSON example files for schema changes.
	 * 
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	private void writeChangeExamples() throws JsonProcessingException, IOException {
		SchemaChangeModel addFieldChange = SchemaChangeModel.createAddFieldChange("fieldToBeAdded", "list", "Field Label Value");
		addFieldChange.setProperty(ADD_FIELD_AFTER_KEY, "firstField");
		addFieldChange.setProperty(LIST_TYPE_KEY, "html");
		writeJson(addFieldChange, "addfield.json");

		SchemaChangeModel fieldTypeChange = SchemaChangeModel.createChangeFieldTypeChange("fieldToBeChanged", "html");
		writeJson(fieldTypeChange, "changefieldtype.json");

		SchemaChangeModel removeFieldChange = SchemaChangeModel.createRemoveFieldChange("fieldToBeRemoved");
		writeJson(removeFieldChange, "removefield.json");

		SchemaChangeModel updateFieldChange = new SchemaChangeModel(UPDATEFIELD, "fieldToBeUpdated");
		updateFieldChange.setProperty(NAME_KEY, "newName");
		writeJson(updateFieldChange, "updatefield.json");

		SchemaChangeModel updateMicroschemaChange = SchemaChangeModel.createUpdateMicroschemaChange();
		updateMicroschemaChange.setProperty(DESCRIPTION_KEY, "new description");
		updateMicroschemaChange.setProperty(LABEL_KEY, "new label");
		writeJson(updateMicroschemaChange, "updatemicroschema.json");

		SchemaChangeModel updateSchemaChange = SchemaChangeModel.createUpdateSchemaChange();
		updateSchemaChange.setProperty(SEGMENT_FIELD_KEY, "newSegmentField");
		updateSchemaChange.setProperty(DISPLAY_FIELD_NAME_KEY, "newSegmentField");
		updateSchemaChange.setProperty(DESCRIPTION_KEY, "new description");
		updateSchemaChange.setProperty(LABEL_KEY, "new label");
		updateSchemaChange.setProperty(CONTAINER_FLAG_KEY, "true");
		writeJson(updateSchemaChange, "updateschema.json");

	}

	private void writeMeshConfig() throws JsonProcessingException, IOException {
		MeshOptions conf = new MeshOptions();
		conf.setTempDirectory("/opt/mesh/data/tmp");
		conf.getUploadOptions().setTempDirectory("/opt/mesh/data/tmp/temp-uploads");
		conf.getAuthenticationOptions().setKeystorePassword(TokenUtil.randomToken());
		conf.getAuthenticationOptions().getOauth2().setMapperScriptPath("config/mymapper.js");
		conf.getAuthenticationOptions().getOauth2().setMapperScriptDevMode(true);

		OAuth2ServerConfig realmConfig = new OAuth2ServerConfig();
		realmConfig.setAuthServerUrl("http://localhost:3000/auth");
		realmConfig.setRealm("master");
		realmConfig.setSslRequired("external");
		realmConfig.setResource("mesh");
		realmConfig.setConfidentialPort(0);
		realmConfig.addCredential("secret", "9b65c378-5b4c-4e25-b5a1-a53a381b5fb4");
		conf.getAuthenticationOptions().getOauth2().setConfig(realmConfig);
		writeYml(conf, "mesh-config.yml");
	}

	private void writeYml(Object object, String filename) throws JsonProcessingException, IOException {
		File outputFile = new File(outputFolder, filename);
		ObjectMapper ymlMapper = OptionsLoader.getYAMLMapper();
		FileUtils.writeStringToFile(outputFile, ymlMapper.writeValueAsString(object));
		System.out.println("Wrote: " + outputFile.getAbsolutePath());
	}

	private void writeJson(Object object, String filename) throws JsonProcessingException, IOException {
		File outputFile = new File(outputFolder, filename);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		FileUtils.writeStringToFile(outputFile, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
		System.out.println("Wrote: " + outputFile.getAbsolutePath());

	}

}
