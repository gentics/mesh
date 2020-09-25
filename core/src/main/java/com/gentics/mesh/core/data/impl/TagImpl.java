package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.TagPermissionChangedEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandlerImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Tag
 */
public class TagImpl extends AbstractMeshCoreVertex<TagResponse> implements Tag {

	private static final Logger log = LoggerFactory.getLogger(TagImpl.class);

	public static final String TAG_VALUE_KEY = "tagValue";

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagImpl.class, MeshVertexImpl.class);
	}

	@Override
	public String getName() {
		return property(TAG_VALUE_KEY);
	}

	@Override
	public void setName(String name) {
		property(TAG_VALUE_KEY, name);
	}

	@Override
	public TagReference transformToReference() {
		return new TagReference().setName(getName()).setUuid(getUuid()).setTagFamily(getTagFamily().getName());
	}

	/**
	 * Use {@link TagDaoWrapper#transformToRestSync(Tag, InternalActionContext, int, String...)} instead.
	 */
	@Override
	@Deprecated
	public TagResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		TagDaoWrapper tagDao = Tx.get().tagDao();
		return tagDao.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public void setTagFamily(HibTagFamily tagFamily) {
		setUniqueLinkOutTo(toGraph(tagFamily), HAS_TAGFAMILY_ROOT);
	}

	@Override
	public HibTagFamily getTagFamily() {
		return out(HAS_TAGFAMILY_ROOT, TagFamilyImpl.class).nextOrNull();
	}

	public void setProject(HibProject project) {
		setUniqueLinkOutTo(toGraph(project), ASSIGNED_TO_PROJECT);
	}

	@Override
	public HibProject getProject() {
		return out(ASSIGNED_TO_PROJECT, ProjectImpl.class).nextOrNull();
	}

	@Deprecated
	@Override
	public void delete(BulkActionContext bac) {
		Tx.get().tagDao().delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		TagDaoWrapper tagDao = Tx.get().tagDao();
		return tagDao.update(this, ac, batch);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		TagDaoWrapper tagRoot = mesh().boot().tagDao();
		return tagRoot.getSubETag(this, ac);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandlerImpl.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getTagFamily().getUuid() + "/tags/" + getUuid();
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
	protected TagMeshEventModel createEvent(MeshEvent type) {
		TagMeshEventModel event = new TagMeshEventModel();
		event.setEvent(type);
		fillEventInfo(event);

		// .project
		HibProject project = getProject();
		ProjectReference reference = project.transformToReference();
		event.setProject(reference);

		// .tagFamily
		HibTagFamily tagFamily = getTagFamily();
		TagFamilyReference tagFamilyReference = tagFamily.transformToReference();
		event.setTagFamily(tagFamilyReference);
		return event;
	}

	@Override
	public TagPermissionChangedEventModel onPermissionChanged(Role role) {
		TagPermissionChangedEventModel model = new TagPermissionChangedEventModel();
		fillPermissionChanged(model, role);
		model.setTagFamily(getTagFamily().transformToReference());
		return model;
	}

	@Override
	public void deleteElement() {
		getElement().remove();
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}

	@Override
	public void generateBucketId() {
		BucketableElementHelper.generateBucketId(this);
	}
}
