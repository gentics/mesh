package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Set;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see TagFamily
 */
public class TagFamilyImpl extends AbstractMeshCoreVertex<TagFamilyResponse, TagFamily> implements TagFamily {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagFamilyImpl.class, MeshVertexImpl.class);
		// TODO why was the branch key omitted? TagEdgeImpl.BRANCH_UUID_KEY
		index.createIndex(edgeIndex(HAS_TAG));
		index.createIndex(edgeIndex(HAS_TAG));
		index.createIndex(edgeIndex(HAS_TAG).withInOut().withOut());
	}

	@Override
	public TagFamilyReference transformToReference() {
		return new TagFamilyReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		return in(HAS_TAG_FAMILY).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public String getDescription() {
		return property("description");
	}

	@Override
	public void setDescription(String description) {
		property("description", description);
	}

	@Override
	public void setProject(Project project) {
		setUniqueLinkOutTo(project, ASSIGNED_TO_PROJECT);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT, ProjectImpl.class).nextOrNull();
	}

	@Override
	public Page<? extends Tag> getTags(MeshAuthUser user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		return new DynamicTransformablePageImpl<Tag>(user, traversal, pagingInfo, READ_PERM, TagImpl.class);
	}

	@Override
	public Tag create(InternalActionContext ac, EventQueueBatch batch) {
		return create(ac, batch, null);
	}

	@Override
	public Tag create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Project project = ac.getProject();
		TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
		String tagName = requestModel.getName();
		if (isEmpty(tagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		}

		MeshAuthUser requestUser = ac.getUser();
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		Tag conflictingTag = findByName(tagName);
		if (conflictingTag != null) {
			throw conflict(conflictingTag.getUuid(), tagName, "tag_create_tag_with_same_name_already_exists", tagName, getName());
		}

		Tag newTag = create(requestModel.getName(), project, requestUser, uuid);
		ac.getUser().addCRUDPermissionOnRole(this, CREATE_PERM, newTag);
		addTag(newTag);

		batch.add(newTag.onCreated());
		return newTag;
	}

	@Override
	public TagFamilyResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		TagFamilyResponse restTagFamily = new TagFamilyResponse();
		if (fields.has("uuid")) {
			restTagFamily.setUuid(getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return restTagFamily;
			}
		}

		if (fields.has("name")) {
			restTagFamily.setName(getName());
		}

		fillCommonRestFields(ac, fields, restTagFamily);

		if (fields.has("perms")) {
			setRolePermissions(ac, restTagFamily);
		}

		return restTagFamily;
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamily {" + getName() + "}");
		}

		// Delete all the tags of the tag root
		for (Tag tag : findAll()) {
			tag.delete(bac);
		}

		bac.add(onDeleted());

		// Now delete the tag root element
		getElement().remove();
		bac.process();
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Project project = ac.getProject();
		String newName = requestModel.getName();

		if (isEmpty(newName)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}

		TagFamily tagFamilyWithSameName = project.getTagFamilyRoot().findByName(newName);
		if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(this.getUuid())) {
			throw conflict(tagFamilyWithSameName.getUuid(), newName, "tagfamily_conflicting_name", newName);
		}
		if (!getName().equals(newName)) {
			this.setName(newName);
			batch.add(onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Tag tag : findAll()) {
				tag.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(getLastEditedTimestamp());
		return keyBuilder.toString();
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return CURRENT_API_BASE_PATH + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR, UserImpl.class).nextOrNull();
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR, UserImpl.class).nextOrNull();
	}

	@Override
	public Single<TagFamilyResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

	@Override
	public Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_TAG;
	}

	@Override
	public void addTag(Tag tag) {
		addItem(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		removeItem(tag);
	}

	@Override
	public Tag findByName(String name) {
		return out(getRootLabel()).mark().has(TagImpl.TAG_VALUE_KEY, name).back().nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public Tag create(String name, Project project, User creator, String uuid) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		if (uuid != null) {
			tag.setUuid(uuid);
		}
		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);

		// Add the tag to the global tag root
		mesh().boot().meshRoot().getTagRoot().addTag(tag);
		// And to the tag family
		addTag(tag);

		// Set the tag family for the tag
		tag.setTagFamily(this);
		return tag;
	}

	@Override
	protected TagFamilyMeshEventModel createEvent(MeshEvent type) {
		TagFamilyMeshEventModel event = new TagFamilyMeshEventModel();
		event.setEvent(type);
		fillEventInfo(event);

		// .project
		Project project = getProject();
		ProjectReference reference = project.transformToReference();
		event.setProject(reference);

		return event;
	}

	@Override
	public PermissionChangedProjectElementEventModel onPermissionChanged(Role role) {
		PermissionChangedProjectElementEventModel model = new PermissionChangedProjectElementEventModel();
		fillPermissionChanged(model, role);
		return model;
	}

}
