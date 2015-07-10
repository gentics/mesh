package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.core.data.GenericNode;

public interface ContainerNode extends GenericNode {

	List<? extends Node> getChildren();

	void setParentNode(ContainerNode parentNode);

	Node create();

}
