package com.gentics.mesh.search.verticle.bulk;

import io.vertx.core.Vertx;

/**
 * A debounce timer for the {@link BulkOperator}.
 */
class BulkTimer {
	private final Vertx vertx;
	private final long bulkTime;
	private final Runnable action;
	private Long currentTimer;

	public BulkTimer(Vertx vertx, long bulkTime, Runnable action) {
		this.vertx = vertx;
		this.bulkTime = bulkTime;
		this.action = action;
	}

	/**
	 * Stops the current timer and starts it anew.
	 */
	public void restart() {
		stop();
		currentTimer = vertx.setTimer(bulkTime, l -> action.run());
	}

	/**
	 * Stops the current timer if one is running.
	 */
	public void stop() {
		if (currentTimer != null) {
			vertx.cancelTimer(currentTimer);
			currentTimer = null;
		}
	}

	/**
	 * Checks if the timer is currently active.
	 * @return
	 */
	public boolean isRunning() {
		return currentTimer != null;
	}
}
