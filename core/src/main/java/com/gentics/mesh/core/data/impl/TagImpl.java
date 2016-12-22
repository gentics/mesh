package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.encodeFragment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

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
	public String getType() {
		return Tag.TYPE;
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
		return new TagReference().setName(getName()).setUuid(getUuid());
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
		batch.addEntry(this, DELETE_ACTION);

		// Nodes which used this tag must be updated in the search index
		// for all releases
		for (Release release : getProject().getReleaseRoot().findAll()) {
			String releaseUuid = release.getUuid();
			// all nodes
			for (Node node : getNodes(release)) {
				// draft and published versions
				for (ContainerType type : Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED)) {
					// all languages
					for (NodeGraphFieldContainer container : node.getGraphFieldContainers(release, type)) {
						container.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, type);
					}
				}
			}
		}
		getVertex().remove();
	}

	@Override
	public Page<? extends Node> findTaggedNodes(MeshAuthUser requestUser, Release release, List<String> languageTags, ContainerType type,
			PagingParameters pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = getTaggedNodesTraversal(requestUser, release, languageTags, type);
		Page<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	/**
	 * Get traversal that finds all nodes that are tagged with this tag The nodes will be restricted to
	 * <ol>
	 * <li><i>requestUser</i> may read the node</li>
	 * <li>node is tagged for the <i>release</i></li>
	 * <li>node has field container in one of the <i>languageTags</i> in the <i>release</i> with <i>type</i></li>
	 * </ol>
	 * 
	 * @param requestUser
	 * @param release
	 * @param languageTags
	 * @param type
	 * @return
	 */
	protected VertexTraversal<?, ?, ?> getTaggedNodesTraversal(MeshAuthUser requestUser, Release release, List<String> languageTags,
			ContainerType type) {

		EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> traversal = TagEdgeImpl.getNodeTraversal(this, release).mark().in(READ_PERM.label())
				.out(HAS_ROLE).in(HAS_USER).retain(requestUser).back().mark().outE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, release.getUuid()).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());

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
			Tag foundTagWithSameName = tagFamily.getTagRoot().findByName(newTagName);
			if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(getUuid())) {
				throw conflict(foundTagWithSameName.getUuid(), newTagName, "tag_create_tag_with_same_name_already_exists", newTagName,
						tagFamily.getName());
			}

			setEditor(ac.getUser());
			setLastEditedTimestamp();
			setName(requestModel.getName());
		}
		addIndexBatchEntry(batch, STORE_ACTION, true);
		return this;

	}

	@Override
	public SearchQueueBatch addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action, boolean addRelatedEntries) {
		batch.addEntry(this, action).set(TagIndexHandler.CUSTOM_PROJECT_UUID, getProject().getUuid());
		if (addRelatedEntries) {
			addRelatedEntries(batch, action);
		}
		return batch;
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		for (Release release : getProject().getReleaseRoot().findAll()) {
			String releaseUuid = release.getUuid();
			for (Node node : getNodes(release)) {
				for (ContainerType type : Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED)) {
					for (NodeGraphFieldContainer container : node.getGraphFieldContainers(release, type)) {
						container.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, type);
					}
				}
			}
		}
		SearchQueueEntry entry = batch.addEntry(getTagFamily(), STORE_ACTION);
		entry.set(TagIndexHandler.CUSTOM_PROJECT_UUID, getProject().getUuid());
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid() + "-" + getLastEditedTimestamp() + "-" + ac.getRelease(getProject()).getUuid());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/" + encodeFragment(getProject().getName()) + "/tagFamilies/" + getTagFamily().getUuid() + "/tags/" + getUuid();
	}
}
