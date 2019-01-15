package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.Events.EVENT_NODE_CREATED;
import static com.gentics.mesh.Events.EVENT_NODE_DELETED;
import static com.gentics.mesh.Events.EVENT_NODE_UPDATED;
import static com.gentics.mesh.core.data.ContainerType.DRAFT;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.syncleus.ferma.EdgeFrame;

import io.reactivex.Single;

/**
 * The Node Domain Model interface.
 * 
 * A node is the main building block for project structures. Each project has one base node which is basically a folder. Additional child nodes can be added to
 * this node and to the created nodes in order to create a project data structure. Each node may be linked to one or more {@link NodeGraphFieldContainer}
 * vertices which contain the language specific data.
 */
public interface Node extends MeshCoreVertex<NodeResponse, Node>, CreatorTrackingVertex, IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	static final String TYPE = "node";

	String RELEASE_UUID_KEY = "releaseUuid";

	static final TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_NODE_CREATED, EVENT_NODE_UPDATED, EVENT_NODE_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Maximum depth for transformations: {@value #MAX_TRANSFORMATION_LEVEL}
	 */
	public static final int MAX_TRANSFORMATION_LEVEL = 3;

	/**
	 * Add the given tag to the list of tags for this node in the given release.
	 * 
	 * @param tag
	 * @param release
	 */
	void addTag(Tag tag, Release release);

	/**
	 * Remove the given tag from the list of tags for this node in the given release.
	 * 
	 * @param tag
	 * @param release
	 */
	void removeTag(Tag tag, Release release);

	/**
	 * Remove all tags for the given release.
	 * 
	 * @param release
	 */
	void removeAllTags(Release release);

	/**
	 * Return a list of all tags that were assigned to this node in the given release.
	 *
	 * @param release
	 * @return
	 */
	List<? extends Tag> getTags(Release release);

	/**
	 * Return a page of all visible tags that are assigned to the node.
	 * 
	 * @param user
	 * @param params
	 * @param release
	 * @return Page which contains the result
	 */
	TransformablePage<? extends Tag> getTags(User user, PagingParameters params, Release release);

	/**
	 * Return the draft field container for the given language in the latest release.
	 * 
	 * @param language
	 * @return
	 */
	NodeGraphFieldContainer getLatestDraftFieldContainer(Language language);

	/**
	 * Return the field container for the given language, type and release.
	 * 
	 * @param language
	 * @param release
	 * @param type
	 *            type
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Language language, Release release, ContainerType type);

	/**
	 * Return the draft field container for the given language in the latest release.
	 * 
	 * @param languageTag
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(String languageTag);

	/**
	 * Return the field container for the given language, type and release Uuid.
	 * 
	 * @param languageTag
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(String languageTag, String releaseUuid, ContainerType type);

	/**
	 * Create a new graph field container for the given language and assign the schema version of the release to the container. The graph field container will
	 * be the (only) DRAFT version for the language/release. If this is the first container for the language, it will also be the INITIAL version. Otherwise the
	 * container will be a clone of the last draft and will have the next version number.
	 * 
	 * @param language
	 * @param release
	 *            release
	 * @param user
	 *            user
	 * @return
	 */
	NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User user);

	/**
	 * Like {@link #createGraphFieldContainer(Language, Release, User)}, but let the new graph field container be a clone of the given original (if not null).
	 * 
	 * @param language
	 * @param release
	 * @param editor
	 *            User which will be set as editor
	 * @param original
	 *            Container to be used as a source for the new container
	 * @param handleDraftEdge
	 *            Whether to move the existing draft edge or create a new draft edge to the new container
	 * @return Created container
	 */
	NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User editor, NodeGraphFieldContainer original,
		boolean handleDraftEdge);

	/**
	 * Return the draft field containers of the node in the latest release.
	 * 
	 * @return
	 */
	default Iterable<? extends NodeGraphFieldContainer> getDraftGraphFieldContainers() {
		// FIX ME: We should not rely on specific releases.
		return getGraphFieldContainersIt(getProject().getLatestRelease(), DRAFT);
	}

	/**
	 * Return a list of all initial graph field containers for the node (in any release).
	 *
	 * @return
	 * @deprecated A new method should be used since loading lists is expensive
	 */
	@Deprecated
	List<? extends NodeGraphFieldContainer> getAllInitialGraphFieldContainers();

	/**
	 * Return a list of graph field containers of given type for the node in the given release.
	 *
	 * @param release
	 * @param type
	 * @return
	 * @deprecated A new method should be used since loading lists is expensive
	 */
	@Deprecated
	default List<? extends NodeGraphFieldContainer> getGraphFieldContainers(Release release, ContainerType type) {
		return getGraphFieldContainers(release.getUuid(), type);
	}

	/**
	 * Return a list of graph field containers of given type for the node in the given release.
	 *
	 * @param releaseUuid
	 * @param type
	 * @return
	 * @deprecated A new method should be used since loading lists is expensive. Use {@link #getGraphFieldContainersIt(String, ContainerType)}
	 */
	@Deprecated
	List<? extends NodeGraphFieldContainer> getGraphFieldContainers(String releaseUuid, ContainerType type);

	/**
	 * Return containers of the given type
	 * 
	 * @param type
	 * @return
	 */
	Iterable<? extends NodeGraphFieldContainer> getGraphFieldContainersIt(ContainerType type);

	/**
	 * Return containers of the given type and release.
	 * 
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	Iterable<? extends NodeGraphFieldContainer> getGraphFieldContainersIt(String releaseUuid, ContainerType type);

	/**
	 * Return containers of the given type and release.
	 * 
	 * @param release
	 * @param type
	 * @return
	 */
	default Iterable<? extends NodeGraphFieldContainer> getGraphFieldContainersIt(Release release, ContainerType type) {
		return getGraphFieldContainersIt(release.getUuid(), type);
	}

	/**
	 * Return the number of field containers of the node of type DRAFT or PUBLISHED in any release.
	 *
	 * @return
	 */
	long getGraphFieldContainerCount();

	/**
	 * Return a list of language names for draft versions in the latest release
	 * 
	 * @return
	 */
	List<String> getAvailableLanguageNames();

	/**
	 * Return a list of language names for versions of given type in the given release.
	 * 
	 * @param release
	 *            release
	 * @param type
	 *            container version type
	 * @return
	 */
	List<String> getAvailableLanguageNames(Release release, ContainerType type);

	/**
	 * Return the project of the node.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Set the project of the node.
	 * 
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Return the children for this node for all releases.
	 * 
	 * @return
	 */
	Iterable<Node> getChildren();

	/**
	 * Return the children for this node in the given release.
	 * 
	 * @param releaseUuid
	 * @return
	 */
	Iterable<Node> getChildren(String releaseUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided release and also checks permissions.
	 */
	Stream<Node> getChildrenStream(InternalActionContext ac);

	/**
	 * Return the list of children for this node, that the given user has read permission for. Filter by the provides information.
	 *
	 * @param requestUser
	 *            user
	 * @param releaseUuid
	 *            release Uuid
	 * @param languageTags
	 * @param type
	 *            edge type
	 * @return
	 */
	List<? extends Node> getChildren(MeshAuthUser requestUser, String releaseUuid, List<String> languageTags, ContainerType type);

	/**
	 * Returns the parent node of this node.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return
	 */
	Node getParentNode(String releaseUuid);

	/**
	 * Set the parent node of this node.
	 * 
	 * @param releaseUuid
	 * @param parentNode
	 */
	void setParentNode(String releaseUuid, Node parentNode);

	/**
	 * Create a child node in this node in the latest release of the project.
	 * 
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @return
	 */
	Node create(User creator, SchemaContainerVersion schemaVersion, Project project);

	/**
	 * Create a child node in this node in the given release
	 * 
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @param release
	 * @return
	 */
	default Node create(User creator, SchemaContainerVersion schemaVersion, Project project, Release release) {
		return create(creator, schemaVersion, project, release, null);
	}

	/**
	 * Create a child node in this node in the given release
	 * 
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @param release
	 * @param uuid
	 * @return
	 */
	Node create(User creator, SchemaContainerVersion schemaVersion, Project project, Release release, String uuid);

	/**
	 * Return a page with child nodes that are visible to the given user.
	 * 
	 * @param ac
	 *            Context of the operation
	 * @param languageTags
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @param pagingParameter
	 * @return
	 */
	TransformablePage<? extends Node> getChildren(InternalActionContext ac, List<String> languageTags, String releaseUuid, ContainerType type,
		PagingParameters pagingParameter);

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 * 
	 * @param ac
	 * @return
	 */
	String getDisplayName(InternalActionContext ac);

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter. When a user requests a node using ?lang=de,en and there
	 * is no de version the en version will be selected and returned.
	 * 
	 * @param languageTags
	 * @param releaseUuid
	 *            release Uuid
	 * @param version
	 *            requested version. This must either be "draft" or "published" or a version number with pattern [major.minor]
	 * @return Next matching field container or null when no language matches
	 */
	NodeGraphFieldContainer findVersion(List<String> languageTags, String releaseUuid, String version);

	/**
	 * Iterate the version chain from the back in order to find the given version.
	 * 
	 * @param languageTag
	 * @param releaseUuid
	 * @param version
	 * @return Found version or null when no version could be found.
	 */
	default NodeGraphFieldContainer findVersion(String languageTag, String releaseUuid, String version) {
		return findVersion(Arrays.asList(languageTag), releaseUuid, version);
	}

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter.
	 * 
	 * @param ac
	 * @param languageTags
	 * @return Next matching field container or null when no language matches
	 */
	default NodeGraphFieldContainer findVersion(InternalActionContext ac, List<String> languageTags) {
		return findVersion(languageTags, ac.getRelease().getUuid(), ac.getVersioningParameters().getVersion());
	}

	/**
	 * Move this node into the target node.
	 * 
	 * @param ac
	 * @param targetNode
	 * @param batch
	 */
	void moveTo(InternalActionContext ac, Node targetNode, SearchQueueBatch batch);

	/**
	 * Transform the node into a node reference rest model.
	 * 
	 * @param ac
	 */
	NodeReference transformToReference(InternalActionContext ac);

	/**
	 * Transform the node into a navigation response rest model.
	 * 
	 * @param ac
	 * @return
	 */
	Single<NavigationResponse> transformToNavigation(InternalActionContext ac);

	/**
	 * Transform the node into a publish status response rest model.
	 *
	 * @param ac
	 * @return
	 */
	PublishStatusResponse transformToPublishStatus(InternalActionContext ac);

	/**
	 * Publish the node (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void publish(InternalActionContext ac, BulkActionContext bac);

	/**
	 * Take the node offline (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void takeOffline(InternalActionContext ac, BulkActionContext bac);

	/**
	 * Take the node offline.
	 * 
	 * @param ac
	 * @param bac
	 * @param release
	 * @param parameters
	 * @return
	 */
	void takeOffline(InternalActionContext ac, BulkActionContext bac, Release release, PublishParameters parameters);

	/**
	 * Transform the node language into a publish status response rest model.
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	PublishStatusModel transformToPublishStatus(InternalActionContext ac, String languageTag);

	/**
	 * Publish a language of the node
	 *
	 * @param ac
	 * @param bac
	 * @param languageTag
	 * @return
	 */
	void publish(InternalActionContext ac, BulkActionContext bac, String languageTag);

	/**
	 * Set the graph field container to be the (only) published for the given release
	 *
	 * @param container
	 * @param releaseUuid
	 */
	void setPublished(NodeGraphFieldContainer container, String releaseUuid);

	/**
	 * Take a language of the node offline.
	 *
	 * @param ac
	 * @param bac
	 * @param release
	 * @param languageTag
	 */
	void takeOffline(InternalActionContext ac, BulkActionContext bac, Release release, String languageTag);

	/**
	 * Delete the language container for the given language from the release. This will remove all PUBLISHED, DRAFT and INITIAL edges to GFCs for the language
	 * and release, and will then delete all "dangling" GFC (GFCs, which are not used by another release).
	 * 
	 * @param ac
	 * @param release
	 * @param language
	 *            Language which will be used to find the field container which should be deleted
	 * @param context
	 * @param failForLastContainer
	 *            Whether to execute the last container check and fail or not.
	 */
	void deleteLanguageContainer(InternalActionContext ac, Release release, Language language, BulkActionContext context,
		boolean failForLastContainer);

	/**
	 * Resolve the given path and return the path object that contains the resolved nodes.
	 *
	 * @param releaseUuid
	 * @param type
	 *            edge type
	 * @param nodePath
	 * @param pathStack
	 * @return
	 */
	Path resolvePath(String releaseUuid, ContainerType type, Path nodePath, Stack<String> pathStack);

	/**
	 * Check whether the node provides the given segment for any language or binary attribute filename return the segment information.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @param segment
	 * 
	 * @return Segment information or null if this node is not providing the given segment
	 */
	PathSegment getSegment(String releaseUuid, ContainerType type, String segment);

	/**
	 * Return the webroot path to the node in the given language. If more than one language is given, the path will lead to the first available language of the
	 * node.
	 * 
	 * @param ac
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 * 
	 * @return
	 */
	String getPath(ActionContext ac, String releaseUuid, ContainerType type, String... languageTag);

	/**
	 * Return the path segment value of this node in the given language. If more than one language is given, the path will lead to the first available language
	 * of the node.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 *
	 * @return
	 */
	String getPathSegment(String releaseUuid, ContainerType type, String... languageTag);

	/**
	 * Update the path segment and increment any found postfix number.
	 * 
	 * @param releaseUuid
	 * @param type
	 * @param languageTag
	 */
	void postfixPathSegment(String releaseUuid, ContainerType type, String languageTag);

	/**
	 * Delete the node from the given release. This will also delete children from the release.
	 * 
	 * If the node is deleted from its last release, it is (permanently) deleted from the db
	 *
	 * @param ac
	 * @param release
	 * @param bac
	 * @param ignoreChecks
	 */
	void deleteFromRelease(InternalActionContext ac, Release release, BulkActionContext bac, boolean ignoreChecks);

	/**
	 * Return the schema container for the node.
	 * 
	 * @return
	 */
	SchemaContainer getSchemaContainer();

	/**
	 * Set the schema container of the node.
	 * 
	 * @param container
	 */
	void setSchemaContainer(SchemaContainer container);

	/**
	 * Check the publish consistency by validating the following constraints:
	 * 
	 * <ul>
	 * <li>A node can only be published if all parent nodes are also published (within the scope of the release)
	 * <li>A published node can only be moved if the target node is also a published node.
	 * <li>A node can only be taken offline if the node has no children which are still published.
	 * </ul>
	 * 
	 * @param ac
	 *            Current action context
	 * @param release
	 *            Release to be used to check the consistency state
	 */
	void assertPublishConsistency(InternalActionContext ac, Release release);

	/**
	 * Create a new published version of the given language in the release.
	 * 
	 * @param language
	 *            language
	 * @param release
	 *            release
	 * @param user
	 *            user
	 * @return published field container
	 */
	NodeGraphFieldContainer publish(Language language, Release release, User user);

	/**
	 * Publish the node for the specified release.
	 * 
	 * @param ac
	 * @param release
	 * @param bac
	 * @return
	 */
	void publish(InternalActionContext ac, Release release, BulkActionContext bac);

	/**
	 * Transform the node into a node list item.
	 * 
	 * @param ac
	 * @param languageTags
	 * @return
	 */
	NodeFieldListItem toListItem(InternalActionContext ac, String[] languageTags);

	/**
	 * Delete the node. Please use {@link #deleteFromRelease(Release, BulkActionContext)} if you want to delete the node just from a specific release.
	 * 
	 * @param bac
	 * @param ignoreChecks
	 */
	void delete(BulkActionContext bac, boolean ignoreChecks);

	/**
	 * Handle the update tags request.
	 * 
	 * @param ac
	 * @param batch
	 * @return Page which includes the new set of tags
	 */
	TransformablePage<? extends Tag> updateTags(InternalActionContext ac, SearchQueueBatch batch);

	/**
	 * Return a map with language tags and resolved link types
	 * 
	 * @param ac
	 * @param linkType
	 * @param release
	 * @return
	 */
	Map<String, String> getLanguagePaths(InternalActionContext ac, LinkType linkType, Release release);

	/**
	 * Return the breadcrumb nodes.
	 * 
	 * @param ac
	 * @return Deque with breadcrumb nodes
	 */
	Deque<Node> getBreadcrumbNodes(InternalActionContext ac);

	/**
	 * Handle the node specific on deleted event.
	 * 
	 * @param uuid
	 * @param name
	 * @param schema
	 * @param languageTag
	 */
	void onDeleted(String uuid, String name, SchemaContainer schema, String languageTag);

	/**
	 * Get an existing edge.
	 * 
	 * @param languageTag
	 *            language tag
	 * @param releaseUuid
	 *            release uuid
	 * @param type
	 *            edge type
	 * @return existing edge or null
	 */
	EdgeFrame getGraphFieldContainerEdge(String languageTag, String releaseUuid, ContainerType type);

	/**
	 * Fully delete the node. Not that this delete operation will delete all versions of the node and also the node itself (from all branches).
	 * 
	 * @param bac
	 * @param recursive
	 */
	void deleteFully(BulkActionContext bac, boolean recursive);

}
