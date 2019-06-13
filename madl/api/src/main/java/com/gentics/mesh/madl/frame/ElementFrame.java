package com.gentics.mesh.madl.frame;

public interface ElementFrame extends com.syncleus.ferma.ElementFrame {
	/**
	 * Return the id of the element.
	 *
	 * @param <N>
	 *            The ID's type.
	 * @return The id of this element.
	 */
	default <N> N id() {
		return getId();
	}
}
