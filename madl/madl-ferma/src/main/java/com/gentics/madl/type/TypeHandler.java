package com.gentics.madl.type;

import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;

import java.util.stream.Stream;

import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.type.ElementTypeDefinition;
import com.gentics.mesh.madl.type.impl.EdgeTypeDefinitionImpl.EdgeTypeDefinitionBuilder;
import com.gentics.mesh.madl.type.impl.VertexTypeDefinitionImpl.VertexTypeDefinitionBuilder;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public interface TypeHandler {

	default void createType(VertexTypeDefinitionBuilder builder) {
		createType(builder.build());
	}

	default void createType(EdgeTypeDefinitionBuilder builder) {
		createType(builder.build());
	}

	void createType(ElementTypeDefinition def);

	/**
	 * Create a new vertex type for the given vertex class type.
	 * 
	 * @param clazzOfVertex
	 * @param superClazzOfVertex
	 *            Super vertex type. If null "V" will be used.
	 */
	default void createVertexType(Class<?> clazz, Class<?> superClazz) {
		createType(vertexType(clazz, superClazz));
	}

	/**
	 * Change the element type.
	 * 
	 * @param vertex
	 * @param newType
	 * @param tx
	 * @return
	 */
	Vertex changeType(Vertex vertex, String newType, Graph tx);

	/**
	 * Create a new vertex type.
	 * 
	 * @param clazzOfVertex
	 * @param superClazzOfVertex
	 *            Super vertex type. If null "V" will be used.
	 */
	void addVertexType(String clazzOfVertex, String superClazzOfVertex);

	/**
	 * Remove the vertex type with the given name.
	 * 
	 * @param string
	 */
	@Deprecated
	void removeVertexType(String typeName);

	/**
	 * Remove the edge type with the given name.
	 * 
	 * @param typeName
	 */
	@Deprecated
	void removeEdgeType(String typeName);

	/**
	 * Update the vertex type for the given element using the class type.
	 * 
	 * @param element
	 * @param classOfVertex
	 */
	void setVertexType(Element element, Class<?> classOfVertex);

	<T extends VertexFrame> long count(Class<? extends T> persistanceClass);

	<T extends VertexFrame> Stream<T> findAll(Class<? extends T> classOfT);

}
