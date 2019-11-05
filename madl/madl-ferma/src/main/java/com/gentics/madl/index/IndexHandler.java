package com.gentics.madl.index;

import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.index.ElementIndexDefinition;
import com.gentics.mesh.madl.index.impl.EdgeIndexDefinitionImpl.EdgeIndexDefinitonBuilder;
import com.gentics.mesh.madl.index.impl.VertexIndexDefinitionImpl.VertexIndexDefinitionBuilder;
import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.VertexFrame;

public interface IndexHandler {

	/**
	 * Invoke a reindex of the graph database indices.
	 */
	void reindex();

	default void createIndex(VertexIndexDefinitionBuilder builder) {
		createIndex(builder.build());
	}

	default void createIndex(EdgeIndexDefinitonBuilder builder) {
		createIndex(builder.build());
	}

	void createIndex(ElementIndexDefinition def);

	/**
	 * Remove the index.
	 * 
	 * @param indexName
	 * @param clazz
	 */
	void removeVertexIndex(String indexName, Class<? extends VertexFrame> clazz);

	/**
	 * Remove the index.
	 * @param indexName
	 */
	void removeIndex(String indexName);

	/**
	 * Perform an edge SB-Tree index lookup. This method will load the index for the given edge label and postfix and return a list of all inbound vertex ids
	 * for the found edges. The key defines the outbound edge vertex id which is used to filter the edges.
	 * 
	 * @param edgeLabel
	 * @param indexPostfix
	 * @param key
	 *            outbound vertex id of the edge to be checked
	 * @return List of found inbound vertex ids for the found edges
	 */
	List<Object> edgeLookup(String edgeLabel, String indexPostfix, Object key);

	/**
	 * Add edge index for the given fields.
	 * 
	 * The index name will be constructed using the label and the index postfix (e.g: has_node_postfix)
	 * 
	 * @param label
	 * @param indexPostfix
	 *            postfix of the index
	 * @param fields
	 * @param unique
	 *            Whether to create a unique key index or not
	 */
	void addCustomEdgeIndex(String label, String indexPostfix, FieldMap fields, boolean unique);

	/**
	 * Create a composed index key
	 * 
	 * @param keys
	 * @return
	 */
	Object createComposedIndexKey(Object... keys);

	/**
	 * Check whether the values can be put into the given index for the given element.
	 * 
	 * @param indexName
	 *            index name
	 * @param element
	 *            element
	 * @param key
	 *            index key to check
	 * @return the conflicting element or null if no conflict exists
	 */
	<T extends ElementFrame> T checkIndexUniqueness(String indexName, T element, Object key);

	/**
	 * Check whether the value can be put into the given index for a new element of given class.
	 * 
	 * @param indexName
	 *            index name
	 * @param classOfT
	 *            class of the proposed new element
	 * @param key
	 *            index key to check
	 * @return the conflicting element or null if no conflict exists
	 */
	<T extends ElementFrame> T checkIndexUniqueness(String indexName, Class<T> classOfT, Object key);

	/**
	 * Find the framed vertex with the given uuid via the index.
	 * 
	 * @param classOfT Class of the element
	 * @param uuid Uuid of the element
	 * @return
	 */
	<T extends VertexFrame> T findByUuid(Class<? extends T> classOfT, String uuid);

}
