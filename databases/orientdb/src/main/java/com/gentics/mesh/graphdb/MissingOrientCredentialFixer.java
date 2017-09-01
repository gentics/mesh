package com.gentics.mesh.graphdb;

import java.io.File;

import com.gentics.mesh.etc.config.MeshOptions;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Fixes some graph databases which were created using OSecurityNull.
 */
public final class MissingOrientCredentialFixer {

	private static final Logger log = LoggerFactory.getLogger(MissingOrientCredentialFixer.class);

	public static void fix(MeshOptions options) {
		File graphDir = new File(options.getStorageOptions().getDirectory(), "storage");
		if (graphDir.exists()) {
			log.info("Checking database {" + graphDir + "}");
			OrientGraphFactory factory = new OrientGraphFactory("plocal:" + graphDir.getAbsolutePath()).setupPool(5, 100);
			try {
				// Enable the patched security class
				factory.setProperty(ODatabase.OPTIONS.SECURITY.toString(), OSecuritySharedPatched.class);
				ODatabaseDocumentTx tx = factory.getDatabase();
				try {
					OClass userClass = tx.getMetadata().getSchema().getClass("OUser");
					if (userClass == null) {
						log.info("OrientDB user credentials not found. Recreating needed roles and users.");
						tx.getMetadata().getSecurity().create();
						tx.commit();
					} else {
						log.info("OrientDB user credentials found. Skipping fix.");
					}
				} finally {
					tx.close();
				}
			} finally {
				factory.close();
			}
		}
	}
}
