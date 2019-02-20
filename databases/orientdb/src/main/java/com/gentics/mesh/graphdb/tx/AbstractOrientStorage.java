package com.gentics.mesh.graphdb.tx;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.gentics.mesh.etc.config.MeshOptions;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.intent.OIntentNoCache;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractOrientStorage implements OrientStorage {

	private static final Logger log = LoggerFactory.getLogger(AbstractOrientStorage.class);

	protected DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS");

	protected MeshOptions options;

	public AbstractOrientStorage(MeshOptions options) {
		this.options = options;
	}

	public MeshOptions getOptions() {
		return options;
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}

		// OrientGraph tx = rawTx();
		// tx.declareIntent(new OIntentNoCache());
		// for (Edge edge : tx.getEdges()) {
		// edge.remove();
		// }
		// for (Vertex vertex : tx.getVertices()) {
		// vertex.remove();
		// }
		// tx.commit();
		// OIndexManager manager = tx.getRawGraph().getMetadata().getIndexManager();
		// manager.getIndexes().forEach(i -> i.rebuild());
		// tx.shutdown();
		OrientGraphNoTx tx2 = rawNoTx();
		try {
			for (Vertex vertex : tx2.getVertices()) {
				vertex.remove();
			}
		} finally {
			tx2.declareIntent(null);
			tx2.shutdown();
		}
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}
	}

}
