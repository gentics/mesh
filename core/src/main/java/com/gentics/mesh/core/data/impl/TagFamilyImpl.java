package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class TagFamilyImpl extends AbstractGenericNode implements TagFamily {

	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getDescription() {
		return getProperty("description");
	}

	public void setDescription(String description) {
		setProperty("description", description);
	}

	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	}

	@Override
	public Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		//TODO check perms
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
	}

	public void addTag(Tag tag) {
		linkOut(tag.getImpl(), HAS_TAG);
	}

	public void removeTag(Tag tag) {
		unlinkOut(tag.getImpl(), HAS_TAG);
		// TODO delete tag node?!
	}

	public Tag create(String name) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		addTag(tag);
		TagRoot tagRoot = BootstrapInitializer.getBoot().tagRoot();
		tagRoot.addTag(tag);
		return tag;
	}

	@Override
	public TagFamilyResponse transformToRest(MeshAuthUser requestUser) {
		TagFamilyResponse response = new TagFamilyResponse();
		response.setName(getName());

		fillRest(response, requestUser);
		return response;
	}

	@Override
	public void delete() {
		//TODO also deletetags?
		getElement().remove();
	}

	@Override
	public TagFamilyImpl getImpl() {
		return this;
	}
}
