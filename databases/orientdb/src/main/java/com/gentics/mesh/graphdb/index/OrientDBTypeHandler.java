package com.gentics.mesh.graphdb.index;

import static com.gentics.mesh.graphdb.FieldTypeMapper.toSubType;
import static com.gentics.mesh.graphdb.FieldTypeMapper.toType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.syncleus.ferma.index.field.FieldMap;
import com.syncleus.ferma.index.field.FieldType;
import com.syncleus.ferma.type.EdgeTypeDefinition;
import com.syncleus.ferma.type.ElementTypeDefinition;
import com.syncleus.ferma.type.VertexTypeDefinition;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class OrientDBTypeHandler implements TypeHandler {

	private static final Logger log = LoggerFactory.getLogger(OrientDBTypeHandler.class);
	private Lazy<OrientDBDatabase> db;

	@Inject
	public OrientDBTypeHandler(Lazy<OrientDBDatabase> db) {
		this.db = db;
	}

	@Override
	public void addVertexType(String clazzOfVertex, String superClazzOfVertex) {

		if (log.isDebugEnabled()) {
			log.debug("Adding vertex type for class {" + clazzOfVertex + "}");
		}
		OrientGraphNoTx noTx = db.get().getTxProvider().rawNoTx();
		try {
			OrientVertexType vertexType = noTx.getVertexType(clazzOfVertex);
			if (vertexType == null) {
				String superClazz = "V";
				if (superClazzOfVertex != null) {
					superClazz = superClazzOfVertex;
				}
				vertexType = noTx.createVertexType(clazzOfVertex, superClazz);
			} else {
				// Update the existing vertex type and set the super class
				if (superClazzOfVertex != null) {
					OrientVertexType superType = noTx.getVertexType(superClazzOfVertex);
					if (superType == null) {
						throw new RuntimeException("The supertype for vertices of type {" + clazzOfVertex + "} can't be set since the supertype {"
							+ superClazzOfVertex + "} was not yet added to orientdb.");
					}
					vertexType.setSuperClass(superType);
				}
			}
		} finally {
			noTx.shutdown();
		}

	}

	@Override
	public void createVertexType(Class<?> clazzOfVertex, Class<?> superClazzOfVertex) {
		String superClazz = superClazzOfVertex == null ? null : superClazzOfVertex.getSimpleName();
		addVertexType(clazzOfVertex.getSimpleName(), superClazz);
	}

	@Override
	public void removeEdgeType(String typeName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex type with name {" + typeName + "}");
		}
		OrientGraphNoTx noTx = db.get().getTxProvider().rawNoTx();
		try {
			noTx.dropEdgeType(typeName);
		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public void removeVertexType(String typeName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex type with name {" + typeName + "}");
		}
		OrientGraphNoTx noTx = db.get().getTxProvider().rawNoTx();
		try {
			OrientVertexType type = noTx.getVertexType(typeName);
			if (type != null) {
				noTx.dropVertexType(typeName);
			}
		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public Vertex changeType(Vertex vertex, String newType, Graph tx) {
		OrientVertex v = (OrientVertex) vertex;
		ORID newId = v.moveToClass(newType);
		return tx.getVertex(newId);
	}

	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex) element).getBaseElement();
		}
		((OrientVertex) element).moveToClass(classOfVertex.getSimpleName());
	}

	@Override
	public void createType(ElementTypeDefinition def) {
		if (def instanceof VertexTypeDefinition) {
			VertexTypeDefinition vertexType = (VertexTypeDefinition) def;
			createVertexType(vertexType.getClazz(), vertexType.getSuperClazz());
		} else if (def instanceof EdgeTypeDefinition) {
			EdgeTypeDefinition edgeType = (EdgeTypeDefinition) def;
			createEdgeType(edgeType);
		}
	}

	private void createEdgeType(EdgeTypeDefinition def) {
		String label = def.getLabel();
		Class<?> superClazzOfEdge = def.getSuperClazz();

		FieldMap fields = def.getFields();
		if (log.isDebugEnabled()) {
			log.debug("Adding edge type for label {" + label + "}");
		}
		OrientGraphNoTx noTx = db.get().getTxProvider().rawNoTx();
		try {
			OrientEdgeType e = noTx.getEdgeType(label);
			if (e == null) {
				String superClazz = "E";
				if (superClazzOfEdge != null) {
					superClazz = superClazzOfEdge.getSimpleName();
				}
				e = noTx.createEdgeType(label, superClazz);
			} else {
				// Update the existing edge type and set the super class
				if (superClazzOfEdge != null) {
					OrientEdgeType superType = noTx.getEdgeType(superClazzOfEdge.getSimpleName());
					if (superType == null) {
						throw new RuntimeException("The supertype for edges with label {" + label + "} can't be set since the supertype {"
							+ superClazzOfEdge.getSimpleName() + "} was not yet added to orientdb.");
					}
					e.setSuperClass(superType);
				}
			}

			if (fields != null) {
				for (String key : fields.keySet()) {
					if (e.getProperty(key) == null) {
						FieldType fieldType = fields.get(key);
						OType type = toType(fieldType);
						OType subType = toSubType(fieldType);
						if (subType != null) {
							e.createProperty(key, type, subType);
						} else {
							e.createProperty(key, type);
						}
					}
				}
			}
		} finally {
			noTx.shutdown();
		}

	}

}
