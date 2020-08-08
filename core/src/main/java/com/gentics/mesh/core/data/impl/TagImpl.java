package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.role.TagPermissionChangedEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
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

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagImpl.class, MeshVertexImpl.class);
	}

	@Override
	public TraversalResult<? extends Node> getNodes(Branch branch) {
		Iterable<? extends NodeImpl> it = TagEdgeImpl.getNodeTraversal(this, branch).frameExplicit(NodeImpl.class);
		return new TraversalResult<>(it);
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
	public void removeNode(Node node) {
		unlinkIn(node, HAS_TAG);
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
		TagRoot tagRoot = mesh().boot().tagRoot();
		return tagRoot.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public void setTagFamily(TagFamily tagFamily) {
		setUniqueLinkOutTo(tagFamily, HAS_TAGFAMILY_ROOT);
	}

	@Override
	public TagFamily getTagFamily() {
		return out(HAS_TAGFAMILY_ROOT, TagFamilyImpl.class).nextOrNull();
	}

	@Override
	public void setProject(Project project) {
		setUniqueLinkOutTo(project, ASSIGNED_TO_PROJECT);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT, ProjectImpl.class).nextOrNull();
	}

	@Deprecated
	@Override
	public void delete(BulkActionContext bac) {
		Tx.get().data().tagDao().delete(this, bac);
	}

	@Override
	public TransformablePage<? extends Node> findTaggedNodes(MeshAuthUser user, Branch branch, List<String> languageTags, ContainerType type,
		PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = getTaggedNodesTraversal(branch, languageTags, type);
		return new DynamicTransformablePageImpl<Node>(user, traversal, pagingInfo, READ_PUBLISHED_PERM, NodeImpl.class);
	}

	@Override
	public TraversalResult<? extends Node> findTaggedNodes(InternalActionContext ac) {
		MeshAuthUser user = ac.getUser();
		Branch branch = ac.getBranch();
		String branchUuid = branch.getUuid();
		UserDaoWrapper userRoot = Tx.get().data().userDao();
		TraversalResult<? extends Node> nodes = new TraversalResult<>(inE(HAS_TAG).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).outV().frameExplicit(NodeImpl.class));
		Stream<? extends Node> s = nodes.stream()
			.filter(item -> {
				// Check whether the node has at least a draft in the selected branch - Otherwise the node should be skipped
				return GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, DRAFT);
			})
			.filter(item -> {
				boolean hasRead = userRoot.hasPermissionForId(user, item.getId(), READ_PERM);
				if (hasRead) {
					return true;
				} else {
					// Check whether the node is published. In this case we need to check the read publish perm.
					boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
					if (isPublishedForBranch) {
						return userRoot.hasPermissionForId(user, item.getId(), READ_PUBLISHED_PERM);
					}
				}
				return false;
			});

		return new TraversalResult<>(() -> s.iterator());
	}

	/**
	 * Get traversal that finds all nodes that are tagged with this tag The nodes will be restricted to
	 * <ol>
	 * <li>node is tagged for the <i>branch</i></li>
	 * <li>node has field container in one of the <i>languageTags</i> in the <i>branch</i> with <i>type</i></li>
	 * </ol>
	 * 
	 * @param branch
	 *            Branch to be used for finding nodes
	 * @param languageTags
	 *            List of language tags used to filter containers which should be included in the traversal
	 * @param type
	 *            Optional type of the node containers to filter by
	 * @return Traversal which can be used to locate the nodes
	 */
	protected VertexTraversal<?, ?, ?> getTaggedNodesTraversal(Branch branch, List<String> languageTags, ContainerType type) {

		EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> traversal = TagEdgeImpl.getNodeTraversal(this, branch).mark().outE(
			HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid());

		if (type != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		}

		traversal = GraphFieldContainerEdgeImpl.filterLanguages(traversal, languageTags);
		return traversal.outV().back();
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		TagDaoWrapper tagDao = Tx.get().data().tagDao();
		return tagDao.update(this, ac, batch);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		TagDaoWrapper tagRoot = mesh().boot().tagDao();
		return tagRoot.getSubETag(this, ac);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getTagFamily().getUuid() + "/tags/" + getUuid();
	}

	@Override
	public User getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	protected TagMeshEventModel createEvent(MeshEvent type) {
		TagMeshEventModel event = new TagMeshEventModel();
		event.setEvent(type);
		fillEventInfo(event);

		// .project
		Project project = getProject();
		ProjectReference reference = project.transformToReference();
		event.setProject(reference);

		// .tagFamily
		TagFamily tagFamily = getTagFamily();
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

}
