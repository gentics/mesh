package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class TagFamilyImpl extends AbstractGenericVertex<TagFamilyResponse>implements TagFamily {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyImpl.class);

	@Override
	public String getType() {
		return TagFamily.TYPE;
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getDescription() {
		return getProperty("description");
	}

	@Override
	public void setDescription(String description) {
		setProperty("description", description);
	}

	@Override
	public Tag findTagByName(String name) {
		return out(HAS_TAG).has(TagImpl.class).mark().out(HAS_FIELD_CONTAINER).has("name", name).back().nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	}

	@Override
	public Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// TODO check perms
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
	}

	@Override
	public void addTag(Tag tag) {
		setLinkOutTo(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void removeTag(Tag tag) {
		unlinkOut(tag.getImpl(), HAS_TAG);
		// TODO delete tag node?!
	}

	@Override
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

		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + getName() + "}");
		}
		for (Tag tag : getTags()) {
			tag.remove();
		}
		getElement().remove();

	}

	@Override
	public TagFamilyImpl getImpl() {
		return this;
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<Permission> permissionsToGrant, Set<Permission> permissionsToRevoke) {
		if (recursive) {
			for (Tag tag : getTags()) {
				tag.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

}
