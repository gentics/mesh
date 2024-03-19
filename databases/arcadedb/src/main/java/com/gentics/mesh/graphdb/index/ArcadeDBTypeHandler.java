package com.gentics.mesh.graphdb.index;

import static com.gentics.mesh.graphdb.FieldTypeMapper.toSubType;
import static com.gentics.mesh.graphdb.FieldTypeMapper.toType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;

import com.arcadedb.database.Database;
import com.arcadedb.database.RID;
import com.arcadedb.exception.SchemaException;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.gremlin.ArcadeEdge;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.gremlin.ArcadeVertex;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.EdgeType;
import com.arcadedb.schema.Type;
import com.arcadedb.schema.VertexType;
import com.gentics.madl.ext.arcadedb.DelegatingFramedArcadeGraph;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.ArcadeDBDatabase;
import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.type.EdgeTypeDefinition;
import com.gentics.mesh.madl.type.ElementTypeDefinition;
import com.gentics.mesh.madl.type.VertexTypeDefinition;
import com.gentics.mesh.util.StreamUtil;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The type handler is used to manage ArcadeDB type system and create, update, removed types.
 */
@Singleton
public class ArcadeDBTypeHandler implements TypeHandler {

	private static final Logger log = LoggerFactory.getLogger(ArcadeDBTypeHandler.class);
	private Lazy<ArcadeDBDatabase> db;

	@Inject
	public ArcadeDBTypeHandler(Lazy<ArcadeDBDatabase> db) {
		this.db = db;
	}

	@Override
	public void addVertexType(String clazzOfVertex, String superClazzOfVertex) {

		if (log.isDebugEnabled()) {
			log.debug("Adding vertex type for class {" + clazzOfVertex + "}");
		}
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			DocumentType vertexType;
			try {
				vertexType = noTx.getSchema().getType(clazzOfVertex);
			} catch (SchemaException e) {
				log.info(e.getLocalizedMessage());
				vertexType = null;
			}
			if (vertexType == null || !(vertexType instanceof VertexType)) {
				vertexType = noTx.getSchema().createVertexType(clazzOfVertex);
				if (superClazzOfVertex != null) {
					vertexType = vertexType.addSuperType(superClazzOfVertex);
				}
			} else {
				// Update the existing vertex type and set the super class
				if (superClazzOfVertex != null) {
					DocumentType superType;
					try {
						superType = noTx.getSchema().getType(superClazzOfVertex);
					} catch (SchemaException e) {
						superType = null;
						log.error(StringUtils.EMPTY, e);
					}
					if (superType == null || !(superType instanceof VertexType)) {
						throw new RuntimeException("The supertype for vertices of type {" + clazzOfVertex + "} can't be set since the supertype {"
							+ superClazzOfVertex + "} was not yet added to the db.");
					}
					vertexType.addSuperType(superType);
				}
			}
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
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			try {
				final DocumentType type = noTx.getSchema().getType(typeName);
				if (type != null && type instanceof EdgeType) {
					noTx.getSchema().dropType(typeName);
				}
			} catch (Exception e) {
				log.warn("Could not delete edge class {" + typeName + "}", e);
			}
		}
	}

	@Override
	public void removeVertexType(String typeName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex type with name {" + typeName + "}");
		}
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			try {
				final DocumentType type = noTx.getSchema().getType(typeName);
				if (type != null && type instanceof VertexType) {
					noTx.getSchema().dropType(typeName);
				}
			} catch (Exception e) {
				log.warn("Could not delete vertex class {" + typeName + "}", e);
			}
		}
	}

	@Override
	public Vertex changeType(Vertex vertex, String newType, Graph tx) {
		ArcadeVertex v = (ArcadeVertex) vertex;
		ArcadeGraph atx = (ArcadeGraph) tx;
		final DocumentType type = atx.getDatabase().getSchema().getType(newType);
		if (type == null || !(type instanceof VertexType)) {
			throw new IllegalArgumentException("Unknown target type: " + newType);
		}
		RID id = v.getIdentity();
		MutableVertex newVertex = (MutableVertex) type.newRecord();
		v.properties().forEachRemaining(p -> {
			newVertex.set(p.key(), p.value());
		});
		v.edges(Direction.OUT).forEachRemaining(e -> {
			ArcadeEdge ae = (ArcadeEdge) e;
			List<Object> properties = new ArrayList<>();
			ae.properties().forEachRemaining(p -> {
				properties.add(p.key());
				properties.add(p.value());
			});
			newVertex.newEdge(ae.getBaseElement().getTypeName(), ae.getBaseElement().getOutVertex(), false, properties.toArray());
		});
		v.edges(Direction.IN).forEachRemaining(e -> {
			ArcadeEdge ae = (ArcadeEdge) e;
			List<Object> properties = new ArrayList<>();
			ae.properties().forEachRemaining(p -> {
				properties.add(p.key());
				properties.add(p.value());
			});
			ae.getBaseElement().getOutVertex().newEdge(ae.getBaseElement().getTypeName(), newVertex, false, properties.toArray());
		});
		v.remove();
		newVertex.setIdentity(id);			
		return tx.vertices(id).next();
	}

	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex<Element>) element).getBaseVertex();
		}
		changeType((Vertex) element, classOfVertex.getSimpleName(), GraphDBTx.getGraphTx().getGraph().getBaseGraph());
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
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			DocumentType e;
			try {
				e = noTx.getSchema().getType(label);
			} catch (SchemaException e1) {
				log.info(e1.getLocalizedMessage());
				e = null;
			}
			if (e == null || !(e instanceof EdgeType)) {
				e = noTx.getSchema().createEdgeType(label);
				if (superClazzOfEdge != null) {
					e = e.addSuperType(superClazzOfEdge.getSimpleName());
				}
			} else {
				// Update the existing edge type and set the super class
				if (superClazzOfEdge != null) {
					e.addSuperType(superClazzOfEdge.getSimpleName());
				}
			}
			if (fields != null) {
				for (String key : fields.keySet()) {
					if (e.getPropertyIfExists(key) == null) {
						FieldType fieldType = fields.get(key);
						Type type = toType(fieldType);
						Type subType = toSubType(fieldType);
						if (subType != null) {
							e.createProperty(key, type, subType.name());
						} else {
							e.createProperty(key, type);
						}
					}
				}
			}
		}
	}

	@Override
	public <T extends VertexFrame> long count(Class<? extends T> persistanceClass) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedArcadeGraph) graph).getBaseGraph();
		ArcadeGraph orientBaseGraph = ((ArcadeGraph) baseGraph);
		return orientBaseGraph.getDatabase().countType(persistanceClass.getSimpleName(), true);
	}

	@Override
	public <T extends VertexFrame> Stream<T> findAll(Class<? extends T> classOfT) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedArcadeGraph) graph).getBaseGraph();
		ArcadeGraph orientBaseGraph = ((ArcadeGraph) baseGraph);

		return StreamUtil.toStream(orientBaseGraph.vertices()).filter(v -> v.label().equals(classOfT.getSimpleName())).map(v -> graph.frameElementExplicit(v, classOfT));
	}
}
