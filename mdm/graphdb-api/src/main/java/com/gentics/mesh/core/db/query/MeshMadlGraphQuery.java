package com.gentics.mesh.core.db.query;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.madl.query.MadlGraphQuery;

/**
 * Mesh MADL query interface.
 * 
 * @param <T>
 * @param <P>
 */
public interface MeshMadlGraphQuery<T extends Element, P> extends MadlGraphQuery {

	MeshMadlGraphQuery<T, P> filter(Optional<String> maybeCustomFilter);

	/**
	 * A shortcut method for multiple {@link AbstractMadlGraphQuery#has(String, Object) calls}
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	MeshMadlGraphQuery<T, P> hasAll(String[] key, Object[] value);

	/**
	 * Set sorting relation's direction
	 * 
	 * @param relationDirection
	 * @return
	 */
	MeshMadlGraphQuery<T, P> relationDirection(Direction relationDirection);

	/**
	 * Fetch the results of this query.
	 * 
	 * @param propsAndDirs 
	 * @param extraParam
	 * @return
	 */
	Iterable<T> fetch(P extraParam);

	String[] getOrderPropsAndDirs();

	void setOrderPropsAndDirs(String[] orderPropsAndDirs);

}