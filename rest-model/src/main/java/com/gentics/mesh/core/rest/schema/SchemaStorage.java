package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.microschema.MicroschemaModel;

/**
 * A schema storage is a store which hold schemas. Schema storages are used to quickly load a schema in order to deserialize or serialize a node. TODO: add
 * version
 */
public interface SchemaStorage {
	/**
	 * Remove the schema with the given name in all versions from the storage.
	 * 
	 * @param name
	 *            Schema name
	 */
	void removeSchema(String name);

	/**
	 * Remove the schema with the given name in the given version from the storage.
	 * 
	 * @param name
	 *            Schema name
	 * @param version
	 *            Schema version
	 */
	void removeSchema(String name, String version);

	/**
	 * Return the schema with the given name in the newest version.
	 * 
	 * @param name
	 *            Schema name
	 * @return Found schema or null when no schema could be found
	 */
	SchemaUpdateModel getSchema(String name);

	/**
	 * Return the schema with the given name in the given version.
	 * 
	 * @param name
	 *            Schema name
	 * @param version
	 *            Schema version
	 * @return Found schema or null when no schema could be found
	 */
	SchemaUpdateModel getSchema(String name, String version);

	/**
	 * Add the given schema to the storage. Existing schemas will be updated.
	 * 
	 * @param schema
	 *            Schema
	 */
	void addSchema(SchemaUpdateModel schema);

	/**
	 * Get the microschema with the given name in the newest version
	 * 
	 * @param name
	 *            microschema name
	 * @return microschema instance or null if the schema could not be found
	 */
	MicroschemaModel getMicroschema(String name);

	/**
	 * Return the microschema with the given name in the given version.
	 * 
	 * @param name
	 *            Microschema name
	 * @param version
	 *            Microschema version
	 * @return Found microschema or null when no microschema could be found
	 */
	MicroschemaModel getMicroschema(String name, String version);

	/**
	 * Add the given microschema to the storage
	 * 
	 * @param microschema
	 *            microschema instance
	 */
	void addMicroschema(MicroschemaModel microschema);

	/**
	 * Remove the microschema with the given name from the storage
	 * 
	 * @param name
	 *            microschema name
	 */
	void removeMicroschema(String name);

	/**
	 * Remove the microschema with the given name in the given version from the storage.
	 * 
	 * @param name
	 *            microschema name
	 * @param version
	 *            microschema version
	 */
	void removeMicroschema(String name, String version);

	/**
	 * Return the size of the storage (schemas an microschemas)
	 * 
	 * @return Size of the storage
	 */
	int size();

	/**
	 * Clear the storage and remove all stored schemas and microschemas
	 */
	void clear();

}
