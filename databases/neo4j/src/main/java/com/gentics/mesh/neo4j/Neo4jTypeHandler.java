package com.gentics.mesh.neo4j;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.syncleus.ferma.type.ElementTypeDefinition;
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
}
