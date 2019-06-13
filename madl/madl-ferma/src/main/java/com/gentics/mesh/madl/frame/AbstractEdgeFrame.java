package com.gentics.mesh.madl.frame;

public abstract class AbstractEdgeFrame extends com.syncleus.ferma.AbstractEdgeFrame implements EdgeFrame {

	@Override
	@Deprecated
	public String getLabel() {
		return super.getLabel();
	}

	@Override
	public String label() {
		return getLabel();
	}
}
