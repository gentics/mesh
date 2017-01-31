package com.gentics.mesh.core.data;

import com.gentics.mesh.graphdb.model.MeshElement;

/**
 * An {@link IndexableElement} is an a vertex which can be added to the search
 * index.
 *
 */
public interface IndexableElement extends MeshElement {

	/**
	 * This method will iterate over all related elements and invoke the given
	 * action for each element.
	 * 
	 * @param action
	 *            Action to be called for each related element
	 */
	default void handleRelatedEntries(HandleElementAction action) {
		// By default no related elements are handled
	}

	/**
	 * Return the type of the indexable element.
	 * 
	 * @return
	 */
	String getType();

}
