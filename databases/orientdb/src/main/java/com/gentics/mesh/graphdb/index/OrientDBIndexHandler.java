package com.gentics.mesh.graphdb.index;

import static com.gentics.mesh.graphdb.FieldTypeMapper.toSubType;
import static com.gentics.mesh.graphdb.FieldTypeMapper.toType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import com.gentics.madl.ext.orientdb.DelegatingFramedOrientGraph;
import com.gentics.madl.index.IndexHandler;
import com.gentics.mesh.core.data.PersistenceClassMap;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.index.EdgeIndexDefinition;
import com.gentics.mesh.madl.index.ElementIndexDefinition;
import com.gentics.mesh.madl.index.IndexType;
import com.gentics.mesh.madl.index.VertexIndexDefinition;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB specific implementation for index handling.
 * 
 * @see IndexHandler
 */
@Singleton
public class OrientDBIndexHandler implements IndexHandler<Object> {

	private static final Logger log = LoggerFactory.getLogger(OrientDBIndexHandler.class);

	private final Lazy<OrientDBDatabase> db;

	private final PersistenceClassMap persistenceClassMap;

	@Inject
	public OrientDBIndexHandler(Lazy<OrientDBDatabase> db, PersistenceClassMap persistenceClassMap) {
		this.db = db;
		this.persistenceClassMap = persistenceClassMap;
	}

	@Override
	public Object createComposedIndexKey(Object... keys) {
		return new OCompositeKey(keys);
	}

	@Override
	public void reindex() {
		try (OrientGraph tx = db.get().getTxProvider().rawTx()) {
			OIndexManager manager = tx.getRawDatabase().getMetadata().getIndexManager();
			manager.getIndexes().forEach(i -> i.rebuild());
		}
	}

	@Override
	@Deprecated
	public void addCustomEdgeIndex(String label, String indexPostfix, FieldMap fields, boolean unique) {
		try (OrientGraph noTx = db.get().getTxProvider().rawNoTx()) {
			final OClass e = noTx.getRawDatabase().getMetadata().getSchema().getClass(label);
		    if (e == null) {
				throw new RuntimeException("Could not find edge type {" + label + "}. Create edge type before creating indices.");
			}
			for (String key : fields.keySet()) {
				if (e.getProperty(key) == null) {
					FieldType type = fields.get(key);
					OType otype = toType(type);
					OType osubType = toSubType(type);
					if (osubType != null) {
						e.createProperty(key, otype, osubType);
					} else {
						e.createProperty(key, otype);
					}
				}
			}
			String name = "e." + label + "_" + indexPostfix;
			name = name.toLowerCase();
			if (fields.size() != 0 && e.getClassIndex(name) == null) {
				String[] fieldArray = fields.keySet().stream().toArray(String[]::new);
				OIndex idx = e.createIndex(name,
					unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(), null,
					new ODocument().fields("ignoreNullValues", true), fieldArray);
				if (idx == null) {
					new RuntimeException("Index for {" + label + "/" + indexPostfix + "} was not created.");
				}
			}

		}
	}

	@Override
	public List<Object> edgeLookup(String edgeLabel, String indexPostfix, Object key) {
		OrientGraph orientBaseGraph = db.get().unwrapCurrentGraph();
		List<Object> ids = new ArrayList<>();

		// Load the edge type in order to access the indices of the edge
		final OClass edgeType = orientBaseGraph.getRawDatabase().getMetadata().getSchema().getClass(edgeLabel);
		if (edgeType != null) {
			// Fetch the required index
			OIndex index = edgeType.getClassIndex("e." + edgeLabel.toLowerCase() + "_" + indexPostfix);
			if (index != null) {
				// Iterate over the sb-tree index entries
				OIndexCursor cursor = index.iterateEntriesMajor(new OCompositeKey(key), true, false);
				while (cursor.hasNext()) {
					Entry<Object, OIdentifiable> entry = cursor.nextEntry();
					if (entry != null) {
						OCompositeKey entryKey = (OCompositeKey) entry.getKey();
						// The index returns all entries. We thus need to filter it manually
						if (entryKey.getKeys().get(1).equals(key)) {
							Object inId = entryKey.getKeys().get(0);
							// Only add the inbound vertex id to the list of ids
							ids.add(inId);
						}
					} else {
						break;
					}
				}
			}
		}
		return ids;
	}

	@Override
	public void removeVertexIndex(String indexName, Class<? extends VertexFrame> clazz) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex index for class {" + clazz.getName() + "}");
		}
		try (OrientGraph noTx = db.get().getTxProvider().rawNoTx()) {
			String name = clazz.getSimpleName();
			final OClass v = noTx.getRawDatabase().getMetadata().getSchema().getClass(name);
			if (v == null) {
				throw new RuntimeException("Vertex type {" + name + "} is unknown. Can't remove index {" + indexName + "}");
			}
			OIndex index = v.getClassIndex(indexName);
			if (index != null) {
				noTx.getRawDatabase().getMetadata().getIndexManager().dropIndex(index.getName());
			}
		}
	}

	@Override
	public void removeIndex(String indexName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing index {" + indexName + "}");
		}
		try (OrientGraph noTx = db.get().getTxProvider().rawNoTx()) {
			noTx.getRawDatabase().getMetadata().getIndexManager().dropIndex(indexName);
		}
	}

	@Override
	public <T extends ElementFrame> T checkIndexUniqueness(String indexName, T element, Object key) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		OrientGraph orientBaseGraph = db.get().unwrapCurrentGraph();

		final OClass elementType;
		if (element instanceof EdgeFrame) {
			String label = ((EdgeFrame) element).getLabel();
			elementType = orientBaseGraph.getRawDatabase().getMetadata().getSchema().getClass(label);
		} else {
			elementType = orientBaseGraph.getRawDatabase().getMetadata().getSchema().getClass(element.getClass().getSimpleName());
		}
		if (elementType != null) {
			OIndex index = elementType.getClassIndex(indexName);
			if (index != null) {
				Object recordId = index.get(key);
				if (recordId != null) {
					if (recordId.equals(element.getElement().id())) {
						return null;
					} else {
						if (element instanceof EdgeFrame) {
							Edge edge = orientBaseGraph.getRawDatabase().getRecord((OIdentifiable) recordId);
							if (edge == null) {
								if (log.isDebugEnabled()) {
									log.debug("Did not find element with id {" + recordId + "} in graph from index {" + indexName + "}");
								}
								return null;
							} else {
								return (T) graph.frameElementExplicit(edge, element.getClass());
							}
						} else {
							return (T) graph.getFramedVertexExplicit(element.getClass(), recordId);
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public <T extends ElementFrame> T checkIndexUniqueness(String indexName, Class<T> classOfT, Object key) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientGraph orientBaseGraph = ((OrientGraph) baseGraph);

		OClass vertexType = orientBaseGraph.getRawDatabase().getMetadata().getSchema().getClass(classOfT.getSimpleName());
		if (vertexType != null) {
			OIndex index = vertexType.getClassIndex(indexName);
			if (index != null) {
				Object recordId = index.get(key);
				if (recordId != null) {
					return (T) graph.getFramedVertexExplicit(classOfT, recordId);
				}
			}
		}
		return null;
	}

	@Override
	public void createIndex(ElementIndexDefinition def) {
		if (def instanceof VertexIndexDefinition) {
			VertexIndexDefinition vertexDef = (VertexIndexDefinition) def;
			addVertexIndex(vertexDef);
		} else if (def instanceof EdgeIndexDefinition) {
			EdgeIndexDefinition edgeDef = (EdgeIndexDefinition) def;
			addEdgeIndex(edgeDef);
		}
	}

	private void addEdgeIndex(EdgeIndexDefinition def) {

		String label = def.getName();
		FieldMap fields = def.getFields();
		String indexPostfix = def.getPostfix();
		boolean unique = def.isUnique();
		boolean includeIn = def.isIncludeIn();
		boolean includeOut = def.isIncludeOut();
		boolean includeInOut = def.isIncludeInOut();
		String[] extraFields = {};

		try (OrientGraph noTx = db.get().getTxProvider().rawNoTx()) {

			// 1. Type handling
			final OClass e = noTx.getRawDatabase().getMetadata().getSchema().getClass(label);
			if (e == null) {
				throw new RuntimeException("Could not find edge type {" + label + "}. Create edge type before creating indices.");
			}

			if (fields != null) {
				for (String key : fields.keySet()) {
					if (e.getProperty(key) == null) {
						FieldType type = fields.get(key);
						OType otype = toType(type);
						OType osubType = toSubType(type);
						if (osubType != null) {
							e.createProperty(key, otype, osubType);
						} else {
							e.createProperty(key, otype);
						}
					}
				}
			}

			if ((includeIn || includeInOut) && e.getProperty("in") == null) {
				e.createProperty("in", OType.LINK);
			}
			if ((includeOut || includeInOut) && e.getProperty("out") == null) {
				e.createProperty("out", OType.LINK);
			}
			for (String key : extraFields) {
				if (e.getProperty(key) == null) {
					e.createProperty(key, OType.STRING);
				}
			}

			// 2. Index handling - (in/out/extra)
			String indexName = "e." + label.toLowerCase();
			String name = indexName + "_inout";
			if (includeInOut && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE, new String[] { "in", "out" });
			}
			name = indexName + "_out";
			if (includeOut && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX, new String[] { "out" });
			}
			name = indexName + "_in";
			if (includeIn && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX, new String[] { "in" });
			}
			name = indexName + "_extra";
			if (extraFields.length != 0 && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, extraFields);
			}

			// 3. Index Handling - regular (with/without postfix)
			name = indexName;
			if (indexPostfix != null) {
				name += "_" + indexPostfix;
			}
			name = name.toLowerCase();
			if (fields != null && fields.size() != 0 && e.getClassIndex(name) == null) {
				String[] fieldArray = fields.keySet().stream().toArray(String[]::new);
				String indexType = unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString();
				OIndex idx = e.createIndex(name, indexType, null, new ODocument().fields("ignoreNullValues", true), fieldArray);
				if (idx == null) {
					new RuntimeException("Index for {" + label + "/" + indexPostfix + "} was not created.");
				}
			}

		}
	}

	private void addVertexIndex(VertexIndexDefinition def) {
		String name = def.getClazz().getSimpleName();
		String indexName = def.getName();
		FieldMap fields = def.getFields();
		IndexType type = def.getType();
		boolean unique = def.isUnique();

		if (!StringUtils.isEmpty(def.getPostfix())) {
			indexName = indexName + "_" + def.getPostfix();
		}

		if (log.isDebugEnabled()) {
			log.debug("Adding vertex index for class {" + name + "}");
		}

		try (OrientGraph noTx = db.get().getTxProvider().rawNoTx()) {
			final OClass v = noTx.getRawDatabase().getMetadata().getSchema().getClass(name);
			if (v == null) {
				throw new RuntimeException("Vertex type {" + name + "} is unknown. Can't create index {" + indexName + "}");
			}
			if (fields != null) {
				for (String key : fields.keySet()) {
					if (v.getProperty(key) == null) {
						FieldType fieldType = fields.get(key);
						OType oType = toType(fieldType);
						OType subType = toSubType(fieldType);
						if (subType != null) {
							v.createProperty(key, oType, subType);
						} else {
							v.createProperty(key, oType);
						}
					}
				}
			}
			String typeName = unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString();

			// Override if requested
			if (type != null) {
				typeName = type.toString();
			}
			if (fields != null && fields.size() != 0 && v.getClassIndex(indexName) == null) {
				String[] fieldArray = fields.keySet().stream().toArray(String[]::new);
				v.createIndex(indexName, typeName,
					null, new ODocument().fields("ignoreNullValues", true), fieldArray);
			}
		}
	}

	@Override
	public <T extends VertexFrame> T findByUuid(Class<? extends T> classOfT, String uuid) {
		Class<?> foundImpl = persistenceClassMap.get(classOfT);
		// Use the found impl when one was found.
		if (foundImpl != null) {
			classOfT = (Class<? extends T>) foundImpl;
		}

		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientGraph orientBaseGraph = ((OrientGraph) baseGraph);
		String type = "MeshVertexImpl";

		OClass vertexType = orientBaseGraph.getRawDatabase().getMetadata().getSchema().getClass(type);
		if (vertexType != null) {
			OIndex index = vertexType.getClassIndex(type);
			if (index != null) {
				Object recordId = index.get(uuid);
				if (recordId != null) {
					return (T) graph.getFramedVertexExplicit(classOfT, recordId);
				}
			}
		}
		return null;
	}
}
