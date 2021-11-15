package com.gentics.mesh.core.endpoint.admin.consistency.check;

import java.util.Iterator;
import java.util.function.BiConsumer;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface CommonConsistencyCheck extends ConsistencyCheck {

	static final Logger log = LoggerFactory.getLogger(CommonConsistencyCheck.class);
	static final long BATCH_SIZE = 10000L;

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
	default <T extends HibElement> ConsistencyCheckResult processForType(Database db, Class<T> clazz, BiConsumer<T, ConsistencyCheckResult> action,
		boolean attemptRepair, Tx tx) {
		log.info("Processing elements of type {" + clazz.getSimpleName() + "}");
		Iterator<? extends T> it = db.getElementsForType(clazz);
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		long count = 0;
		while (it.hasNext()) {
			T element = it.next();
			action.accept(element, result);
			if (count != 0 && count % BATCH_SIZE == 0) {
				if (attemptRepair) {
					tx.commit();
				}
				log.info("Processed {" + count + "} " + clazz.getSimpleName() + " elements.");
			}
			count++;
		}
		if (attemptRepair) {
			tx.commit();
		}
		log.info("Processed a total of {" + count + "} " + clazz.getSimpleName() + " elements.");
		return result;
	}
}
