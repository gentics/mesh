package com.gentics.mesh.generator;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ADD_FIELD_AFTER_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DESCRIPTION_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LABEL_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.SEGMENT_FIELD_KEY;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.etc.config.MeshOptions;

public class MeshExampleGenerator {

	public static void main(String[] args) throws JsonProcessingException, IOException {
		writeMeshConfig();
		writeChangeExamples();
	}

	/**
	 * Generate json example files for schema changes.
	 * 
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	private static void writeChangeExamples() throws JsonProcessingException, IOException {
		SchemaChangeModel addFieldChange = SchemaChangeModel.createAddFieldChange("fieldToBeAdded", "list");
		addFieldChange.setProperty(ADD_FIELD_AFTER_KEY, "firstField");
		addFieldChange.setProperty(LIST_TYPE_KEY, "html");
		writeJson(addFieldChange, "addfield.json");

		SchemaChangeModel fieldTypeChange = SchemaChangeModel.createChangeFieldTypeChange("fieldToBeChanged", "html");
		writeJson(fieldTypeChange, "changefieldtype.json");

		SchemaChangeModel removeFieldChange = SchemaChangeModel.createRemoveFieldChange("fieldToBeRemoved");
		writeJson(removeFieldChange, "removefield.json");

		SchemaChangeModel updateFieldChange = SchemaChangeModel.createUpdateFieldChange("fieldToBeUpdated");
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

	private static void writeMeshConfig() throws JsonProcessingException, IOException {

		MeshOptions conf = new MeshOptions();
		conf.setTempDirectory("/opt/mesh/data/tmp");
		conf.getUploadOptions().setTempDirectory("/opt/mesh/data/tmp/temp-uploads");
		writeJson(conf, "mesh-config.json");
	}

	private static void writeJson(Object object, String filename) throws JsonProcessingException, IOException {

		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "src" + File.separator + "main" + File.separator + "docs" + File.separator + "json";
		}
		new File(baseDirProp).mkdirs();
		File outputFile = new File(baseDirProp, filename);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		FileUtils.writeStringToFile(outputFile, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
		System.out.println("Wrote: " + outputFile.getAbsolutePath());

	}

}
