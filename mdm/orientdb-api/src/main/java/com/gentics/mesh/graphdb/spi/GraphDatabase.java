package com.gentics.mesh.graphdb.spi;

import java.util.Iterator;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

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
	Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues);

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
	 * Locate all vertices for the given type.
	 * 
	 * @param classOfVertex
	 * @return
	 */
	<T extends MeshVertex> Iterator<? extends T> getVerticesForType(Class<T> classOfVertex);

	/**
	 * Get the underlying raw transaction.
	 * 
	 * @return
	 */
	TransactionalGraph rawTx();

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
