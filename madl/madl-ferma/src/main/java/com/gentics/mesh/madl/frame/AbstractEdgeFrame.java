package com.gentics.mesh.madl.frame;

import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

public abstract class AbstractEdgeFrame extends com.syncleus.ferma.AbstractEdgeFrame implements EdgeFrame {

	/**
	 * @deprecated Replaced by {@link #label()}
	 */
	@Override
	@Deprecated
	public String getLabel() {
		return super.getLabel();
	}

	@Override
	public VertexTraversal<?, ?, ?> inV() {
		return super.inV();
	}

	@Override
	public VertexTraversal<?, ?, ?> outV() {
		return super.outV();
	}

	@Override
	public VertexTraversal<?, ?, ?> bothV() {
		return super.bothV();
	}

	@Override
	public EdgeTraversal<?, ?, ?> traversal() {
		return super.traversal();
	}

	@Override
	public <T> T reframe(Class<T> kind) {
		return super.reframe(kind);
	}

	@Override
	public <T> T reframeExplicit(Class<T> kind) {
		return super.reframeExplicit(kind);
	}

	@Override
	public String label() {
		return getLabel();
	}

	@Override
	public void remove() {
		super.remove();
	}
}
