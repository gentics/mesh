package com.gentics.mesh.core.db;

import java.util.Optional;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.dagger.BaseMeshComponent;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A developer extension API for {@link TxData}.
 * 
 * @author plyhun
 *
 */
public interface CommonTxData extends TxData {

	/**
	 * Root Mesh component access.
	 * 
	 * @return
	 */
	BaseMeshComponent mesh();

	/**
	 * Install a queue batch into the transaction.
	 * 
	 * @param batch
	 */
	void setEventQueueBatch(EventQueueBatch batch);

	/**
	 * Install a bulk action context into the transaction.
	 * 
	 * @param bac
	 */
	void setBulkActionContext(BulkActionContext bac);

	/**
	 * Get a queue batch, if previously set.
	 * 
	 * @return
	 */
	Optional<EventQueueBatch> maybeGetEventQueueBatch();

	/**
	 * Get a bulk action context, if previously set.
	 * 
	 * @return
	 */
	Optional<BulkActionContext> maybeGetBulkActionContext();

	/**
	 * Check if Vert.x is settled
	 * 
	 * @return
	 */
	boolean isVertxReady();

	/**
	 * Remove the queue batch from the transaction, if there was any.
	 * 
	 * @param batch
	 */
	default void suppressEventQueueBatch() {
		maybeGetEventQueueBatch().ifPresent(EventQueueBatch::clear);
	}

	/**
	 * Get a queue batch, if previously set, either explicitly or within the bulk context. Otherwise create and set a new batch.
	 * 
	 * @return
	 */
	default EventQueueBatch getOrCreateEventQueueBatch() {
		return maybeGetEventQueueBatch().orElseGet(() -> maybeGetBulkActionContext().map(BulkActionContext::batch).orElseGet(() -> {
				EventQueueBatch b = mesh().batchProvider().get();
				setEventQueueBatch(b);
				return b;
			}));
	}

	/**
	 * Get a bulk action context, if previously set. Otherwise create and set a new one. If a queue batch is already set, keep it inside the bulk ctx.
	 * 
	 * @return
	 */
	default BulkActionContext getOrCreateBulkActionContext() {
		return maybeGetBulkActionContext().orElseGet(() -> {
			BulkActionContext bac = mesh().bulkProvider().get();
			maybeGetEventQueueBatch().ifPresent(batch -> {
				bac.setBatch(batch);
				setEventQueueBatch(null);
			});
			setBulkActionContext(bac);
			return bac;
		});
	}
}
