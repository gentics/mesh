package com.gentics.mesh.madl.frame;

public interface EdgeFrame extends ElementFrame, com.syncleus.ferma.EdgeFrame {

	/**
	 * Return the label of the edge.
	 * 
	 * @return
	 */
	default String label() {
		return getLabel();
	}

}
