package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractIndexedVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagFamilyImpl extends AbstractIndexedVertex<TagFamilyResponse>implements TagFamily {

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
	public TagFamily transformToRest(InternalActionContext ac, Handler<AsyncResult<TagFamilyResponse>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrx(tc -> {
			TagFamilyResponse response = new TagFamilyResponse();
			response.setName(getName());

			fillRest(response, ac);
			tc.complete(response);
		} , (AsyncResult<TagFamilyResponse> rh) -> {
			handler.handle(rh);
		});
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
		addIndexBatch(DELETE_ACTION);

	}

	@Override
	public void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Project project = ac.getProject();
		Database db = MeshSpringConfiguration.getInstance().database();
		String newName = requestModel.getName();

		if (StringUtils.isEmpty(newName)) {
			handler.handle(ac.failedFuture(BAD_REQUEST, "tagfamily_name_not_set"));
		} else {
			loadObject(ac, "uuid", UPDATE_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					TagFamily tagFamilyWithSameName = project.getTagFamilyRoot().findByName(newName);
					TagFamily tagFamily = rh.result();
					if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(tagFamily.getUuid())) {
						handler.handle(ac.failedFuture(CONFLICT, "tagfamily_conflicting_name", newName));
						return;
					}
					SearchQueueBatch batch;
					try (Trx txUpdate = db.trx()) {
						tagFamily.setName(newName);
						batch = addIndexBatch(UPDATE_ACTION);
						txUpdate.success();
					}
					processOrFail2(ac, batch, handler);
				}
			});
		}

	}

	@Override
	public TagFamilyImpl getImpl() {
		return this;
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Tag tag : getTags()) {
				tag.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		if (action == DELETE_ACTION) {
			for (Tag tag : getTags()) {
				batch.addEntry(tag, DELETE_ACTION);
			}
		} else {
			for (Tag tag : getTags()) {
				batch.addEntry(tag, UPDATE_ACTION);
			}
		}
	}

}
