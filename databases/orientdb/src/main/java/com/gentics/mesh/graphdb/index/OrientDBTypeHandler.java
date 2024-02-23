package com.gentics.mesh.graphdb.index;

import static com.gentics.mesh.graphdb.FieldTypeMapper.toSubType;
import static com.gentics.mesh.graphdb.FieldTypeMapper.toType;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientVertex;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;

import com.gentics.madl.ext.orientdb.DelegatingFramedOrientGraph;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.type.EdgeTypeDefinition;
import com.gentics.mesh.madl.type.ElementTypeDefinition;
import com.gentics.mesh.madl.type.VertexTypeDefinition;
import com.gentics.mesh.util.StreamUtil;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.tx.OTransactionNoTx;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The type handler is used to manage OrientDB type system and create, update, removed types.
 */
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
		OrientGraph noTx = db.get().getTxProvider().rawNoTx();
		try {
			OClass vertexType = noTx.getRawDatabase().getMetadata().getSchema().getClass(clazzOfVertex);
			if (vertexType == null) {
				String superClazz = "V";
				if (superClazzOfVertex != null) {
					superClazz = superClazzOfVertex;
				}
				final OClass superType = noTx.getRawDatabase().getMetadata().getSchema().getClass(superClazz);
				vertexType = noTx.getRawDatabase().getMetadata().getSchema().createClass(clazzOfVertex, superType);
			} else {
				// Update the existing vertex type and set the super class
				if (superClazzOfVertex != null) {
					final OClass superType = noTx.getRawDatabase().getMetadata().getSchema().getClass(superClazzOfVertex);
					if (superType == null) {
						throw new RuntimeException("The supertype for vertices of type {" + clazzOfVertex + "} can't be set since the supertype {"
							+ superClazzOfVertex + "} was not yet added to orientdb.");
					}
					vertexType.setSuperClass(superType);
				}
			}
		} finally {
			noTx.close();
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
		OrientGraph noTx = db.get().getTxProvider().rawNoTx();
		try {
			noTx.getRawDatabase().getMetadata().getSchema().dropClass(typeName);
		} finally {
			noTx.close();
		}
	}

	@Override
	public void removeVertexType(String typeName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex type with name {" + typeName + "}");
		}
		OrientGraph noTx = db.get().getTxProvider().rawNoTx();
		try {
			final OClass type = noTx.getRawDatabase().getMetadata().getSchema().getClass(typeName);
			if (type != null) {
				noTx.getRawDatabase().getMetadata().getSchema().dropClass(typeName);
			}
		} finally {
			noTx.close();
		}
	}

	@Override
	public Vertex changeType(Vertex vertex, String newType, Graph tx) {
		OrientVertex v = (OrientVertex) vertex;
		ORID newId = v.getRawElement().moveTo(newType, null);
		return tx.vertices(newId).next();
	}

	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex<Element>) element).getBaseVertex();
		}
		((OrientVertex) element).getRawElement().moveTo(classOfVertex.getSimpleName(), null);
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
		OrientGraph noTx = db.get().getTxProvider().rawNoTx();
		try {
			OClass e = noTx.getRawDatabase().getMetadata().getSchema().getClass(label);
			if (e == null) {
				String superClazz = "E";
				if (superClazzOfEdge != null) {
					superClazz = superClazzOfEdge.getSimpleName();
				}
				final OClass superType = noTx.getRawDatabase().getMetadata().getSchema().getClass(superClazz);
				e = noTx.getRawDatabase().getMetadata().getSchema().createClass(label, superType);
			} else {
				// Update the existing edge type and set the super class
				if (superClazzOfEdge != null) {
					final OClass superType = noTx.getRawDatabase().getMetadata().getSchema().getClass(superClazzOfEdge.getSimpleName());
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
			noTx.close();
		}
	}

	@Override
	public <T extends VertexFrame> long count(Class<? extends T> persistanceClass) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph orientBaseGraph = ((OrientBaseGraph) baseGraph);
		return orientBaseGraph.countVertices(persistanceClass.getSimpleName());
	}

	@Override
	public <T extends VertexFrame> Stream<T> findAll(Class<? extends T> classOfT) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph orientBaseGraph = ((OrientBaseGraph) baseGraph);

		return StreamUtil.toStream(orientBaseGraph.getVerticesOfClass(classOfT.getSimpleName())).map(v -> {
			return (T) graph.getFramedVertexExplicit(classOfT, v.getId());
		});
	}
}
