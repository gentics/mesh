package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class TagFamilyImpl extends AbstractGenericVertex<TagFamilyResponse> implements TagFamily {

	@Override
	public String getType() {
		return TagFamily.TYPE;
	}

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

	public Tag create(String name, Project project, User creator) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		tag.setCreator(creator);
		tag.setEditor(creator);
		addTag(tag);
		// Add to global list of tags
		TagRoot tagRoot = BootstrapInitializer.getBoot().tagRoot();
		tagRoot.addTag(tag);
		// Add tag to project list of tags
		project.getTagRoot().addTag(tag);
		return tag;
	}

	@Override
	public TagFamily transformToRest(RoutingContext rc, Handler<AsyncResult<TagFamilyResponse>> handler) {
		TagFamilyResponse response = new TagFamilyResponse();
		response.setName(getName());

		fillRest(response, rc);
		handler.handle(Future.succeededFuture(response));
		return this;
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
