package com.gentics.mesh.madl.frame;

public interface EdgeFrame extends ElementFrame, com.syncleus.ferma.EdgeFrame {

	/**
	 * Return the label of the edge.
	 * 
	 * @return
	 */
	default String label() {
		//TODO Move this to edge frame instead
		return getLabel();
	}

}
