package com.gentics.mesh.core.rest.schema.change.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Name of schema update operations which can be handled.
 */
public enum SchemaChangeOperation {

	ADDFIELD, REMOVEFIELD, CHANGEFIELDTYPE, UPDATEFIELD, UPDATESCHEMA, UPDATEMICROSCHEMA;

	private static final Logger log = LoggerFactory.getLogger(SchemaChangeOperation.class);

	/**
	 * Get the auto migration script for this change
	 * @param properties change properties
	 * @return auto migration script, may be null
	 * @throws IOException
	 */
	public String getAutoMigrationScript(Map<String, Object> properties) throws IOException {
		switch(this) {
		case ADDFIELD:
			return null;
		case CHANGEFIELDTYPE:
			String newType = String.valueOf(properties.get("type"));
			if (newType != null) {
				switch (newType) {
				case "binary":
					return loadAutoMigrationScript("typechange_binary.js");
				case "boolean":
					return loadAutoMigrationScript("typechange_boolean.js");
				case "date":
					return loadAutoMigrationScript("typechange_date.js");
				case "micronode":
					return loadAutoMigrationScript("typechange_micronode.js");
				case "node":
					return loadAutoMigrationScript("typechange_node.js");
				case "number":
					return loadAutoMigrationScript("typechange_number.js");
				case "html":
				case "string":
					return loadAutoMigrationScript("typechange_string.js");
				case "list":
					String newListType = String.valueOf(properties.get("listType"));
					if (newListType != null) {
						switch (newListType) {
						case "boolean":
							return loadAutoMigrationScript("typechange_booleanlist.js");
						case "date":
							return loadAutoMigrationScript("typechange_datelist.js");
						case "micronode":
							return loadAutoMigrationScript("typechange_micronodelist.js");
						case "node":
							return loadAutoMigrationScript("typechange_nodelist.js");
						case "number":
							return loadAutoMigrationScript("typechange_numberlist.js");
						case "html":
						case "string":
							return loadAutoMigrationScript("typechange_stringlist.js");
						}
					}
				}
			}
		case REMOVEFIELD:
			return loadAutoMigrationScript("fieldremove.js");
		case UPDATEFIELD:
			return null;
		case UPDATESCHEMA:
			return null;
		default:
			return null;
		}
	}

	/**
	 * Load the automatic migration script with given name
	 * @param scriptName script name
	 * @return script file contents
	 * @throws IOException
	 */
	private String loadAutoMigrationScript(String scriptName) throws IOException {
		try (InputStream ins = getClass().getResourceAsStream("/script/" + scriptName)) {
			if (ins == null) {
				log.error("Json could not be loaded from classpath file {" + scriptName + "}");
				throw new FileNotFoundException("Could not find script file {" + scriptName + "}");
			} else {
				StringWriter writer = new StringWriter();
				try {
					IOUtils.copy(ins, writer);
					return writer.toString();
				} catch (IOException e) {
					log.error("Error while reading script file {" + scriptName + "}", e);
					throw e;
				}
			}
		}
	}
}
