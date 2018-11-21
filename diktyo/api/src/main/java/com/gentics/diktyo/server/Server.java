package com.gentics.diktyo.server;

public interface Server {

	/**
	 * Start the server.
	 * 
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * Stop the server.
	 */
	void stop();

	/**
	 * Restart the server.
	 * 
	 * @throws Exception
	 */
	default void restart() throws Exception {
		stop();
		// TODO add grace period
		start();
	}

}
