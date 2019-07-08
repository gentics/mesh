package com.gentics.madl.frame;

import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.traversal.TraversalResult;
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
	public <T extends VertexFrame> TraversalResult<? extends T> inV(Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(inV().frameExplicit(clazz));
		return result;
	}

	@Override
	public VertexTraversal<?, ?, ?> outV() {
		return super.outV();
	}

	@Override
	public <T extends VertexFrame> TraversalResult<? extends T> outV(Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(outV().frameExplicit(clazz));
		return result;
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
