package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

public class MeshNode extends GenericPropertyContainer {

	public List<Tag> getTags() {
		return out(BasicRelationships.HAS_TAG).toList(Tag.class);
	}

	public void addTag(Tag tag) {
		linkOut(tag, BasicRelationships.HAS_TAG);
	}

	public void removeTag(Tag tag) {
		unlinkOut(tag, BasicRelationships.HAS_TAG);
	}

	public List<MeshNode> getChildren() {
		return in(BasicRelationships.HAS_PARENT_NODE).toList(MeshNode.class);
	}

	public MeshNode getParentNode() {
		return out(BasicRelationships.HAS_PARENT_NODE).next(MeshNode.class);
	}

	public void setParentNode(MeshNode parent) {
		setLinkOut(parent, BasicRelationships.HAS_PARENT_NODE);
	}

}
