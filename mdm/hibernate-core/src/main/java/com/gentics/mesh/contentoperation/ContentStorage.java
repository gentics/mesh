package com.gentics.mesh.contentoperation;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * A collection of methods which abstract operations on content tables.
 */
public interface ContentStorage {

	/**
	 * Find one field container with the provided version and the provided UUID
	 * @param version mandatory version
	 * @param contentUuid mandatory content id
	 * @return the field container if found, otherwise null
	 */
	HibNodeFieldContainerImpl findOne(FieldSchemaVersionElement<?,?,?,?,?> version, UUID contentUuid);

	/**
	 * Find one field container for the given {@link ContentKey}.
	 * @param key The content key to search for.
	 * @return The field container if found, and {@code null} otherwise.
	 * @param <T>
	 */
	<T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> T findOne(ContentKey key);

	/**
	 * Find one micro field container with the provided version and the provided UUID
	 * @param version mandatory version
	 * @param contentUuid mandatory content id
	 * @return the micro field container if found, otherwise null
	 */
	HibMicronodeContainerImpl findOneMicronode(FieldSchemaVersionElement<?,?,?,?,?> version, UUID contentUuid);

	/**
	 * Find all containers with the provided version.
	 *
	 * @param version mandatory version of the node field containers
	 * @return a list of field containers
	 */
	List<HibNodeFieldContainerImpl> findMany(FieldSchemaVersionElement<?,?,?,?,?> version);

	/**
	 * Find all containers with the provided version.
	 *
	 * @param version mandatory version of the micronode field containers
	 * @return a list of field containers
	 */
	List<HibMicronodeContainerImpl> findManyMicronodes(FieldSchemaVersionElement<?,?,?,?,?> version);

	/**
	 * Find many field containers for the given edges having columns between the provided parameters.
	 *
	 * @param edges mandatory edges of field containers
	 * @param columnBetween a triple, where left is the content column and middle and right are the bound to be checked
	 * @return a stream of field containers
	 */
	<T extends Comparable<T>> Stream<HibNodeFieldContainerImpl> findMany(List<HibNodeFieldContainerEdgeImpl> edges, Triple<ContentColumn, T, T> columnBetween);

	/**
	 * Find many field containers for the provided edges
	 *
	 * @param edges mandatory edges
	 * @return a list of field containers
	 */
	List<HibNodeFieldContainerImpl> findMany(Collection<HibNodeFieldContainerEdgeImpl> edges);

	/**
	 * Find many node field containers for the provided keys
	 * @param contentKeys
	 * @return
	 */
	List<HibNodeFieldContainerImpl> findMany(Set<ContentKey> contentKeys);

	/**
	 * Find many micronode field containers for the provided keys
	 * @param contentKeys
	 * @return
	 */
	List<HibMicronodeContainerImpl> findManyMicronodes(Set<ContentKey> contentKeys);

	/**
	 * Find column value for the given version and contentUuid
	 * @param version mandatory version
	 * @param contentUuid mandatory content id
	 * @param contentColumn mandatory content column
	 * @param <T>
	 * @return
	 */
	<T> T findColumn(FieldSchemaVersionElement<?,?,?,?,?> version, UUID contentUuid, ContentColumn contentColumn);

	/**
	 * Insert the container into the content table
	 * @param container container to insert
	 * @param schemaVersion schema version
	 */
	void insert(HibNodeFieldContainerImpl container, SchemaVersion schemaVersion);

	/**
	 * Insert the micronode container into the content table
	 * @param container micronode container to insert
	 * @param microschemaVersion schema version
	 */
	void insert(HibMicronodeContainerImpl container, MicroschemaVersion microschemaVersion);

	/**
	 * Deletes the (micro)node field container with the provided id and version
	 * @param id mandatory id
	 * @param version mandatory version
	 */
	void delete(UUID id, FieldSchemaVersionElement<?, ?, ?, ?, ?> version);

	/**
	 * Deletes the (micro)node field containers for the given version and project.
	 * @param version mandatory version
	 * @param project mandatory project
	 */
	long delete(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, Project project);

	/**
	 * Deletes the micronodes for the given version related to one of the provided nodes
	 * @param version
	 * @param nodes
	 * @return
	 */
	long delete(SchemaVersion version, Set<HibNodeImpl> nodes);

	/**
	 * Delete all the containers that are referenced by the provided keys.
	 * @param contentKeys
	 */
	long delete(Set<ContentKey> contentKeys);

	/**
	 * Deletes the (micro)node field containers for the given version and project.
	 * @param version mandatory version
	 *
	 */
	long deleteUnreferencedMicronodes(MicroschemaVersion version);

	/**
	 * Deletes all the node field containers
	 * @param nodeFieldContainers
	 */
	void delete(Collection<? extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> nodeFieldContainers);

	/**
	 * Drop the content table for the version. This is added to the current HibernateTx to be done before the transaction is closed.
	 * @param version mandatory  version
	 */
	void dropTable(FieldSchemaVersionElement<?, ?, ?, ?, ?> version);

	/**
	 * Return the count of all NodeFieldContainers
	 * @return
	 */
	long getGlobalCount();

	/**
	 * Check whether the column already exists. If it doesn't, a new one will be created
	 * Use this method only for testing purposes, since we should avoid modifying a content table after it has been created.
	 * @param version the version used to identify the table
	 * @param column the column to add
	 */
	void addColumnIfNotExists(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, DynamicContentColumn column);

	/**
	 * Create a node field container table. The table creation will be done at the end of the current transaction,
	 * since DDL operations might commit pending DML operations, depending on the database
	 * @param version
	 */
	void createTable(SchemaVersion version);

	/**
	 * Create an index on the nodes field container table for the version. The index will be added at the end of the transaction
	 * @param version
	 * @param column
	 * @param unique
	 */
	void createIndex(SchemaVersion version, CommonContentColumn column, boolean unique);

	/**
	 * Create a micronode field container table. The table creation will be done at the end of the current transaction,
	 * since DDL operations might commit pending DML operations, depending on the database
	 * @param microVersion
	 */
	void createMicronodeTable(MicroschemaVersion microVersion);

	/**
	 * Return the content keys of the field containers for the given versions related to one of the provided nodes.
	 * @param version
	 * @param nodes
	 * @return
	 */
	List<ContentKey> findByNodes(SchemaVersion version, Set<HibNodeImpl> nodes);
}
