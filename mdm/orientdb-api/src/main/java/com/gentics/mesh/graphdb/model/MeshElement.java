package com.gentics.mesh.graphdb.model;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.syncleus.ferma.ElementFrame;
import com.tinkerpop.blueprints.Element;

import io.vertx.core.Vertx;

/**
 * Basic interface for graph elements.
 */
public interface MeshElement extends ElementFrame, HibElement {

	/**
	 * Return the underlying graph element.
	 * 
	 * @return Graph element
	 */
	Element getElement();

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
	 * Store the string value as {@link Set} property value.
	 * 
	 * @param propertyKey
	 * @param value
	 */
	default void addToStringSetProperty(String propertyKey, String value) {
		Set<String> set = property(propertyKey);
		if (set == null) {
			set = Collections.singleton(value);
		} else {
			set.add(value);
		}
		property(propertyKey, set);
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
	GraphDatabase db();

	/**
	 * Return the used Vert.x instance.
	 *
	 * @return
	 */
	Vertx vertx();

	/**
	 * Return the mesh options.
	 *
	 * @return
	 */
	MeshOptions options();
}
