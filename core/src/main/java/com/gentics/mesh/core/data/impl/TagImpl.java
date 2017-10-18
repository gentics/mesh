package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.encodeFragment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.HandleElementAction;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Tag
 */
public class TagImpl extends AbstractMeshCoreVertex<TagResponse, Tag> implements Tag {

	private static final Logger log = LoggerFactory.getLogger(TagImpl.class);

	public static final String TAG_VALUE_KEY = "tagValue";

	public static void init(Database database) {
		database.addVertexType(TagImpl.class, MeshVertexImpl.class);
	}

	@Override
	public List<? extends Node> getNodes(Release release) {
		return TagEdgeImpl.getNodeTraversal(this, release).toListExplicit(NodeImpl.class);
	}

	@Override
	public String getName() {
		return getProperty(TAG_VALUE_KEY);
	}

	@Override
	public void setName(String name) {
		setProperty(TAG_VALUE_KEY, name);
	}

	@Override
	public void removeNode(Node node) {
		unlinkIn(node, HAS_TAG);
	}

	@Override
	public TagReference transformToReference() {
		return new TagReference().setName(getName()).setUuid(getUuid()).setTagFamily(getTagFamily().getName());
	}

	@Override
	public TagResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		TagResponse restTag = new TagResponse();

		TagFamily tagFamily = getTagFamily();
		if (tagFamily != null) {
			TagFamilyReference tagFamilyReference = new TagFamilyReference();
			tagFamilyReference.setName(tagFamily.getName());
			tagFamilyReference.setUuid(tagFamily.getUuid());
			restTag.setTagFamily(tagFamilyReference);
		}
		restTag.setName(getName());

		fillCommonRestFields(ac, restTag);
		setRolePermissions(ac, restTag);

		return restTag;
	}

	@Override
	public void setTagFamily(TagFamily tagFamily) {
		setUniqueLinkOutTo(tagFamily, HAS_TAGFAMILY_ROOT);
	}

	@Override
	public TagFamily getTagFamily() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
	}

	@Override
	public void setProject(Project project) {
		setUniqueLinkOutTo(project, ASSIGNED_TO_PROJECT);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag {" + getName() + "}");
		}
		batch.delete(this, true);

		// Nodes which used this tag must be updated in the search index for all releases
		for (Release release : getProject().getReleaseRoot().findAllIt()) {
			String releaseUuid = release.getUuid();
			for (Node node : getNodes(release)) {
				batch.store(node, releaseUuid);
			}
		}
		getVertex().remove();
	}

	@Override
	public TransformablePage<? extends Node> findTaggedNodes(MeshAuthUser user, Release release, List<String> languageTags, ContainerType type,
			PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = getTaggedNodesTraversal(release, languageTags, type);
		return new DynamicTransformablePageImpl<Node>(user, traversal, pagingInfo, READ_PERM, NodeImpl.class);
	}

	/**
	 * Get traversal that finds all nodes that are tagged with this tag The nodes will be restricted to
	 * <ol>
	 * <li>node is tagged for the <i>release</i></li>
	 * <li>node has field container in one of the <i>languageTags</i> in the <i>release</i> with <i>type</i></li>
	 * </ol>
	 * 
	 * @param release
	 *            Release to be used for finding nodes
	 * @param languageTags
	 *            List of language tags used to filter containers which should be included in the traversal
	 * @param type
	 *            Optional type of the node containers to filter by
	 * @return Traversal which can be used to locate the nodes
	 */
	protected VertexTraversal<?, ?, ?> getTaggedNodesTraversal(Release release, List<String> languageTags, ContainerType type) {

		EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> traversal = TagEdgeImpl.getNodeTraversal(this, release).mark()
				.outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, release.getUuid());

		if (type != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		}

		traversal = GraphFieldContainerEdgeImpl.filterLanguages(traversal, languageTags);
		return traversal.outV().back();
	}

	@Override
	public Tag update(InternalActionContext ac, SearchQueueBatch batch) {
		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		String newTagName = requestModel.getName();
		if (isEmpty(newTagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		} else {
			TagFamily tagFamily = getTagFamily();

			// Check for conflicts
			Tag foundTagWithSameName = tagFamily.findByName(newTagName);
			if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(getUuid())) {
				throw conflict(foundTagWithSameName.getUuid(), newTagName, "tag_create_tag_with_same_name_already_exists", newTagName,
						tagFamily.getName());
			}

			setEditor(ac.getUser());
			setLastEditedTimestamp();
			setName(requestModel.getName());
		}
		batch.store(getTagFamily(), false);
		batch.store(this, true);
		return this;

	}

	@Override
	public void handleRelatedEntries(HandleElementAction action) {
		// Locate all nodes that use the tag across all releases and update these nodes
		for (Release release : getProject().getReleaseRoot().findAllIt()) {
			for (Node node : getNodes(release)) {
				for (ContainerType type : Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED)) {
					GenericEntryContextImpl context = new GenericEntryContextImpl();
					context.setContainerType(type);
					context.setReleaseUuid(release.getUuid());
					context.setProjectUuid(node.getProject().getUuid());
					action.call(node, context);
				}
			}
		}
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(super.getETag(ac));
		keyBuilder.append(getLastEditedTimestamp());
		keyBuilder.append(ac.getRelease(getProject()).getUuid());
		return ETag.hash(keyBuilder);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/" + encodeFragment(getProject().getName()) + "/tagFamilies/" + getTagFamily().getUuid() + "/tags/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public Single<TagResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db.operateTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
