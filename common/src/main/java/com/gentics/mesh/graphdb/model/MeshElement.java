package com.gentics.mesh.graphdb.model;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.ElementFrame;
import com.tinkerpop.blueprints.Element;

import io.vertx.core.Vertx;

/**
 * Basic interface for graph elements.
 */
public interface MeshElement extends ElementFrame {

	/**
	 * Set the uuid of the element.
	 * 
	 * @param uuid
	 *            Uuid of the element
	 */
	void setUuid(String uuid);

	/**
	 * Return the uuid of the element.
	 * 
	 * @return Uuid of the element
	 */
	String getUuid();

	/**
	 * Return the underlying graph element.
	 * 
	 * @return Graph element
	 */
	Element getElement();

	/**
	 * Return the internal element version.
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Provide TP 3.x compliant method.
	 * 
	 * @param name
	 * @return
	 */
	default <T> T property(String name) {
		return getProperty(name);
	}

	/**
	 * Set the property.
	 * 
	 * @param key
	 * @param value
	 */
	default <R> void property(String key, R value) {
		setProperty(key, value);
	}

	/**
	 * Remove the property with the given key.
	 * 
	 * @param key
	 */
	default void removeProperty(String key) {
		setProperty(key, null);
	}

	/**
	 * Return the db reference.
	 * 
	 * @return
	 */
	Database db();

	/**
	 * Return the used Vert.x instance.
	 * 
	 * @return
	 */
	Vertx vertx();
}
