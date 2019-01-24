package com.gentics.mesh.core.endpoint.admin.consistency;

import java.util.Iterator;
import java.util.function.BiConsumer;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractConsistencyCheck implements ConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(AbstractConsistencyCheck.class);
	private static final long BATCH_SIZE = 4000L;

	@Override
	public long getBatchSize() {
		return BATCH_SIZE;
	}

	/**
	 * Loads the elements of the given type from the graph and processes them using the given action.
	 * 
	 * @param db
	 *            Database reference
	 * @param clazz
	 *            Type of elements to be loaded and processed
	 * @param action
	 *            Processing action to be invoked
	 * @param attemptRepair
	 *            Handle repair
	 * @param tx
	 *            Current transaction
	 */
	protected <T extends MeshVertex> ConsistencyCheckResult processForType(Database db, Class<T> clazz, BiConsumer<T, ConsistencyCheckResult> action,
		boolean attemptRepair, Tx tx) {
		log.info("Processing elements of type {" + clazz.getSimpleName() + "}");
		Iterator<? extends T> it = db.getVerticesForType(clazz);
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		long count = 0;
		while (it.hasNext()) {
			T element = it.next();
			action.accept(element, result);
			if (count != 0 && count % getBatchSize() == 0) {
				if (attemptRepair) {
					tx.getGraph().commit();
				}
				log.info("Processed {" + count + "} " + clazz.getSimpleName() + " elements.");
			}
			count++;
		}
		if (attemptRepair) {
			tx.getGraph().commit();
		}
		log.info("Processed a total of {" + count + "} " + clazz.getSimpleName() + " elements.");
		return result;
	}

}
