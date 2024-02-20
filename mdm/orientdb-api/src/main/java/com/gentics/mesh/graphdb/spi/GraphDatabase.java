package com.gentics.mesh.graphdb.spi;

import java.util.Iterator;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.parameter.PagingParameters;

public interface GraphDatabase extends Database {

	/**
	 * Return the type handler for the database.
	 */
	TypeHandler type();

	/**
	 * Utilize the index and locate the matching vertices.
	 *
	 * @param classOfVertex
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	default Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		return getVertices(classOfVertex, fieldNames, fieldValues, null, Optional.empty(), Optional.empty());
	}

	/**
	 * Utilize the index and locate the matching vertices, considering paging parameters. Optionally, data fetch filtering may be applied.
	 *
	 * @param classOfVertex
	 * @param fieldNames
	 * @param fieldValues
	 * @param paging
	 * @param maybeFilter
	 * @return
	 */
	Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues, PagingParameters paging, Optional<ContainerType> maybeContainerType, Optional<String> maybeFilter);

	/**
	 * Utilize the index and locate the matching vertices for the given parameters and the given range.
	 * 
	 * @param classOfVertex
	 * @param postfix
	 * @param fieldNames
	 * @param fieldValues
	 * @param rangeKey
	 * @param start
	 * @param end
	 * @return
	 */
	Iterable<Vertex> getVerticesForRange(Class<?> classOfVertex, String postfix, String[] fieldNames, Object[] fieldValues, String rangeKey,
		long start,
		long end);

	/**
	 * Utilize the index and locate the matching vertices.
	 *
	 * @param <T>
	 *            Type of the vertices
	 * @param classOfVertex
	 *            Class to be used for framing
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	<T extends VertexFrame> Result<T> getVerticesTraversal(Class<T> classOfVertex, String[] fieldNames, Object[] fieldValues);

	/**
	 * Utilize the index and locate the matching vertices.
	 *
	 * @param classOfVertex
	 *            Class to be used for framing
	 * @param fieldName
	 * @param fieldValue
	 * @return
	 */
	default <T extends VertexFrame> Result<T> getVerticesTraversal(Class<T> classOfVertex, String fieldName, Object fieldValue) {
		return getVerticesTraversal(classOfVertex, new String[] { fieldName }, new Object[] { fieldValue });
	}

	/**
	 * Get the underlying raw transaction.
	 * 
	 * @return
	 */
	Graph rawTx();

	/**
	 * Find the vertex with the given key/value setup. Indices which provide this information will automatically be utilized.
	 * 
	 * @param propertyKey
	 * @param propertyValue
	 * @param clazz
	 * @return Found element or null if no element was found
	 */
	<T extends MeshElement> T findVertex(String propertyKey, Object propertyValue, Class<T> clazz);

	/**
	 * Find the edge with the given key/value setup. Indices which provide this information will automatically be utilized.
	 * 
	 * @param propertyKey
	 * @param propertyValue
	 * @param clazz
	 * @return Found element or null if no element was found
	 */
	<T extends EdgeFrame> T findEdge(String propertyKey, Object propertyValue, Class<T> clazz);

	/**
	 * Return the index handler for the database.
	 * 
	 * @return
	 */
	IndexHandler index();

	/**
	 * Return the graph element version.
	 * 
	 * @param element
	 * @return
	 */
	String getElementVersion(Element element);
}
