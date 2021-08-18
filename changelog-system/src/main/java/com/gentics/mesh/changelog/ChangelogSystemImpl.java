package com.gentics.mesh.changelog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.cli.PostProcessFlags;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.google.common.base.Objects;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central changelog system class which is responsible for handling listed changes.
 * 
 * A change must be added to the {@link ChangesList} in order to be handled by the {@link ChangelogSystemImpl}.
 */
@Singleton
public class ChangelogSystemImpl implements ChangelogSystem {

	private static final Logger log = LoggerFactory.getLogger(ChangelogSystemImpl.class);

	public static final String MESH_VERSION = "meshVersion";

	public static final String MESH_DB_REV = "meshDatabaseRevision";

	private Database db;

	private MeshOptions options;

	@Inject
	public ChangelogSystemImpl(Database db, MeshOptions options) {
		this.db = db;
		this.options = options;
	}

	@Override
	public boolean applyChanges(PostProcessFlags flags, List<Change> list) {
		for (Change change : list) {
			// Execute each change in a new transaction
			change.setDb(db);
			try {
				if (!change.isApplied()) {
					log.info("Handling change {" + change.getUuid() + "}");
					log.info("Name: " + change.getName());
					log.info("Description: " + change.getDescription());

					long start = System.currentTimeMillis();
					change.apply();
					change.setDuration(System.currentTimeMillis() - start);
					if (!change.validate()) {
						throw new Exception("Validation for change {" + change.getUuid() + "/" + change.getName() + "} failed.");
					}
					change.markAsComplete();
					if (flags != null && change.requiresReindex()) {
						flags.requireReindex();
					}
				} else {
					log.debug("Change {" + change.getUuid() + "} is already applied.");
				}
			} catch (Throwable e) {
				log.error("Error while handling change {" + change.getUuid() + "/" + change.getName() + "}.", e);
				return false;
			}
		}
		return true;
	}

	@Override
	public void markAllAsApplied(List<Change> list) {
		TransactionalGraph graph = db.rawTx();
		try {
			for (Change change : list) {
				change.setGraph(graph);
				change.setDb(db);
				change.markAsComplete();
				log.info("Marking change {" + change.getUuid() + "/" + change.getName() + "} as completed.");
			}
		} finally {
			graph.shutdown();
		}
	}

	@Override
	public boolean applyChanges(PostProcessFlags flags) {
		return applyChanges(flags, ChangesList.getList(options));
	}

	@Override
	public void markAllAsApplied() {
		markAllAsApplied(ChangesList.getList(options));
		setCurrentVersionAndRev();
	}

	@Override
	public void setCurrentVersionAndRev() {
		log.info("Updating stored database revision and mesh version.");
		// Version is okay. So lets store the version and the updated revision.
		String currentVersion = Mesh.getPlainVersion();
		TransactionalGraph graph = db.rawTx();
		try {
			Vertex root = MeshGraphHelper.getMeshRootVertex(graph);
			String rev = db.getDatabaseRevision();
			if (!Objects.equal(root.getProperty(MESH_VERSION), currentVersion)) {
				root.setProperty(MESH_VERSION, currentVersion);
			}
			if (!Objects.equal(root.getProperty(MESH_DB_REV), rev)) {
				root.setProperty(MESH_DB_REV, rev);
			}
			graph.commit();
		} finally {
			graph.shutdown();
		}
	}

	@Override
	public boolean requiresChanges() {
		List<Change> changes = ChangesList.getList(options);
		TransactionalGraph graph = db.rawTx();
		try {
			for (Change change : changes) {
				change.setGraph(graph);
				change.setDb(db);
				if (!change.isApplied()) {
					log.info("Change " + change.getName() + " has not yet been applied.");
					return true;
				}
			}
		} finally {
			graph.shutdown();
		}
		return false;
	}
}
