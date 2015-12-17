package com.gentics.mesh.core.rest.schema;

/**
 * A schema storage is a store which hold schemas. Schema storages are used to quickly load a schema in order to deserialize or serialize a node.
 *
 */
public interface SchemaStorage {

	/**
	 * Remove the schema with the given name from the storage.
	 * 
	 * @param name
	 *            Schema name
	 */
	void removeSchema(String name);

	/**
	 * Return the schema with the given name.
	 * 
	 * @param name
	 *            Schema name
	 * @return Found schema or null when no schema could be found
	 */
	Schema getSchema(String name);

	/**
	 * Add the given schema to the storage. Existing schemas will be updated.
	 * 
	 * @param schema
	 *            Schema
	 */
	void addSchema(Schema schema);

	/**
	 * Get the microschema with the given name
	 * 
	 * @param name
	 *            microschema name
	 * @return microschema instance or null if the schema could not be found
	 */
	Microschema getMicroschema(String name);

	/**
	 * Add the given microschema to the storage
	 * 
	 * @param microschema
	 *            microschema instance
	 */
	void addMicroschema(Microschema microschema);

	/**
	 * Remove the microschema with the given name from the storage
	 * 
	 * @param name
	 *            microschema name
	 */
	void removeMicroschema(String name);

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
