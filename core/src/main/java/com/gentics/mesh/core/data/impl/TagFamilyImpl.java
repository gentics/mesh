package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RestModelHelper;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class TagFamilyImpl extends AbstractIndexedVertex<TagFamilyResponse>implements TagFamily {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(TagFamilyImpl.class);
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		TagFamilyRoot root = in(HAS_TAG_FAMILY).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
		return root;
	}

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
	public Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException {
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
	public Tag create(String name, User creator) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		tag.setCreated(creator);
		addTag(tag);
		// Add to global list of tags
		TagRoot tagRoot = BootstrapInitializer.getBoot().tagRoot();
		tagRoot.addTag(tag);
		// Add tag to project list of tags
		TagFamilyRoot root = getTagFamilyRoot();
		Objects.requireNonNull(root, "The tag root for tag family {" + getName() + "} could not be found.");

		Project project = root.getProject();
		Objects.requireNonNull(project, "The project of tag family root {" + root.getUuid() + "} could not be found.");
		project.getTagRoot().addTag(tag);
		return tag;
	}

	@Override
	public TagFamily transformToRest(InternalActionContext ac, Handler<AsyncResult<TagFamilyResponse>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrx(noTrx -> {
			Set<ObservableFuture<Void>> futures = new HashSet<>();

			TagFamilyResponse restTagFamily = new TagFamilyResponse();
			restTagFamily.setName(getName());

			// Add common fields
			ObservableFuture<Void> obsFieldSet = RxHelper.observableFuture();
			futures.add(obsFieldSet);
			fillCommonRestFields(restTagFamily, ac, rh -> {
				if (rh.failed()) {
					obsFieldSet.toHandler().handle(Future.failedFuture(rh.cause()));
				} else {
					obsFieldSet.toHandler().handle(Future.succeededFuture());
				}
			});

			// Role permissions
			RestModelHelper.setRolePermissions(ac, this, restTagFamily);

			// Merge and complete
			Observable.merge(futures).last().subscribe(lastItem -> {
				noTrx.complete(restTagFamily);
			} , error -> {
				noTrx.fail(error);
			});

		} , (AsyncResult<TagFamilyResponse> rh) -> {
			handler.handle(rh);
		});
		return this;
	}

	@Override
	public void delete() {
		addIndexBatch(DELETE_ACTION);
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + getName() + "}");
		}
		for (Tag tag : getTags()) {
			tag.remove();
		}
		getElement().remove();

	}

	@Override
	public void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Project project = ac.getProject();
		Database db = MeshSpringConfiguration.getInstance().database();
		String newName = requestModel.getName();

		if (StringUtils.isEmpty(newName)) {
			handler.handle(failedFuture(ac, BAD_REQUEST, "tagfamily_name_not_set"));
		} else {
			loadObject(ac, "uuid", UPDATE_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					TagFamily tagFamilyWithSameName = project.getTagFamilyRoot().findByName(newName);
					TagFamily tagFamily = rh.result();
					if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(tagFamily.getUuid())) {
						HttpStatusCodeErrorException conflictError = conflict(ac, tagFamilyWithSameName.getUuid(), newName,
								"tagfamily_conflicting_name", newName);
						handler.handle(Future.failedFuture(conflictError));
						return;
					}
					db.trx(txUpdate -> {
						tagFamily.setName(newName);
						SearchQueueBatch batch = addIndexBatch(UPDATE_ACTION);
						txUpdate.complete(batch);
					} , (AsyncResult<SearchQueueBatch> txUpdated) -> {
						if (txUpdated.failed()) {
							handler.handle(Future.failedFuture(txUpdated.cause()));
						} else {
							processOrFail2(ac, txUpdated.result(), handler);
						}
					});
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
