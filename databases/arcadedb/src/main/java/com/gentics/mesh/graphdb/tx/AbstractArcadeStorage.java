package com.gentics.mesh.graphdb.tx;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.metric.SimpleMetric.NO_TX;
import static com.gentics.mesh.metric.SimpleMetric.TX;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.arcadedb.database.Database;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.integration.backup.Backup;
import com.arcadedb.integration.restore.Restore;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.StreamUtil;

import io.micrometer.core.instrument.Counter;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation of an {@link ArcadeStorage}. Backup/Restore and clear code are for example shareable across specific implementations.
 */
public abstract class AbstractArcadeStorage implements ArcadeStorage {

	private static final Logger log = LoggerFactory.getLogger(AbstractArcadeStorage.class);

	protected DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS");

	protected final MetricsService metrics;

	protected final GraphDBMeshOptions options;

	protected final Counter txCounter;

	protected final Counter noTxCounter;

	public AbstractArcadeStorage(GraphDBMeshOptions options, MetricsService metrics) {
		this.options = options;
		this.metrics = metrics;
		this.txCounter = metrics.counter(TX);
		this.noTxCounter = metrics.counter(NO_TX);
	}

	public GraphDBMeshOptions getOptions() {
		return options;
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}
		ArcadeGraph tx = rawTx();
		try {
			for (Vertex vertex : StreamUtil.toIterable(tx.vertices())) {
				vertex.remove();
			}
		} finally {
			tx.tx().commit();
			tx.close();
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
		String dateString = formatter.format(new Date());
		String backupFile = "backup_" + dateString + ".zip";
		new File(backupDirectory).mkdirs();
		String absolutePath = new File(backupDirectory, backupFile).getAbsolutePath();
		try (Database db = createSession()) {
			Backup backup = new Backup(db, absolutePath);
			backup.setVerboseLevel(computeVerboseLevel());
			backup.backupDatabase();
			return absolutePath;
		}
	}

	@Override
	public void restore(String backupFile) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running restore using {" + backupFile + "} backup file.");
		}
		String absolutePath = new File(options.getStorageOptions().getDirectory(), DB_NAME).getAbsolutePath();
		Restore restore = new Restore(backupFile, absolutePath);
		restore.setVerboseLevel(computeVerboseLevel());
		restore.restoreDatabase();
	}

	protected int computeVerboseLevel() {
		return (int) Arrays.asList(log.isTraceEnabled(), log.isDebugEnabled(), log.isInfoEnabled(), log.isWarnEnabled()).stream().filter(Boolean::booleanValue).count();
	}

	/**
	 * Create a new session to access the database. Remember to close the session after usage.
	 * 
	 * @return
	 */
	public abstract Database createSession();
}
