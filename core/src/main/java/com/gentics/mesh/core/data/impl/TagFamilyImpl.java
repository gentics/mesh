package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Set;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see TagFamily
 */
public class TagFamilyImpl extends AbstractMeshCoreVertex<TagFamilyResponse> implements TagFamily {

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
		return HibClassConverter.toGraph(mesh().boot().tagDao().create(this, ac, batch));
	}

	@Override
	public Tag create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return HibClassConverter.toGraph(mesh().boot().tagDao().create(this, ac, batch, uuid));
	}

	@Override
	public TagFamilyResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		TagFamilyDaoWrapper tagFamilyDao = Tx.get().tagFamilyDao();
		return tagFamilyDao.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public void delete(BulkActionContext bac) {
		TagFamilyDaoWrapper tagFamilyDao = Tx.get().tagFamilyDao();
		tagFamilyDao.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyDaoWrapper tagFamilyDao = Tx.get().tagFamilyDao();
		return tagFamilyDao.update(this, ac, batch);
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
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
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
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

	@Override
	public Tag findByName(String name) {
		return out(getRootLabel())
			.mark()
			.has(TagImpl.TAG_VALUE_KEY, name)
			.back()
			.nextOrDefaultExplicit(TagImpl.class, null);
	}

	public void deleteElement() {
		getElement().remove();
	}
}
