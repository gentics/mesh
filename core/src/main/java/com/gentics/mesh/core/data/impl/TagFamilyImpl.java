package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see TagFamily
 */
public class TagFamilyImpl extends AbstractMeshCoreVertex<TagFamilyResponse, TagFamily> implements TagFamily {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(TagFamilyImpl.class, MeshVertexImpl.class);
	}

	@Override
	public TagFamilyReference createEmptyReferenceModel() {
		return new TagFamilyReference();
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		return in(HAS_TAG_FAMILY).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
	}

	@Override
	public TagRoot getTagRoot() {
		return out(HAS_TAG_ROOT).has(TagRootImpl.class).nextOrDefaultExplicit(TagRootImpl.class, null);
	}

	@Override
	public void setTagRoot(TagRoot tagRoot) {
		linkOut(tagRoot.getImpl(), HAS_TAG_ROOT);
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
	public void setProject(Project project) {
		setUniqueLinkOutTo(project.getImpl(), ASSIGNED_TO_PROJECT);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public PageImpl<? extends Tag> getTags(MeshAuthUser requestUser, PagingParameters pagingInfo) throws InvalidArgumentException {
		// TODO check perms
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
	}

	@Override
	public Tag create(String name, Project project, User creator) {
		Tag tag = getTagRoot().create(name, project, this, creator);
		return tag;
	}

	@Override
	public Observable<Tag> create(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		return db.noTrx(() -> {
			Project project = ac.getProject();
			TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
			String tagName = requestModel.getFields().getName();
			if (isEmpty(tagName)) {
				throw error(BAD_REQUEST, "tag_name_not_set");
			}

			//			TagFamilyReference reference = requestModel.getTagFamily();
			//			if (reference == null) {
			//				throw error(BAD_REQUEST, "tag_tagfamily_reference_not_set");
			//			}
			//			boolean hasName = !isEmpty(reference.getName());
			//			boolean hasUuid = !isEmpty(reference.getUuid());
			//			if (!hasUuid && !hasName) {
			//				throw error(BAD_REQUEST, "tag_tagfamily_reference_uuid_or_name_missing");
			//			}

			// First try the tag family reference by uuid if specified
			//			TagFamily tagFamily = null;
			//			String nameOrUuid = null;
			//			if (hasUuid) {
			//				nameOrUuid = reference.getUuid();
			//				tagFamily = project.getTagFamilyRoot().findByUuid(reference.getUuid()).toBlocking().first();
			//			} else if (hasName) {
			//				nameOrUuid = reference.getName();
			//				tagFamily = project.getTagFamilyRoot().findByName(reference.getName()).toBlocking().first();
			//			}

			MeshAuthUser requestUser = ac.getUser();
			if (!requestUser.hasPermissionSync(ac, this, CREATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", getUuid());
			}

			Tag conflictingTag = getTagRoot().findByName(tagName).toBlocking().single();
			if (conflictingTag != null) {
				throw conflict(conflictingTag.getUuid(), tagName, "tag_create_tag_with_same_name_already_exists", tagName, getName());
			}

			Tuple<SearchQueueBatch, Tag> tuple = db.trx(() -> {
				this.reload();
				requestUser.reload();
				project.reload();
				Tag newTag = create(requestModel.getFields().getName(), project, requestUser);
				ac.getUser().addCRUDPermissionOnRole(this, CREATE_PERM, newTag);
				BootstrapInitializer.getBoot().meshRoot().getTagRoot().addTag(newTag);
				getTagRoot().addTag(newTag);

				SearchQueueBatch batch = newTag.createIndexBatch(STORE_ACTION);
				return Tuple.tuple(batch, newTag);
			});

			SearchQueueBatch batch = tuple.v1();
			Tag tag = tuple.v2();

			return batch.process().map(t -> tag);
		});
	}

	@Override
	public Observable<TagFamilyResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		Set<Observable<TagFamilyResponse>> obs = new HashSet<>();

		TagFamilyResponse restTagFamily = new TagFamilyResponse();
		restTagFamily.setName(getName());

		// Add common fields
		obs.add(fillCommonRestFields(ac, restTagFamily));

		// Role permissions
		obs.add(setRolePermissions(ac, restTagFamily));

		// Merge and complete
		return Observable.merge(obs).last();
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		batch.addEntry(this, DELETE_ACTION);
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + getName() + "}");
		}
		for (Tag tag : getTagRoot().findAll()) {
			tag.delete(batch);
		}
		getTagRoot().delete(batch);
		getElement().remove();

	}

	@Override
	public Observable<TagFamily> update(InternalActionContext ac) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.trx(() -> {
			Project project = ac.getProject();
			String newName = requestModel.getName();

			if (isEmpty(newName)) {
				throw error(BAD_REQUEST, "tagfamily_name_not_set");
			}

			Observable<TagFamily> tagFamilyWithSameNameObs = project.getTagFamilyRoot().findByName(newName);
			Observable<TagFamily> obs = tagFamilyWithSameNameObs.map(tagFamilyWithSameName -> {
				if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(this.getUuid())) {
					throw conflict(tagFamilyWithSameName.getUuid(), newName, "tagfamily_conflicting_name", newName);
				}
				SearchQueueBatch batch = db.trx(() -> {
					this.setName(newName);
					return createIndexBatch(STORE_ACTION);
				});

				batch.process().toBlocking().single();
				return this;
			});
			return obs;
		});
	}

	@Override
	public TagFamilyImpl getImpl() {
		return this;
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Tag tag : getTagRoot().findAll()) {
				tag.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		List<Tuple<String, Object>> customProperties = Arrays
				.asList(Tuple.tuple(TagFamilyIndexHandler.CUSTOM_PROJECT_UUID, getProject().getUuid()));
		if (action == DELETE_ACTION) {
			for (Tag tag : getTagRoot().findAll()) {
				batch.addEntry(tag, DELETE_ACTION, customProperties);
			}
		} else {
			for (Tag tag : getTagRoot().findAll()) {
				batch.addEntry(tag, STORE_ACTION, customProperties);
			}
		}
	}

	@Override
	public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		batch.addEntry(this, action,
				Arrays.asList(Tuple.tuple(TagFamilyIndexHandler.CUSTOM_PROJECT_UUID, getProject().getUuid())));
		addRelatedEntries(batch, action);
		return batch;
	}
}
