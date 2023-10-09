package com.gentics.mesh.core.data.node;

import java.util.List;
import java.util.Optional;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * Container object for handling nodes in combination with a specific known container.
 */
public class NodeContent {

	private HibNode node;
	private final HibNodeFieldContainer container;
	private final List<String> languageFallback;
	private final ContainerType type;
	private Optional<HibNode> nodeParent;

	/**
	 * Create a new node content.
	 * 
	 * @param node
	 * @param container
	 * @param languageFallback
	 *            Language fallback list which was used to load the content
	 */
	public NodeContent(HibNode node, HibNodeFieldContainer container, List<String> languageFallback, ContainerType type) {
		this(node, container, languageFallback, type, null);
	}

	/**
	 * Create a new node content.
	 * 
	 * @param node
	 * @param container
	 * @param languageFallback
	 *            Language fallback list which was used to load the content
	 */
	public NodeContent(HibNode node, HibNodeFieldContainer container, List<String> languageFallback, ContainerType type, HibNode nodeParent) {
		this.node = node;
		this.container = container;
		this.languageFallback = languageFallback;
		this.type = type;
		this.setNodeParent(Optional.ofNullable(nodeParent));
	}

	/**
	 * Return the node of the content.
	 * 
	 * @return
	 */
	public HibNode getNode() {
		ContentDao contentDao = Tx.get().contentDao();
		if (node == null && container != null) {
			node = contentDao.getNode(container);
		}
		return node;
	}

	public HibNodeFieldContainer getContainer() {
		return container;
	}

	public List<String> getLanguageFallback() {
		return languageFallback;
	}

	public ContainerType getType() {
		return type;
	}

	public HibNode getNodeParent(InternalActionContext ac) {
		return nodeParent.orElseGet(() -> getNode().getParentNode(ac.getVersioningParameters().getBranch()));
	}

	public void setNodeParent(Optional<HibNode> nodeParent) {
		this.nodeParent = nodeParent;
	}
}
