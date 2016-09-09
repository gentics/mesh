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
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.TagGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.TagGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
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
import rx.Completable;
import rx.Single;

/**
 * @see Tag
 */
public class TagImpl extends AbstractGenericFieldContainerVertex<TagResponse, Tag> implements Tag {

	private static final Logger log = LoggerFactory.getLogger(TagImpl.class);

	public static final String DEFAULT_TAG_LANGUAGE_TAG = "en";

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
	public List<? extends TagGraphFieldContainer> getFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(TagGraphFieldContainerImpl.class).toListExplicit(TagGraphFieldContainerImpl.class);
	}

	@Override
	public TagGraphFieldContainer getFieldContainer(Language language) {
		return getGraphFieldContainer(language, null, null, TagGraphFieldContainerImpl.class);
	}

	@Override
	public TagGraphFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateGraphFieldContainer(language, TagGraphFieldContainerImpl.class);
	}

	@Override
	public String getName() {
		return getFieldContainer(MeshInternal.get().boot().languageRoot().getTagDefaultLanguage()).getName();
	}

	@Override
	public void setName(String name) {
		getOrCreateFieldContainer(MeshInternal.get().boot().languageRoot().getTagDefaultLanguage()).setName(name);
	}

	@Override
	public void removeNode(Node node) {
		unlinkIn(node.getImpl(), HAS_TAG);
	}

	@Override
	public Single<TagResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		TagResponse restTag = new TagResponse();

		TagFamily tagFamily = getTagFamily();
		if (tagFamily != null) {
			TagFamilyReference tagFamilyReference = new TagFamilyReference();
			tagFamilyReference.setName(tagFamily.getName());
			tagFamilyReference.setUuid(tagFamily.getUuid());
			restTag.setTagFamily(tagFamilyReference);
		}
		restTag.getFields().setName(getName());

		Completable filledFields = fillCommonRestFields(ac, restTag);
		Completable setPerms = setRolePermissions(ac, restTag);

		return Completable.merge(filledFields, setPerms).andThen(Single.just(restTag));
	}

	@Override
	public void setTagFamily(TagFamily tagFamily) {
		setUniqueLinkOutTo(tagFamily.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	@Override
	public TagFamily getTagFamily() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
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
	public PageImpl<? extends Node> findTaggedNodes(MeshAuthUser requestUser, Release release, List<String> languageTags, ContainerType type,
			PagingParameters pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = getTaggedNodesTraversal(requestUser, release, languageTags, type);
		PageImpl<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, pagingInfo, NodeImpl.class);
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
				.out(HAS_ROLE).in(HAS_USER).retain(requestUser.getImpl()).back().mark().outE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, release.getUuid()).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());

		traversal = GraphFieldContainerEdgeImpl.filterLanguages(traversal, languageTags);
		return traversal.outV().back();
	}

	@Override
	public Single<Tag> update(InternalActionContext ac) {
		Database db = MeshInternal.get().database();
		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		return db.tx(() -> {
			String newTagName = requestModel.getFields().getName();
			if (isEmpty(newTagName)) {
				throw error(BAD_REQUEST, "tag_name_not_set");
			} else {
				TagFamily tagFamily = getTagFamily();

				// Check for conflicts
				Tag foundTagWithSameName = tagFamily.getTagRoot().findByName(newTagName).toBlocking().value();
				if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(getUuid())) {
					throw conflict(foundTagWithSameName.getUuid(), newTagName, "tag_create_tag_with_same_name_already_exists", newTagName,
							tagFamily.getName());
				}

				setEditor(ac.getUser());
				setLastEditedTimestamp(System.currentTimeMillis());
				setName(requestModel.getFields().getName());
			}
			return createIndexBatch(STORE_ACTION);
		}).process().toSingleDefault(this);

	}

	@Override
	public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch();
		batch.addEntry(this, action).set(TagIndexHandler.CUSTOM_PROJECT_UUID, getProject().getUuid());
		addRelatedEntries(batch, action);
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
	public TagReference createEmptyReferenceModel() {
		return new TagReference();
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
