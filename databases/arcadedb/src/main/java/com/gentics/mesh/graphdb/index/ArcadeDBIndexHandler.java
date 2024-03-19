package com.gentics.mesh.graphdb.index;

import static com.gentics.mesh.graphdb.FieldTypeMapper.toSubType;
import static com.gentics.mesh.graphdb.FieldTypeMapper.toType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import com.arcadedb.database.Database;
import com.arcadedb.database.Identifiable;
import com.arcadedb.exception.SchemaException;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.index.Index;
import com.arcadedb.index.IndexCursor;
import com.arcadedb.index.IndexInternal;
import com.arcadedb.index.TypeIndex;
import com.arcadedb.index.lsm.LSMTreeIndexAbstract.NULL_STRATEGY;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.IndexBuilder;
import com.arcadedb.schema.Schema.INDEX_TYPE;
import com.arcadedb.schema.Type;
import com.arcadedb.schema.VectorIndexBuilder;
import com.gentics.madl.ext.arcadedb.DelegatingFramedArcadeGraph;
import com.gentics.madl.index.IndexHandler;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.PersistenceClassMap;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.ArcadeDBDatabase;
import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.index.EdgeIndexDefinition;
import com.gentics.mesh.madl.index.ElementIndexDefinition;
import com.gentics.mesh.madl.index.IndexType;
import com.gentics.mesh.madl.index.VertexIndexDefinition;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * ArcadeDB specific implementation for index handling.
 * 
 * @see IndexHandler
 */
@Singleton
public class ArcadeDBIndexHandler implements IndexHandler<String[]> {

	private static final Logger log = LoggerFactory.getLogger(ArcadeDBIndexHandler.class);

	private final Lazy<ArcadeDBDatabase> db;

	private final PersistenceClassMap persistenceClassMap;

	@Inject
	public ArcadeDBIndexHandler(Lazy<ArcadeDBDatabase> db, PersistenceClassMap persistenceClassMap) {
		this.db = db;
		this.persistenceClassMap = persistenceClassMap;
	}

	@Override
	public String[] createComposedIndexKey(Object... keys) {
		IntStream.range(0, keys.length).forEach(i -> {
			keys[i] = Objects.toString(keys[i]);
		});
		return (String[]) keys;
	}

	@Override
	public void reindex() {
		try (ArcadeGraph tx = db.get().getTxProvider().rawTx()) {
			for (Index i: tx.getDatabase().getSchema().getIndexes()) {
				IndexInternal ii = (IndexInternal) i;

				Type[] keyTypes = ii.getKeyTypes();
				String name = ii.getName();
				NULL_STRATEGY nullStrategy = ii.getNullStrategy();
				boolean unique = ii.isUnique();
				INDEX_TYPE type = ii.getType();
				int pageSize = ii.getPageSize();
				List<String> properties = ii.getPropertyNames();

				tx.getDatabase().getSchema().dropIndex(name);

				if (properties != null && !properties.isEmpty()) {
					i = tx.getDatabase().getSchema().buildTypeIndex(name, properties.toArray(new String[properties.size()])).withType(type).withUnique(unique).withNullStrategy(nullStrategy).withPageSize(pageSize).create();
				} else {
					i = tx.getDatabase().getSchema().buildManualIndex(name, keyTypes).withType(type).withUnique(unique).withNullStrategy(nullStrategy).withPageSize(pageSize).create();
				}
			}
		}
	}

	@Override
	public void addCustomEdgeIndex(String label, String indexPostfix, FieldMap fields, boolean unique) {
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			final DocumentType e = noTx.getSchema().getType(label);
		    if (e == null) {
				throw new RuntimeException("Could not find edge type {" + label + "}. Create edge type before creating indices.");
			}
			for (String key : fields.keySet()) {
				if (e.getPropertyIfExists(key) == null) {
					FieldType type = fields.get(key);
					Type otype = toType(type);
					Type osubType = toSubType(type);
					if (osubType != null) {
						e.createProperty(key, otype, osubType.name());
					} else {
						e.createProperty(key, otype);
					}
				}
			}
			String name = "e." + label + "_" + indexPostfix;
			name = name.toLowerCase();
			if (fields.size() != 0) {
				String[] fieldArray = fields.keySet().stream().toArray(String[]::new);
				TypeIndex idx = noTx.getSchema().buildTypeIndex(label, fieldArray).withIndexName(name).withUnique(unique).withNullStrategy(NULL_STRATEGY.SKIP).create();
				if (idx == null) {
					new RuntimeException("Index for {" + label + "/" + indexPostfix + "} was not created.");
				}
			}

		}
	}

	@Override
	public List<Object> edgeLookup(String edgeLabel, String indexPostfix, String[] key) {
		ArcadeGraph arcadeBaseGraph = db.get().unwrapCurrentGraph();
		List<Object> ids = new ArrayList<>();

		// Load the edge type in order to access the indices of the edge
		final DocumentType edgeType = arcadeBaseGraph.getDatabase().getSchema().getType(edgeLabel);
		if (edgeType != null) {
			// Fetch the required index
			Index index = arcadeBaseGraph.getDatabase().getSchema().getIndexByName("e." + edgeLabel.toLowerCase() + "_" + indexPostfix);
			if (index != null) {
				// Iterate over the sb-tree index entries
				IndexCursor cursor = index.get(key);
				while (cursor.hasNext()) {
					Identifiable entry = cursor.next();
					ids.add(entry.getIdentity());
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
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			String name = clazz.getSimpleName();
			final DocumentType v = noTx.getSchema().getType(name);
			if (v == null) {
				throw new RuntimeException("Vertex type {" + name + "} is unknown. Can't remove index {" + indexName + "}");
			}
			Index index = noTx.getSchema().getIndexByName(indexName);
			if (index != null) {
				noTx.getSchema().dropIndex(index.getName());
			}
		}
	}

	@Override
	public void removeIndex(String indexName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing index {" + indexName + "}");
		}
		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			noTx.getSchema().dropIndex(indexName);
		}
	}

	@Override
	public <T extends ElementFrame> T checkIndexUniqueness(String indexName, T element, String[] key) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		ArcadeGraph arcadeBaseGraph = db.get().unwrapCurrentGraph();

		final DocumentType elementType;
		if (element instanceof EdgeFrame) {
			String label = ((EdgeFrame) element).getLabel();
			elementType = arcadeBaseGraph.getDatabase().getSchema().getType(label);
		} else {
			elementType = arcadeBaseGraph.getDatabase().getSchema().getType(element.getClass().getSimpleName());
		}
		if (elementType != null) {
			Index index = arcadeBaseGraph.getDatabase().getSchema().getIndexByName(indexName);
			if (index != null) {
				IndexCursor recordId = index.get(key);
				if (recordId != null) {
					if (recordId.equals(element.getElement().id())) {
						return null;
					} else {
						if (element instanceof EdgeFrame) {
							Edge edge = arcadeBaseGraph.getEdgeFromRecord(recordId.getRecord());
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
	public <T extends ElementFrame> T checkIndexUniqueness(String indexName, Class<T> classOfT, String[] key) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph baseGraph = ((DelegatingFramedArcadeGraph) graph).getBaseGraph();
		ArcadeGraph arcadeBaseGraph = ((ArcadeGraph) baseGraph);

		DocumentType vertexType = arcadeBaseGraph.getDatabase().getSchema().getType(classOfT.getSimpleName());
		if (vertexType != null) {
			Index index = arcadeBaseGraph.getDatabase().getSchema().getIndexByName(indexName);
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

		try (Database noTx = db.get().getTxProvider().rawNoTx()) {

			// 1. Type handling
			final DocumentType e = noTx.getSchema().getType(label);
			if (e == null) {
				throw new RuntimeException("Could not find edge type {" + label + "}. Create edge type before creating indices.");
			}

			if (fields != null) {
				for (String key : fields.keySet()) {
					if (e.getPropertyIfExists(key) == null) {
						FieldType type = fields.get(key);
						Type otype = toType(type);
						Type osubType = toSubType(type);
						if (osubType != null) {
							e.createProperty(key, otype, osubType.name());
						} else {
							e.createProperty(key, otype);
						}
					}
				}
			}

			if ((includeIn || includeInOut) && e.getPropertyIfExists("in") == null) {
				e.createProperty("in", Type.LINK);
			}
			if ((includeOut || includeInOut) && e.getPropertyIfExists("out") == null) {
				e.createProperty("out", Type.LINK);
			}
			for (String key : extraFields) {
				if (e.getPropertyIfExists(key) == null) {
					e.createProperty(key, Type.STRING);
				}
			}

			// 2. Index handling - (in/out/extra)
			String indexName = "e." + label.toLowerCase();
			String name = indexName + "_inout";
			if (includeInOut) {
				try {
					noTx.getSchema().getIndexByName(name);
				} catch (SchemaException ex) {
					noTx.getSchema().buildTypeIndex(label, new String[] { "in", "out" }).withIndexName(name).withType(INDEX_TYPE.HSNW).withUnique(false).create();
				}
			}
			name = indexName + "_out";
			if (includeOut) {
				try {
					noTx.getSchema().getIndexByName(name);
				} catch (SchemaException ex) {
					noTx.getSchema().buildTypeIndex(label, new String[] { "out" }).withIndexName(name).withType(INDEX_TYPE.HSNW).withUnique(false).create();
				}
			}
			name = indexName + "_in";
			if (includeIn) {
				try {
					noTx.getSchema().getIndexByName(name);
				} catch (SchemaException ex) {
					noTx.getSchema().buildTypeIndex(label, new String[] { "in" }).withIndexName(name).withType(INDEX_TYPE.HSNW).withUnique(false).create();
				}
			}
			name = indexName + "_extra";
			if (extraFields.length != 0) {
				try {
					noTx.getSchema().getIndexByName(name);
				} catch (SchemaException ex) {
					noTx.getSchema().buildTypeIndex(label, extraFields).withIndexName(name).withType(INDEX_TYPE.HSNW).withUnique(true).create();
				}
			}

			// 3. Index Handling - regular (with/without postfix)
			name = indexName;
			if (indexPostfix != null) {
				name += "_" + indexPostfix;
			}
			name = name.toLowerCase();
			if (fields != null && fields.size() != 0) {
				try {
					noTx.getSchema().getIndexByName(name);
				} catch (SchemaException ex) {
					String[] fieldArray = fields.keySet().stream().toArray(String[]::new);
					Index idx = noTx.getSchema().buildTypeIndex(label, fieldArray).withNullStrategy(NULL_STRATEGY.SKIP).withIndexName(name).withType(INDEX_TYPE.HSNW).withUnique(unique).create();
					if (idx == null) {
						new RuntimeException("Index for {" + label + "/" + indexPostfix + "} was not created.");
					}
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

		try (Database noTx = db.get().getTxProvider().rawNoTx()) {
			final DocumentType v = noTx.getSchema().getType(name);
			if (v == null) {
				throw new RuntimeException("Vertex type {" + name + "} is unknown. Can't create index {" + indexName + "}");
			}
			if (fields != null) {
				for (String key : fields.keySet()) {
					if (v.getPropertyIfExists(key) == null) {
						FieldType fieldType = fields.get(key);
						Type oType = toType(fieldType);
						Type subType = toSubType(fieldType);
						if (subType != null) {
							v.createProperty(key, oType, subType.name());
						} else {
							v.createProperty(key, oType);
						}
					}
				}
			}
			String typeName = (unique ? "" : "NOT") + "UNIQUE_HASH_INDEX";
			// Override if requested
			if (type != null) {
				typeName = type.toString();
			}
			INDEX_TYPE itype;
			switch (typeName) {
			case "FULLTEXT":
			case "DICTIONARY":
				itype = INDEX_TYPE.FULL_TEXT;
				break;
			case "UNIQUE_HASH_INDEX":
			case "NOTUNIQUE_HASH_INDEX":
			case "FULLTEXT_HASH_INDEX":
			case "DICTIONARY_HASH_INDEX":
				itype = INDEX_TYPE.HSNW;
				break;
			default:
				itype = INDEX_TYPE.LSM_TREE;
			}
			if (fields != null && fields.size() != 0) {
				try {
					noTx.getSchema().getIndexByName(indexName);
				} catch (SchemaException ex) {
					String[] fieldArray = fields.keySet().stream().toArray(String[]::new);
					IndexBuilder<?> builder;
					if (fields.size() == 1) {
						builder = noTx.getSchema().buildVectorIndex().withVertexType(name).withType(itype);
						if (MeshVertex.UUID_KEY.equals(fieldArray[0])) {
							builder = ((VectorIndexBuilder) builder).withIdProperty(fieldArray[0]);
						} else {
							builder = ((VectorIndexBuilder) builder).withVectorProperty(fieldArray[0], toType(fields.get(fieldArray[0])));
						}
					} else {
						builder = noTx.getSchema().buildTypeIndex(name, fieldArray).withType(INDEX_TYPE.LSM_TREE);
					}
					Index idx = builder.withNullStrategy(NULL_STRATEGY.SKIP).withIndexName(indexName).withUnique(unique).create();
					if (idx == null) {
						new RuntimeException("Index for {" + name + "} was not created.");
					}
				}
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
		Graph baseGraph = ((DelegatingFramedArcadeGraph) graph).getBaseGraph();
		ArcadeGraph arcadeBaseGraph = ((ArcadeGraph) baseGraph);
		String type = "MeshVertexImpl";

		DocumentType vertexType = arcadeBaseGraph.getDatabase().getSchema().getType(type);
		if (vertexType != null) {
			Index index = arcadeBaseGraph.getDatabase().getSchema().getIndexByName(type);
			if (index != null) {
				Object recordId = index.get(new String[] {uuid});
				if (recordId != null) {
					return (T) graph.getFramedVertexExplicit(classOfT, recordId);
				}
			}
		}
		return null;
	}
}
