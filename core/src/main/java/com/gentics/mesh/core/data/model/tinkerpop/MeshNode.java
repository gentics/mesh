package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class MeshNode extends GenericPropertyContainer {

	public List<? extends Tag> getTags() {
		return out(MeshRelationships.HAS_TAG).toList(Tag.class);
	}

	public void addTag(Tag tag) {
		linkOut(tag, MeshRelationships.HAS_TAG);
	}

	public void removeTag(Tag tag) {
		unlinkOut(tag, MeshRelationships.HAS_TAG);
	}

	public List<? extends MeshNode> getChildren() {
		return in(MeshRelationships.HAS_PARENT_NODE).toList(MeshNode.class);
	}

	public MeshNode getParentNode() {
		return out(MeshRelationships.HAS_PARENT_NODE).next(MeshNode.class);
	}

	public void setParentNode(MeshNode parent) {
		setLinkOut(parent, MeshRelationships.HAS_PARENT_NODE);
	}

}
