package com.gentics.mesh.graphdb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.ferma.AbstractDelegatingFramedOrientGraph;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.TrxHandler;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB specific mesh graph database implementation.
 */
public class OrientDBDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private OrientGraphFactory factory;
	private DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS");
	private int maxRetry = 25;

	@Override
	public void stop() {
		factory.close();
		Orient.instance().shutdown();
		Database.setThreadLocalGraph(null);
	}

	@Override
	public void init(GraphStorageOptions options, Vertx vertx) throws Exception {
		super.init(options, vertx);
		if (options != null && options.getParameters() != null && options.getParameters().get("maxTransactionRetry") != null) {
			this.maxRetry = options.getParameters().get("maxTransactionRetry").getAsInt();
			log.info("Using {" + this.maxRetry + "} transaction retries before failing");
		}
	}

	// Code is broken and cause NPE and stuck tests:
	// https://github.com/orientechnologies/orientdb/issues/6336
	// @Override
	// public void clear() {
	// OrientGraphNoTx noTx = factory.getNoTx();
	// try {
	// noTx.drop();
	// } finally {
	// noTx.shutdown();
	// }
	// }

	@Override
	public TransactionalGraph rawTx() {
		return factory.getTx();
	}

	@Override
	public void start() throws Exception {
		Orient.instance().startup();
		if (options == null || options.getDirectory() == null) {
			log.info("No graph database settings found. Fallback to in memory mode.");
			factory = new OrientGraphFactory("memory:tinkerpop").setupPool(5, 100);
		} else {
			factory = new OrientGraphFactory("plocal:" + options.getDirectory()).setupPool(5, 100);
		}
		if (options != null && options.getStartServer()) {
			startOrientServer();
		}
		configureGraphDB();
	}

	private InputStream getOrientServerConfig() throws IOException {
		InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		configString = configString.replaceAll("%PLUGIN_DIRECTORY%", "orient-plugins");
		configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "finest");
		configString = configString.replaceAll("%FILE_LOG_LEVEL%", "fine");
		String safePath = StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(options.getDirectory()).getAbsolutePath()));
		configString = configString.replaceAll("%MESH_DB_PATH%", "plocal:" + safePath);
		if (log.isDebugEnabled()) {
			log.debug("Effective orientdb server configuration:" + configString);
		}

		String safeParentDirPath = StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(options.getDirectory()).getParentFile().getAbsolutePath()));
		configString = configString.replaceAll("%MESH_DB_PARENT_PATH%", safeParentDirPath);
		System.out.println(configString);
		InputStream stream = new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8));
		return stream;
	}

	private void startOrientServer() throws Exception {
		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		System.out.println(orientdbHome);
		OServer server = OServerMain.create();
		log.info("Extracting OrientDB Studio");
		InputStream ins = getClass().getResourceAsStream("/plugins/studio-2.2.zip");
		File pluginDirectory = new File("orient-plugins");
		pluginDirectory.mkdirs();
		IOUtils.copy(ins, new FileOutputStream(new File(pluginDirectory, "studio-2.2.zip")));

		server.startup(getOrientServerConfig());
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();
		manager.startup();
	}

	private void configureGraphDB() {
		log.info("Configuring orientdb...");
		OrientGraphNoTx tx = factory.getNoTx();
		try {
			tx.setUseLightweightEdges(false);
			tx.setUseVertexFieldsForEdgeLabels(false);
		} finally {
			tx.shutdown();
		}
	}

	@Override
	public void addEdgeIndex(String label, String... extraFields) {
		OrientGraphNoTx tx = factory.getNoTx();
		try {
			OrientEdgeType e = tx.getEdgeType(label);
			if (e == null) {
				e = tx.createEdgeType(label);
			}
			if (e.getProperty("in") == null) {
				e.createProperty("in", OType.LINK);
			}
			if (e.getProperty("out") == null) {
				e.createProperty("out", OType.LINK);
			}
			for (String key : extraFields) {
				if (e.getProperty(key) == null) {
					e.createProperty(key, OType.STRING);
				}
			}
			String indexName = "e." + label.toLowerCase();
			List<String> fields = new ArrayList<>(Arrays.asList("out", "in"));
			fields.addAll(Arrays.asList(extraFields));

			if (e.getClassIndex(indexName) == null) {
				e.createIndex(indexName, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, fields.toArray(new String[fields.size()]));
			}
		} finally {
			tx.shutdown();
		}
	}

	@Override
	public Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		FramedGraph graph = Database.getThreadLocalGraph();
		Graph baseGraph = ((AbstractDelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph orientBaseGraph = ((OrientBaseGraph) baseGraph);
		return orientBaseGraph.getVertices(classOfVertex.getSimpleName(), fieldNames, fieldValues).iterator();
	}

	/**
	 * Unwrap the current thread local graph.
	 * 
	 * @return
	 */
	private OrientBaseGraph unwrapCurrentGraph() {
		FramedGraph graph = Database.getThreadLocalGraph();
		Graph baseGraph = ((AbstractDelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph tx = ((OrientBaseGraph) baseGraph);
		return tx;
	}

	@Override
	public void addEdgeType(String label, String... stringPropertyKeys) {
		if (log.isDebugEnabled()) {
			log.debug("Adding edge type for label {" + label + "}");
		}
		OrientBaseGraph tx = unwrapCurrentGraph();

		OrientEdgeType e = tx.getEdgeType(label);
		if (e == null) {
			e = tx.createEdgeType(label);
		}
		for (String key : stringPropertyKeys) {
			if (e.getProperty(key) == null) {
				e.createProperty(key, OType.STRING);
			}
		}
	}

	@Override
	public void addVertexType(Class<?> clazzOfVertex, Class<?> superClazzOfVertex) {
		if (log.isDebugEnabled()) {
			log.debug("Adding vertex type for class {" + clazzOfVertex.getName() + "}");
		}
		OrientGraphNoTx tx = factory.getNoTx();
		try {
			OrientVertexType e = tx.getVertexType(clazzOfVertex.getSimpleName());
			if (e == null) {
				String superClazz = "V";
				if (superClazzOfVertex != null) {
					superClazz = superClazzOfVertex.getSimpleName();
				}
				e = tx.createVertexType(clazzOfVertex.getSimpleName(), superClazz);
			} else {
				if (superClazzOfVertex != null) {
					OrientVertexType superType = tx.getVertexType(superClazzOfVertex.getSimpleName());
					if (superType == null) {
						throw new RuntimeException("The supertype for vertices of type {" + clazzOfVertex + "} can't be set since the supertype {"
								+ superClazzOfVertex.getSimpleName() + "} was not yet added to orientdb.");
					}
					e.setSuperClass(superType);
				}
			}
		} finally {
			tx.shutdown();
		}
	}

	@Override
	public void addEdgeIndexSource(String label) {
		if (log.isDebugEnabled()) {
			log.debug("Adding source edge index for label {" + label + "}");
		}
		OrientGraphNoTx tx = factory.getNoTx();
		try {
			OrientEdgeType e = tx.getEdgeType(label);
			if (e == null) {
				e = tx.createEdgeType(label);
			}
			if (e.getProperty("out") == null) {
				e.createProperty("out", OType.LINK);
			}
			String indexName = "e." + label.toLowerCase();
			String[] fields = { "out" };
			if (e.getClassIndex(indexName) == null) {
				e.createIndex(indexName, OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX, fields);
			}
		} finally {
			tx.shutdown();
		}
	}

	@Override
	public void addVertexIndex(String indexName, Class<?> clazzOfVertices, boolean unique, String... fields) {
		if (log.isDebugEnabled()) {
			log.debug("Adding vertex index  for class {" + clazzOfVertices.getName() + "}");
		}
		OrientGraphNoTx tx = factory.getNoTx();
		try {
			String name = clazzOfVertices.getSimpleName();
			OrientVertexType v = tx.getVertexType(name);
			if (v == null) {
				throw new RuntimeException("Vertex type {" + name + "} is unknown. Can't create index {" + indexName + "}");
			}
			for (String field : fields) {
				if (v.getProperty(field) == null) {
					v.createProperty(field, OType.STRING);
				}
			}
			if (v.getClassIndex(indexName) == null) {
				v.createIndex(indexName, unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(),
						null, new ODocument().fields("ignoreNullValues", true), fields);
			}
		} finally {
			tx.shutdown();
		}

	}

	@Override
	public <T extends MeshElement> T checkIndexUniqueness(String indexName, T element, Object key) {
		FramedGraph graph = Database.getThreadLocalGraph();
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();

		OrientVertexType vertexType = orientBaseGraph.getVertexType(element.getClass().getSimpleName());
		if (vertexType != null) {
			OIndex<?> index = vertexType.getClassIndex(indexName);
			if (index != null) {
				Object recordId = index.get(key);
				if (recordId != null) {
					if (recordId.equals(element.getElement().getId())) {
						return null;
					} else {
						return (T) graph.getFramedVertexExplicit(element.getClass(), recordId);
					}
				}
			}
		}
		return null;
	}

	@Override
	public <T extends MeshElement> T checkIndexUniqueness(String indexName, Class<T> classOfT, Object key) {
		FramedGraph graph = Database.getThreadLocalGraph();
		Graph baseGraph = ((AbstractDelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph orientBaseGraph = ((OrientBaseGraph) baseGraph);

		OrientVertexType vertexType = orientBaseGraph.getVertexType(classOfT.getSimpleName());
		if (vertexType != null) {
			OIndex<?> index = vertexType.getClassIndex(indexName);
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
	public void reload(MeshElement element) {
		((OrientVertex) element.getElement()).reload();
	}

	@Override
	public Trx trx() {
		return new OrientDBTrx(factory);
	}

	@Override
	public <T> T trx(TrxHandler<T> txHandler) {
		/**
		 * OrientDB uses the MVCC pattern which requires a retry of the code that manipulates the graph in cases where for example an
		 * {@link OConcurrentModificationException} is thrown.
		 */
		T handlerResult = null;
		boolean handlerFinished = false;
		for (int retry = 0; retry < maxRetry; retry++) {

			try (Trx tx = trx()) {
				handlerResult = txHandler.call();
				handlerFinished = true;
				tx.success();
			} catch (OSchemaException e) {
				log.error("OrientDB schema exception detected.");
				// TODO maybe we should invoke a metadata getschema reload?
				// factory.getTx().getRawGraph().getMetadata().getSchema().reload();
				// Database.getThreadLocalGraph().getMetadata().getSchema().reload();
			} catch (OConcurrentModificationException e) {
				if (log.isTraceEnabled()) {
					log.trace("Error while handling transaction. Retrying " + retry, e);
				}
				// Reset previous result
				handlerFinished = false;
				handlerResult = null;
			} catch (RuntimeException e) {
				log.error("Error handling transaction", e);
				throw e;
			} catch (Exception e) {
				log.error("Error handling transaction", e);
				throw new RuntimeException("Transaction error", e);
			}
			if (log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
			}
			if (handlerFinished) {
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit for trx exceeded");
	}

	@Override
	public NoTrx noTrx() {
		return new OrientDBNoTrx(factory);
	}

	@Override
	public void backupGraph(String backupDirectory) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			String dateString = formatter.format(new Date());
			String backupFile = "backup_" + dateString + ".zip";
			OutputStream out = new FileOutputStream(new File(backupDirectory, backupFile).getAbsolutePath());
			db.backup(out, null, null, listener, 9, 2048);
		} finally {
			db.close();
		}
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			InputStream in = new FileInputStream(backupFile);
			db.restore(in, null, null, listener);
		} finally {
			db.close();
		}

	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};

			String dateString = formatter.format(new Date());
			String exportFile = "export_" + dateString;

			ODatabaseExport export = new ODatabaseExport(db, new File(outputDirectory, exportFile).getAbsolutePath(), listener);
			export.exportDatabase();
			export.close();
		} finally {
			db.close();
		}
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			ODatabaseImport databaseImport = new ODatabaseImport(db, importFile, listener);
			databaseImport.importDatabase();
			databaseImport.close();
		} finally {
			db.close();
		}

	}

	@Override
	public Object createComposedIndexKey(Object... keys) {
		return new OCompositeKey(keys);
	}

	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex) element).getBaseElement();
		}
		((OrientVertex) element).moveToClass(classOfVertex.getSimpleName());
	}
}
