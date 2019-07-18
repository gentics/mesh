package com.gentics.mesh.madl.frame;

public interface ElementFrame extends com.syncleus.ferma.ElementFrame {
	/**
	 * Return the id of the element.
	 *
	 * @return The id of this element.
	 */
	default Object id() {
		return getId();
	}
}
