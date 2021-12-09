package com.gentics.mesh.core.data.node;

import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.syncleus.ferma.EdgeFrame;

/**
 * The Node Domain Model interface.
 *
 * A node is the main building block for project structures. Each project has one base node which is basically a folder. Additional child nodes can be added to
 * this node and to the created nodes in order to create a project data structure. Each node may be linked to one or more {@link NodeGraphFieldContainer}
 * vertices which contain the language specific data.
 */
public interface Node extends MeshCoreVertex<NodeResponse>, CreatorTrackingVertex, ProjectElement, HibNode, GraphDBBucketableElement {

	String BRANCH_UUID_KEY = "branchUuid";

	@Override
	default boolean hasPublishPermissions() {
		return true;
	}

	Result<HibNode> getChildren();

	Result<HibNode> getChildren(String branchUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided branch and also checks permissions.
	 */
	Stream<Node> getChildrenStream(InternalActionContext ac);

	/**
	 * Gets all NodeGraphField edges that reference this node.
	 * @return
	 */
	Stream<HibNodeField> getInboundReferences();

	/**
	 * Get an existing edge.
	 *
	 * @param languageTag
	 *            language tag
	 * @param branchUuid
	 *            branch uuid
	 * @param type
	 *            edge type
	 * @return existing edge or null
	 */
	EdgeFrame getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type);

	/**
	 * Return a page of all visible tags that are assigned to the node.
	 *
	 * @param user
	 * @param params
	 * @param branch
	 * @return Page which contains the result
	 */
	Page<? extends HibTag> getTags(HibUser user, PagingParameters params, HibBranch branch);

	void assertPublishConsistency(InternalActionContext ac, HibBranch branch);

	/**
	 * Tests if the node is tagged with the given tag.
	 *
	 * @param tag
	 * @param branch
	 * @return
	 */
	boolean hasTag(HibTag tag, HibBranch branch);

	/**
	 * Remove the element.
	 */
	void removeElement();

	/**
	 * Create a child node in this node in the latest branch of the project.
	 *
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @return
	 */
	HibNode create(HibUser creator, HibSchemaVersion schemaVersion, HibProject project);

	/**
	 * Adds reference update events to the context for all draft and published contents that reference this node.
	 *
	 * @param bac
	 */
	void addReferenceUpdates(BulkActionContext bac);

	void removeParent(String branchUuid);

	/**
	 * Return a page with child nodes that are visible to the given user.
	 *
	 * @param ac
	 *            Context of the operation
	 * @param languageTags
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param pagingParameter
	 * @return
	 */
	Page<HibNode> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingParameter);

	/**
	 * Create a child node in this node in the given branch
	 *
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @param branch
	 * @param uuid
	 * @return
	 */
	HibNode create(HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid);


	/**
	 * Remove published edges for each container found
	 *
	 * @param branchUuid
	 * @param bac
	 */
	void removePublishedEdges(String branchUuid, BulkActionContext bac);

	/**
	 * Resolve the given path and return the path object that contains the resolved nodes.
	 *
	 * @param branchUuid
	 * @param type
	 *            edge type
	 * @param nodePath
	 * @param pathStack
	 * @return
	 */
	Path resolvePath(String branchUuid, ContainerType type, Path nodePath, Stack<String> pathStack);

	/**
	 * Return the path segment value of this node preferable in the given language.
	 *
	 * If more than one language is given, the path will lead to the first available language
	 * of the node.
	 *
	 * When no language matches and <code>anyLanguage</code> is <code>true</code> the results language
	 * is nondeterministic.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param anyLanguage
	 *            whether to return the path segment value of this node in any language, when none in <code>langaugeTag</code> match
	 * @param languageTag
	 *
	 * @return
	 */
	String getPathSegment(String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag);

	/**
	 * Return the path segment value of this node in the given language. If more than one language is given, the path will lead to the first available language
	 * of the node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 *
	 * @return
	 */
	default String getPathSegment(String branchUuid, ContainerType type, String... languageTag) {
		return getPathSegment(branchUuid, type, false, languageTag);
	}

	/**
	 * Check whether the node is visible in the given branch (that means has at least one DRAFT graphfieldcontainer in the branch)
	 *
	 * @param branchUuid
	 *            branch uuid
	 * @return true if the node is visible in the branch
	 */
	boolean isVisibleInBranch(String branchUuid);

	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	HibNodeFieldContainer getLatestDraftFieldContainer(String languageTag);

	/**
	 * Return containers of the given type
	 *
	 * @param type
	 * @return
	 */
	Result<HibNodeFieldContainer> getFieldContainers(ContainerType type);

	/**
	 * Return the field container for the given language, type and branch.
	 *
	 * @param languageTag
	 * @param branch
	 * @param type
	 *            type
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(String languageTag, HibBranch branch, ContainerType type);

    /**
     * Return traversal of graph field containers of given type for the node in the given branch.
     *
     * @param branchUuid
     * @param type
     * @return
     */
    Result<HibNodeFieldContainer> getFieldContainers(String branchUuid, ContainerType type);

	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(String languageTag);

	/**
	 * Return the field container for the given language, type and branch Uuid.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(String languageTag, String branchUuid, ContainerType type);

	/**
	 * Return the number of field containers of the node of type DRAFT or PUBLISHED in any branch.
	 *
	 * @return
	 */
	long getFieldContainerCount();

	/**
	 * Remove all edges to field container with type {@link ContainerType#INITIAL} for the specified branch uuid
	 * @param initial
	 * @param branchUUID
	 */
	void removeInitialFieldContainerEdge(HibNodeFieldContainer initial, String branchUUID);

	/**
	 * Remove the published edge for the given language tag and branch UUID
	 *
	 * @param languageTag
	 * @param branchUuid
	 */
    void removePublishedEdge(String languageTag, String branchUuid);

    /**
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(InternalActionContext ac, HibNodeFieldContainer container, String branchUuid);
}
