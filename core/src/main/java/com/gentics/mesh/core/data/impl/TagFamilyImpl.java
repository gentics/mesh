package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
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
		database.addVertexType(TagFamilyImpl.class);
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
	//
	//	@Override
	//	public Tag findTagByName(String name) {
	//		return out(HAS_TAG).has(TagImpl.class).mark().out(HAS_FIELD_CONTAINER).has("name", name).back().nextOrDefaultExplicit(TagImpl.class, null);
	//	}
	//
	//	@Override
	//	public List<? extends Tag> getTags() {
	//		return out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	//	}

	@Override
	public PageImpl<? extends Tag> getTags(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException {
		// TODO check perms
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
	}

	//	@Override
	//	public void addTag(Tag tag) {
	//		setLinkOutTo(tag.getImpl(), HAS_TAG);
	//	}
	//
	//	@Override
	//	public void removeTag(Tag tag) {
	//		unlinkOut(tag.getImpl(), HAS_TAG);
	//		// TODO delete tag node?!
	//	}

	@Override
	public Tag create(String name, Project project, User creator) {
		Tag tag = getTagRoot().create(name, project, this, creator);
		return tag;
	}

	@Override
	public Observable<TagFamilyResponse> transformToRestSync(InternalActionContext ac, String...languageTags) {
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
	public void delete() {
		addIndexBatch(DELETE_ACTION);
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + getName() + "}");
		}
		for (Tag tag : getTagRoot().findAll()) {
			tag.remove();
		}
		getTagRoot().delete();
		getElement().remove();

	}

	@Override
	public Observable<TagFamily> update(InternalActionContext ac) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.trx(() -> {
			Project project = ac.getProject();
			String newName = requestModel.getName();

			if (StringUtils.isEmpty(newName)) {
				throw error(BAD_REQUEST, "tagfamily_name_not_set");
			}

			Observable<TagFamily> tagFamilyObs = project.getTagFamilyRoot().loadObject(ac, "uuid", UPDATE_PERM);
			Observable<TagFamily> tagFamilyWithSameNameObs = project.getTagFamilyRoot().findByName(newName);

			Observable<TagFamily> obs = Observable.zip(tagFamilyObs, tagFamilyWithSameNameObs, (tagFamily, tagFamilyWithSameName) -> {
				if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(tagFamily.getUuid())) {
					throw conflict(tagFamilyWithSameName.getUuid(), newName, "tagfamily_conflicting_name", newName);
				}
				SearchQueueBatch batch = db.trx(() -> {
					tagFamily.setName(newName);
					return addIndexBatch(UPDATE_ACTION);
				});

				// TODO i have no clue why map(i-> tagFamily) is not working.
				batch.process().toBlocking().first();
				return tagFamily;
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
		if (action == DELETE_ACTION) {
			for (Tag tag : getTagRoot().findAll()) {
				batch.addEntry(tag, DELETE_ACTION);
			}
		} else {
			for (Tag tag : getTagRoot().findAll()) {
				batch.addEntry(tag, UPDATE_ACTION);
			}
		}
	}

}
