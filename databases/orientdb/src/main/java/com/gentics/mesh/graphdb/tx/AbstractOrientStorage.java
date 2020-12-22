package com.gentics.mesh.graphdb.tx;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.metric.SimpleMetric.NO_TX;
import static com.gentics.mesh.metric.SimpleMetric.TX;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import io.micrometer.core.instrument.Counter;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation of an {@link OrientStorage}. Backup/Restore and clear code are for example shareable across specific implementations.
 */
public abstract class AbstractOrientStorage implements OrientStorage {

	private static final Logger log = LoggerFactory.getLogger(AbstractOrientStorage.class);

	protected DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS");

	protected final MetricsService metrics;

	protected final MeshOptions options;

	protected final Counter txCounter;

	protected final Counter noTxCounter;

	public AbstractOrientStorage(MeshOptions options, MetricsService metrics) {
		this.options = options;
		this.metrics = metrics;
		this.txCounter = metrics.counter(TX);
		this.noTxCounter = metrics.counter(NO_TX);
	}

	public MeshOptions getOptions() {
		return options;
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}
		OrientGraph tx = rawTx();
		try {
			for (Vertex vertex : tx.getVertices()) {
				vertex.remove();
			}
		} finally {
			tx.commit();
			tx.shutdown();
		}
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}
	}

	@Override
	public String backup(String backupDirectory) throws FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running backup to backup directory {" + backupDirectory + "}.");
		}
		boolean isMemoryMode = options.getStorageOptions().getDirectory() == null;
		if (isMemoryMode) {
			throw error(SERVICE_UNAVAILABLE, "backup_error_not_supported_in_memory_mode");
		}
		ODatabaseSession db = createSession();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			String dateString = formatter.format(new Date());
			String backupFile = "backup_" + dateString + ".zip";
			new File(backupDirectory).mkdirs();
			String absolutePath = new File(backupDirectory, backupFile).getAbsolutePath();
			try (OutputStream out = new FileOutputStream(absolutePath)) {
				db.backup(out, null, null, listener, 1, 2048);
			}
			return absolutePath;
		} finally {
			db.close();
		}
	}

	@Override
	public void restore(String backupFile) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running restore using {" + backupFile + "} backup file.");
		}
		log.debug("Opening database {}", DB_NAME);
		ODatabaseSession db = createSession();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			try (InputStream in = new FileInputStream(backupFile)) {
				db.restore(in, null, null, listener);
			}
		} finally {
			db.close();
		}
	}

	/**
	 * Create a new session to access the database. Remember to close the session after usage.
	 * 
	 * @return
	 */
	public abstract ODatabaseSession createSession();

}
