package com.gentics.mesh.core.data.node;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Stack;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.InvalidArgumentException;

import rx.Completable;
import rx.Single;

/**
 * The Node Domain Model interface.
 * 
 * A node is the main building block for project structures. Each project has one base node which is basically a folder. Additional child nodes can be added to
 * this node and to the created nodes in order to create a project data structure. Each node may be linked to one or more {@link NodeGraphFieldContainer}
 * vertices which contain the language specific data.
 * 
 */
public interface Node extends MeshCoreVertex<NodeResponse, Node>, CreatorTrackingVertex {

	public static final String TYPE = "node";
	public static final int MAX_TRANSFORMATION_LEVEL = 2;

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
	 * Return a list of tags that were assigned to this node in the given release
	 *
	 * @param release
	 * @return
	 */
	List<? extends Tag> getTags(Release release);

	/**
	 * Return the draft field container for the given language in the latest release
	 * 
	 * @param language
	 * @return
	 */
	NodeGraphFieldContainer getLatestDraftFieldContainer(Language language);

	/**
	 * Return the field container for the given language, type and release
	 * 
	 * @param language
	 * @param release
	 * @param type
	 *            type
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Language language, Release release, ContainerType type);

	/**
	 * Return the draft field container for the given language in the latest release
	 * 
	 * @param languageTag
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(String languageTag);

	/**
	 * Return the field container for the given language, type and release Uuid
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
	 * container will be a clone of the last draft and will have the next version number
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
	 * Like {@link #createGraphFieldContainer(Language, Release, User)}, but let the new graph field container be a clone of the given original (if not null)
	 * 
	 * @param language
	 * @param release
	 * @param user
	 * @param original
	 * @return
	 */
	NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User user, NodeGraphFieldContainer original);

	/**
	 * Return a list of draft graph field containers for the node in the latest release
	 * 
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getGraphFieldContainers();

	/**
	 * Return a list of all initial graph field containers for the node (in any release)
	 *
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getAllInitialGraphFieldContainers();

	/**
	 * Return a list of graph field containers of given type for the node in the given release
	 *
	 * @param release
	 * @param type
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getGraphFieldContainers(Release release, ContainerType type);

	/**
	 * Return a list of graph field containers of given type for the node in the given release
	 *
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getGraphFieldContainers(String releaseUuid, ContainerType type);

	/**
	 * Return the number of field containers of the node of type DRAFT or PUBLISHED in any release
	 *
	 * @return
	 */
	long getGraphFieldContainerCount();

	/**
	 * Return a page of tags that are assigned to the node.
	 * 
	 * @param release
	 *            release
	 * @param params
	 * 
	 * @return
	 * @throws InvalidArgumentException
	 */
	PageImpl<? extends Tag> getTags(Release release, PagingParameters params) throws InvalidArgumentException;

	/**
	 * Return a list of language names for draft versions in the latest release
	 * 
	 * @return
	 */
	List<String> getAvailableLanguageNames();

	/**
	 * Return a list of language names for versions of given type in the given release
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
	 * Return the list of children for this node for all releases
	 * 
	 * @return
	 */
	List<? extends Node> getChildren();

	/**
	 * Return the list of children for this node in the given release
	 * 
	 * @param releaseUuid
	 * @return
	 */
	List<? extends Node> getChildren(String releaseUuid);

	/**
	 * Return the list of children for this node, that the given user has read permission for
	 *
	 * @param requestUser
	 *            user
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @return
	 */
	List<? extends Node> getChildren(MeshAuthUser requestUser, String releaseUuid, ContainerType type);

	/**
	 * Returns the parent node of this node.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return
	 */
	Node getParentNode(String releaseUuid);

	/**
	 * Set the parent node of this node
	 * 
	 * @param releaseUuid
	 * @param parentNode
	 */
	void setParentNode(String releaseUuid, Node parentNode);

	/**
	 * Create a child node in this node in the latest release of the project
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
	 *            qparam release
	 * @return
	 */
	Node create(User creator, SchemaContainerVersion schemaVersion, Project project, Release release);

	/**
	 * Return a page with child nodes that are visible to the given user.
	 * 
	 * @param requestUser
	 * @param languageTags
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @param pagingParameter
	 * @return
	 * @throws InvalidArgumentException
	 */
	PageImpl<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, String releaseUuid, ContainerType type,
			PagingParameters pagingParameter) throws InvalidArgumentException;

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 * 
	 * @param ac
	 * @return
	 */
	String getDisplayName(InternalActionContext ac);

	/**
	 * Find a node field container that matches the nearest possible value for the ?lang= request parameter. When a user requests a node using ?lang=de,en and
	 * there is no de version the en version will be selected and returned.
	 * 
	 * @param languageTags
	 * @param releaseUuid
	 *            release Uuid
	 * @param version
	 *            requested version. This must either be "draft" or "published" or a version number with pattern [major.minor]
	 * @return Next matching field container or null when no language matched
	 */
	NodeGraphFieldContainer findNextMatchingFieldContainer(List<String> languageTags, String releaseUuid, String version);

	/**
	 * Move this node into the target node.
	 * 
	 * @param ac
	 * @param targetNode
	 * @return
	 */
	Completable moveTo(InternalActionContext ac, Node targetNode);

	/**
	 * Transform the node into a node reference rest model.
	 * 
	 * @param ac
	 */
	Single<NodeReferenceImpl> transformToReference(InternalActionContext ac);

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
	Single<PublishStatusResponse> transformToPublishStatus(InternalActionContext ac);

	/**
	 * Publish the node (all languages)
	 *
	 * @param ac
	 * @return
	 */
	Completable publish(InternalActionContext ac);

	/**
	 * Take the node offline (all languages)
	 *
	 * @param ac
	 * @return
	 */
	Completable takeOffline(InternalActionContext ac);

	/**
	 * Transform the node language into a publish status response rest model.
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	Single<PublishStatusModel> transformToPublishStatus(InternalActionContext ac, String languageTag);

	/**
	 * Publish a language of the node
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	Completable publish(InternalActionContext ac, String languageTag);

	/**
	 * Set the graph field container to be the (only) published for the given release
	 *
	 * @param container
	 * @param releaseUuid
	 */
	void setPublished(NodeGraphFieldContainer container, String releaseUuid);

	/**
	 * Take a language of the node offline
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	Completable takeOffline(InternalActionContext ac, String languageTag);

	/**
	 * Delete the language container for the given language from the release This will not actually delete the container, but will remove the DRAFT and
	 * PUBLISHED edge to the container for the release
	 * 
	 * @param release
	 * @param language
	 *            Language which will be used to find the field container which should be deleted
	 * @param batch
	 */
	void deleteLanguageContainer(Release release, Language language, SearchQueueBatch batch);

	/**
	 * Return the path segment of this node.
	 * 
	 * @return
	 */
	Single<String> getPathSegment(InternalActionContext ac);

	/**
	 * Return the full path to this node.
	 * 
	 * @param ac
	 * @return
	 */
	Single<String> getPath(InternalActionContext ac);

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
	Single<Path> resolvePath(String releaseUuid, ContainerType type, Path nodePath, Stack<String> pathStack);

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
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 * 
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	Single<String> getPath(String releaseUuid, ContainerType type, String... languageTag) throws UnsupportedEncodingException;

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
	Single<String> getPathSegment(String releaseUuid, ContainerType type, String... languageTag);

	/**
	 * Delete the node and ignore any checks.
	 * 
	 * @param ignoreChecks
	 * @param batch
	 */
	void delete(boolean ignoreChecks, SearchQueueBatch batch);

	/**
	 * Delete the node from the given release. This will also delete children from the release.
	 * 
	 * If the node is deleted from its last release, it is (permanently) deleted from the db
	 *
	 * @param release
	 * @param batch
	 */
	void deleteFromRelease(Release release, SearchQueueBatch batch);

	/**
	 * Set the breadcrumb information to the given rest node.
	 * 
	 * @param ac
	 * @param restNode
	 * @return
	 */
	Completable setBreadcrumbToRest(InternalActionContext ac, NodeResponse restNode);

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
	 */
	void assertPublishConsistency(InternalActionContext ac);

	/**
	 * Create a new published version of the given language in the release
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

}
