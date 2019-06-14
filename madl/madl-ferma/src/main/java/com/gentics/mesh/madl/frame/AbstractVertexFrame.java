package com.gentics.mesh.madl.frame;

import java.util.Set;

import com.gentics.mesh.madl.traversal.TraversalResult;

public abstract class AbstractVertexFrame extends com.syncleus.ferma.AbstractVertexFrame implements VertexFrame {

	/**
	 * @deprecated Replaced by {@link #id()}
	 */
	@Deprecated
	@Override
	public <N> N getId() {
		return super.getId();
	}

	@Override
	public <N> N id() {
		return getId();
	}

	@Override
	public <T> T getProperty(String name) {
		return super.getProperty(name);
	}

	@Override
	public Set<String> getPropertyKeys() {
		return super.getPropertyKeys();
	}

	@Override
	public void remove() {
		super.remove();
	}

	@Override
	public <K> K setLinkOutExplicit(Class<K> kind, String... labels) {
		return super.setLinkOutExplicit(kind, labels);
	}

	@Override
	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkOut(vertex, labels);
		// Create a new edge with the given label
		linkOut(vertex, labels);
	}

	/**
	 * Add a single link <b>in-bound</b> link to the given vertex. Note that this method will remove all other links to other vertices for the given labels and
	 * only create a single edge between both vertices per label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	@Override
	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges with the given label
		unlinkIn(null, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	/**
	 * Add a unique <b>in-bound</b> link to the given vertex for the given set of labels. Note that this method will effectively ensure that only one
	 * <b>in-bound</b> link exists between the two vertices for each label.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	@Override
	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		// Unlink all edges between both objects with the given label
		unlinkIn(vertex, labels);
		// Create a new edge with the given label
		linkIn(vertex, labels);
	}

	/**
	 * Remove all out-bound edges with the given label from the current vertex and create a new new <b>out-bound</b> edge between the current and given vertex
	 * using the specified label. Note that only a single out-bound edge per label will be preserved.
	 * 
	 * @param vertex
	 *            Target vertex
	 * @param labels
	 *            Labels to handle
	 */
	@Override
	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		// Unlink all edges with the given label
		unlinkOut(null, labels);
		// Create a new edge with the given label
		linkOut(vertex, labels);
	}

	@Override
	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(out(label).frameExplicit(clazz));
		return result;
	}

	@Override
	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		TraversalResult<? extends T> result = new TraversalResult<>(in(label).frameExplicit(clazz));
		return result;
	}
}
