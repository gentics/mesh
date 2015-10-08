package com.gentics.mesh.graphdb.spi;

@FunctionalInterface
public interface TrxHandler<E> {

	/**
	 * Something has happened, so handle it.
	 *
	 * @param event
	 *            the event to handle
	 */
	void handle(E event) throws Exception;
}
