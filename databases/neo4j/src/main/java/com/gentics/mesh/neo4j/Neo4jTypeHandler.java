package com.gentics.mesh.neo4j;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.type.ElementTypeDefinition;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

@Singleton
public class Neo4jTypeHandler implements TypeHandler {

	@Inject
	public Neo4jTypeHandler() {
	}

	@Override
	public void createType(ElementTypeDefinition def) {
		// TODO Auto-generated method stub

	}

	@Override
	public Vertex changeType(Vertex vertex, String newType, Graph tx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addVertexType(String clazzOfVertex, String superClazzOfVertex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeVertexType(String typeName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeEdgeType(String typeName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		// TODO Auto-generated method stub
	}

	@Override
	public <T extends VertexFrame> long count(Class<? extends T> persistanceClass) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T extends VertexFrame> Stream<T> findAll(Class<? extends T> classOfT) {
		return null;
	}
}
