package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;

import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.cli.ODBBootstrapInitializer;
import org.apache.commons.lang3.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.TagEdgeImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see TagRoot
 */
public class TagRootImpl extends AbstractRootVertex<Tag> implements TagRoot {

	/**
	 * Initialise the indices and type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagRootImpl.class, MeshVertexImpl.class);
		// TODO why was the branch key omitted? TagEdgeImpl.BRANCH_UUID_KEY
		index.createIndex(edgeIndex(HAS_TAG));
		index.createIndex(edgeIndex(HAS_TAG).withInOut().withOut());
	}

	private static final Logger log = LoggerFactory.getLogger(TagRootImpl.class);

	@Override
	public Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_TAG;
	}

	@Override
	public void addTag(HibTag tag) {
		addItem(toGraph(tag));
	}

	@Override
	public void removeTag(HibTag tag) {
		removeItem(toGraph(tag));
	}

	@Override
	public long globalCount() {
		return db().count(TagImpl.class);
	}

	@Override
	public Tag findByName(String name) {
		return out(getRootLabel()).mark().has(TagImpl.TAG_VALUE_KEY, name).back().nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public void delete(BulkActionContext bac) {
		// TODO add check to prevent deletion of MeshRoot.tagRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag root {" + getUuid() + "}");
		}
		// Delete all the tags of the tag root
		for (Tag tag : findAll()) {
			tag.delete(bac);
		}
		// Now delete the tag root element
		getElement().remove();
		bac.process();
	}

	@Override
	public HibTag create(String name, HibProject project, HibTagFamily tagFamily, HibUser creator) {
		Tag tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);
		addTag(tag);

		// Add to global list of tags
		ODBBootstrapInitializer boot = (ODBBootstrapInitializer) mesh().boot();
		TagRoot globalTagRoot = boot.tagRoot();
		if (this != globalTagRoot) {
			globalTagRoot.addTag(tag);
		}

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);
		tag.generateBucketId();
		return tag;
	}

	@Override
	public Tag create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new NotImplementedException("The tag family is the root element thus should be used for creation of tags.");
	}

	@Override
	public boolean update(Tag element, InternalActionContext ac, EventQueueBatch batch) {
		return super.update(element, ac, batch);
	}

	@Override
	public void delete(Tag element, BulkActionContext bac) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public TagResponse transformToRestSync(Tag element, InternalActionContext ac, int level, String... languageTags) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public Page<? extends Node> findTaggedNodes(Tag tag, HibUser user, Branch branch, List<String> languageTags, ContainerType type,
		PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = getTaggedNodesTraversal(tag, branch, languageTags, type);
		return new DynamicTransformablePageImpl<Node>(user, traversal, pagingInfo, READ_PUBLISHED_PERM, NodeImpl.class);
	}

	@Override
	public Result<? extends Node> findTaggedNodes(HibTag tag, InternalActionContext ac) {
		HibUser user = ac.getUser();
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac);
		String branchUuid = branch.getUuid();
		UserDao userRoot = Tx.get().userDao();
		TraversalResult<? extends Node> nodes = new TraversalResult<>(toGraph(tag).inE(HAS_TAG).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).outV().frameExplicit(NodeImpl.class));
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

	@Override
	public Result<? extends Node> getNodes(Tag tag, HibBranch branch) {
		Iterable<? extends NodeImpl> it = TagEdgeImpl.getNodeTraversal(tag, branch).frameExplicit(NodeImpl.class);
		return new TraversalResult<>(it);
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
	private VertexTraversal<?, ?, ?> getTaggedNodesTraversal(Tag tag, Branch branch, List<String> languageTags, ContainerType type) {

		EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> traversal = TagEdgeImpl.getNodeTraversal(tag, branch).mark().outE(
			HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid());

		if (type != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		}

		traversal = GraphFieldContainerEdgeImpl.filterLanguages(traversal, languageTags);
		return traversal.outV().back();
	}

	@Override
	public HibTag create(HibTagFamily tagFamily, String name, HibProject project, HibUser creator, String uuid) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		if (uuid != null) {
			tag.setUuid(uuid);
		}
		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);
		tag.generateBucketId();

		ODBBootstrapInitializer boot = (ODBBootstrapInitializer) mesh().boot();
		// Add the tag to the global tag root
		boot.meshRoot().getTagRoot().addTag(tag);
		// And to the tag family
		toGraph(tagFamily).addTag(tag);

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);
		return tag;

	}

}
