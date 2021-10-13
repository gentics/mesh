package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.DateUtils;

/**
 * Dao for {@link HibNode}
 */
public interface NodeDao extends Dao<HibNode>, DaoTransformable<HibNode, NodeResponse>, RootDao<HibProject, HibNode> {
	/**
	 * Return the API path for the node.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibNode element, InternalActionContext ac);

	/**
	 * Create a child node in the latest branch of the project.
	 *
	 * @param parentNode
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @return
	 */
	HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project);

	/**
	 * Create a child node in the given branch
	 *
	 * @param parentNode
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @param branch
	 * @return
	 */
	default HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch) {
		return create(parentNode, creator, schemaVersion, project, branch, null);
	}

	/**
	 * Create a child node in the given branch
	 *
	 * @param parentNode
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @param branch
	 * @param uuid
	 * @return
	 */
	HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid);

	/**
	 * Return the children for this node for all branches.
	 *
	 * @return
	 */
	Result<? extends HibNode> getChildren(HibNode node);

	/**
	 * Return the children for this node in the given branch.
	 *
	 * @param branchUuid
	 * @return
	 */
	Result<? extends HibNode> getChildren(HibNode node, String branchUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided branch and also checks permissions.
	 */
	Stream<? extends HibNode> getChildrenStream(HibNode node, InternalActionContext ac);

	/**
	 * Returns the parent node of this node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	HibNode getParentNode(HibNode node, String branchUuid);

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
	Page<? extends HibNode> getChildren(HibNode node, InternalActionContext ac, List<String> languageTags, String branchUuid,
		ContainerType type,
		PagingParameters pagingParameter);

	/**
	 * Set the parent node of this node.
	 *
	 * @param branchUuid
	 * @param parentNode
	 */
	void setParentNode(HibNode node, String branchUuid, HibNode parentNode);

	/**
	 * Return a list of language names for draft versions in the latest branch
	 *
	 * @return
	 */
	List<String> getAvailableLanguageNames(HibNode node);

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 *
	 * @param ac
	 * @return
	 */
	String getDisplayName(HibNode node, InternalActionContext ac);

	/**
	 * Move this node into the target node.
	 *
	 * @param ac
	 * @param targetNode
	 * @param batch
	 */
	void moveTo(HibNode sourceNode, InternalActionContext ac, HibNode targetNode, EventQueueBatch batch);

	/**
	 * Transform the node into a navigation response rest model.
	 *
	 * @param ac
	 * @return
	 */
	NavigationResponse transformToNavigation(HibNode node, InternalActionContext ac);

	/**
	 * Transform the node into a publish status response rest model.
	 *
	 * @param ac
	 * @return
	 */
	PublishStatusResponse transformToPublishStatus(HibNode node, InternalActionContext ac);

	/**
	 * Publish the node (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void publish(HibNode node, InternalActionContext ac, BulkActionContext bac);

	/**
	 * Take the node offline (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac);

	/**
	 * Transform the node language into a publish status response rest model.
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	PublishStatusModel transformToPublishStatus(HibNode node, InternalActionContext ac, String languageTag);

	/**
	 * Publish a language of the node
	 *
	 * @param ac
	 * @param bac
	 * @param languageTag
	 * @return
	 */
	void publish(HibNode node, InternalActionContext ac, BulkActionContext bac, String languageTag);

	/**
	 * Take a language of the node offline.
	 *
	 * @param ac
	 * @param bac
	 * @param branch
	 * @param languageTag
	 */
	void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag);

	/**
	 * Return the webroot path to the node in the given language. If more than one language is given, the path will lead to the first available language of the
	 * node.
	 *
	 * @param ac
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 *
	 * @return
	 */
	String getPath(HibNode node, ActionContext ac, String branchUuid, ContainerType type, String... languageTag);

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
	Path resolvePath(HibNode baseNode, String branchUuid, ContainerType type, Path nodePath, Stack<String> pathStack);

	/**
	 * Delete the node. Please use {@link ContentDaoWrapper#deleteFromBranch(InternalActionContext, HibBranch, BulkActionContext, boolean)} if you want to
	 * delete the node just from a specific branch.
	 *
	 * @param bac
	 * @param ignoreChecks
	 * @param recursive
	 */
	void delete(HibNode node, BulkActionContext bac, boolean ignoreChecks, boolean recursive);

	/**
	 * Return the breadcrumb nodes.
	 *
	 * @param ac
	 * @return Deque with breadcrumb nodes
	 */
	Result<? extends HibNode> getBreadcrumbNodes(HibNode node, InternalActionContext ac);

	/**
	 * Check whether the node is the base node of its project
	 *
	 * @return true for base node
	 */
	boolean isBaseNode(HibNode node);

	/**
	 * Check whether the node is visible in the given branch (that means has at least one DRAFT graphfieldcontainer in the branch)
	 *
	 * @param branchUuid
	 *            branch uuid
	 * @return true if the node is visible in the branch
	 */
	boolean isVisibleInBranch(HibNode node, String branchUuid);

	/**
	 * Transform the node information to a version list response.
	 *
	 * @param ac
	 * @return Versions response
	 */
	NodeVersionsResponse transformToVersionList(HibNode node, InternalActionContext ac);

	/**
	 * Update the node.
	 * 
	 * @param node
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(HibNode node, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Update the tags of the node and return a page of updated tags.
	 * 
	 * @param node
	 * @param ac
	 * @param batch
	 * @return
	 */
	Page<? extends HibTag> updateTags(HibNode node, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Load a stream of nodes with the given perms in the project.
	 * 
	 * @param project
	 * @param ac
	 * @param perm
	 * @return
	 */
	Stream<? extends HibNode> findAllStream(HibProject project, InternalActionContext ac, InternalPermission perm);

	/**
	 * Return the count of nodes for the project.
	 * 
	 * @param project
	 * @return
	 */
	long computeCount(HibProject project);

	/**
	 * Create a new node.
	 * 
	 * @param project
	 * @param user
	 * @param version
	 * @return
	 */
	HibNode create(HibProject project, HibUser user, HibSchemaVersion version);
	
	/**
	 * Find the node by UUID globally.<br>
	 * <b>Attention: this method serves administration purposes. Don't use it for the node manipulation or general retrieval!</b><br>
	 * Use {@link NodeDao#findByUuid(HibProject, String)} with the valid project binding instead.
	 * 
	 * @param uuid
	 * @return
	 */
	HibNode findByUuidGlobal(String uuid);

	/**
	 * Count all the nodes globally.<br>
	 * <b>Attention: this method serves administration purposes!</b>
	 * 
	 * @param uuid
	 * @return
	 */
	long globalCount();

	/**
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(HibNode node, InternalActionContext ac, HibNodeFieldContainer container, String branchUuid);

	@Override
	default NodeResponse transformToRestSync(HibNode node, InternalActionContext ac, int level, String... languageTags) {
		Tx tx = Tx.get();
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		// Increment level for each node transformation to avoid stackoverflow situations
		level = level + 1;
		NodeResponse restNode = new NodeResponse();
		if (fields.has("uuid")) {
			restNode.setUuid(node.getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return restNode;
			}
		}

		HibSchema container = node.getSchemaContainer();
		if (container == null) {
			throw error(BAD_REQUEST, "The schema container for node {" + node.getUuid() + "} could not be found.");
		}
		HibBranch branch = tx.getBranch(ac, node.getProject());
		if (fields.has("languages")) {
			restNode.setAvailableLanguages(getLanguageInfo(node, ac));
		}

		setFields(node, ac, branch, restNode, level, fields, languageTags);

		if (fields.has("parent")) {
			setParentNodeInfo(node, ac, branch, restNode);
		}
		if (fields.has("perms")) {
			setRolePermissions(node, ac, restNode);
		}
		if (fields.has("children")) {
			setChildrenInfo(node, ac, branch, restNode);
		}
		if (fields.has("tags")) {
			setTagsToRest(node, ac, restNode, branch);
		}
		node.fillCommonRestFields(ac, fields, restNode);
		if (fields.has("breadcrumb")) {
			setBreadcrumbToRest(node, ac, restNode);
		}
		if (fields.has("path")) {
			setPathsToRest(node, ac, restNode, branch);
		}
		if (fields.has("project")) {
			setProjectReference(node, ac, restNode);
		}

		return restNode;
	}

	private void setBreadcrumbToRest(HibNode node, InternalActionContext ac, NodeResponse restNode) {
		List<NodeReference> breadcrumbs = getBreadcrumbNodeStream(node, ac)
			.map(node1 -> node1.transformToReference(ac))
			.collect(Collectors.toList());
		restNode.setBreadcrumb(breadcrumbs);
	}

	/**
	 * Stream the hierarchical pacth of the node. 
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	default Stream<HibNode> getBreadcrumbNodeStream(HibNode node, InternalActionContext ac) {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();

		String branchUuid = tx.getBranch(ac, node.getProject()).getUuid();
		HibNode current = node;

		Deque<HibNode> breadcrumb = new ArrayDeque<>();
		while (current != null) {
			breadcrumb.addFirst(current);
			current = nodeDao.getParentNode(current, branchUuid);
		}

		return breadcrumb.stream();
	}

	/**
	 * Get publish status for all languages of the node.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	default Map<String, PublishStatusModel> getLanguageInfo(HibNode node, InternalActionContext ac) {
		Map<String, PublishStatusModel> languages = new HashMap<>();
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac, node.getProject());

		node.getFieldContainers(branch, PUBLISHED).stream().forEach(c -> {

			String date = DateUtils.toISO8601(c.getLastEditedTimestamp(), 0);

			PublishStatusModel status = new PublishStatusModel();
			status.setPublished(true);
			status.setVersion(c.getVersion().toString());
			HibUser editor = c.getEditor();
			if (editor != null) {
				status.setPublisher(editor.transformToReference());
			}
			status.setPublishDate(date);
			languages.put(c.getLanguageTag(), status);
		});

		node.getFieldContainers(branch, DRAFT).stream().filter(c -> !languages.containsKey(c.getLanguageTag())).forEach(c -> {
			PublishStatusModel status = new PublishStatusModel().setPublished(false).setVersion(c.getVersion().toString());
			languages.put(c.getLanguageTag(), status);
		});
		return languages;
	}

	default HibNode loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		Tx tx = Tx.get();
		UserDao userDao = tx.userDao();
		ContentDao contentDao = tx.contentDao();

		HibNode element = findByUuidGlobal(uuid);
		if (!errorIfNotFound && element == null) {
			return null;
		}
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		HibUser requestUser = ac.getUser();
		if (perm == READ_PUBLISHED_PERM) {
			HibBranch branch = tx.getBranch(ac, element.getProject());

			List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList(Tx.get().data().options());
			HibNodeFieldContainer fieldContainer = contentDao.findVersion(element, requestedLanguageTags, branch.getUuid(),
				ac.getVersioningParameters().getVersion());

			if (fieldContainer == null) {
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_branch_language", uuid,
					String.join(",", requestedLanguageTags), branch.getUuid());
			}
			// Additionally check whether the read published permission could grant read
			// perm for published nodes
			boolean isPublished = fieldContainer.isPublished(branch.getUuid());
			if (isPublished && userDao.hasPermission(requestUser, element, READ_PUBLISHED_PERM)) {
				return element;
				// The container could be a draft. Check whether READ perm is granted.
			} else if (!isPublished && userDao.hasPermission(requestUser, element, READ_PERM)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", uuid, READ_PUBLISHED_PERM.getRestPerm().getName());
			}
		} else if (userDao.hasPermission(requestUser, element, perm)) {
			return element;
		}
		throw error(FORBIDDEN, "error_missing_perm", uuid, perm.getRestPerm().getName());
	}

	/**
	 * Set the project reference to the node response model.
	 *
	 * @param ac
	 * @param restNode
	 */
	private void setProjectReference(HibNode node, InternalActionContext ac, NodeResponse restNode) {
		restNode.setProject(node.getProject().transformToReference());
	}

	/**
	 * Set the parent node reference to the rest model.
	 *
	 * @param ac
	 * @param branch
	 *            Use the given branch to identify the branch specific parent node
	 * @param restNode
	 *            Model to be updated
	 * @return
	 */
	private void setParentNodeInfo(HibNode node, InternalActionContext ac, HibBranch branch, NodeResponse restNode) {
		HibNode parentNode = getParentNode(node, branch.getUuid());
		if (parentNode != null) {
			restNode.setParentNode(parentNode.transformToReference(ac));
		} else {
			// Only the base node of the project has no parent. Therefore this
			// node must be a container.
			restNode.setContainer(true);
		}
	}

	/**
	 * Set the node fields to the given rest model.
	 *
	 * @param ac
	 * @param branch
	 *            Branch which will be used to locate the correct field container
	 * @param restNode
	 *            Rest model which will be updated
	 * @param level
	 *            Current level of transformation
	 * @param fieldsSet
	 * @param languageTags
	 * @return
	 */
	private void setFields(HibNode node, InternalActionContext ac, HibBranch branch, NodeResponse restNode, int level, FieldsSet fieldsSet,
		String... languageTags) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		NodeParameters nodeParameters = ac.getNodeParameters();

		String[] langs = nodeParameters.getLanguages();
		if (langs != null) {
			for (String languageTag : langs) {
				HibLanguage language = Tx.get().languageDao().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(BAD_REQUEST, "error_language_not_found", languageTag);
				}
			}
		}

		List<String> requestedLanguageTags = null;
		if (languageTags != null && languageTags.length > 0) {
			requestedLanguageTags = Arrays.asList(languageTags);
		} else {
			requestedLanguageTags = nodeParameters.getLanguageList(Tx.get().data().options());
		}

		// First check whether the NGFC for the requested language,branch and version could be found.
		HibNodeFieldContainer fieldContainer = node.findVersion(requestedLanguageTags, branch.getUuid(), versioiningParameters.getVersion());
		if (fieldContainer == null) {
			// If a published version was requested, we check whether any
			// published language variant exists for the node, if not, response
			// with NOT_FOUND
			if (forVersion(versioiningParameters.getVersion()) == PUBLISHED && !node.getFieldContainers(branch, PUBLISHED).iterator().hasNext()) {
				log.error("Could not find field container for languages {" + requestedLanguageTags + "} and branch {" + branch.getUuid()
					+ "} and version params version {" + versioiningParameters.getVersion() + "}, branch {" + branch.getUuid() + "}");
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_branch_version", node.getUuid(), branch.getUuid());
			}

			// If a specific version was requested, that does not exist, we also
			// return NOT_FOUND
			if (forVersion(versioiningParameters.getVersion()) == INITIAL) {
				throw error(NOT_FOUND, "object_not_found_for_version", versioiningParameters.getVersion());
			}

			String langInfo = getLanguageInfo(requestedLanguageTags);
			if (log.isDebugEnabled()) {
				log.debug("The fields for node {" + node.getUuid() + "} can't be populated since the node has no matching language for the languages {"
					+ langInfo + "}. Fields will be empty.");
			}
			// No field container was found so we can only set the schema
			// reference that points to the container (no version information
			// will be included)
			if (fieldsSet.has("schema")) {
				restNode.setSchema(node.getSchemaContainer().transformToReference());
			}
			// TODO BUG Issue #119 - Actually we would need to throw a 404 in these cases but many current implementations rely on the empty node response.
			// The response will also contain information about other languages and general structure information.
			// We should change this behaviour and update the client implementations.
			// throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
		} else {
			SchemaModel schema = fieldContainer.getSchemaContainerVersion().getSchema();
			if (fieldsSet.has("container")) {
				restNode.setContainer(schema.getContainer());
			}
			if (fieldsSet.has("displayField")) {
				restNode.setDisplayField(schema.getDisplayField());
			}
			if (fieldsSet.has("displayName")) {
				restNode.setDisplayName(node.getDisplayName(ac));
			}

			if (fieldsSet.has("language")) {
				restNode.setLanguage(fieldContainer.getLanguageTag());
			}
			// List<String> fieldsToExpand = ac.getExpandedFieldnames();
			// modify the language fallback list by moving the container's
			// language to the front
			List<String> containerLanguageTags = new ArrayList<>(requestedLanguageTags);
			containerLanguageTags.remove(restNode.getLanguage());
			containerLanguageTags.add(0, restNode.getLanguage());

			// Schema reference
			if (fieldsSet.has("schema")) {
				restNode.setSchema(fieldContainer.getSchemaContainerVersion().transformToReference());
			}

			// Version reference
			if (fieldsSet.has("version") && fieldContainer.getVersion() != null) {
				restNode.setVersion(fieldContainer.getVersion().toString());
			}

			// editor and edited
			if (fieldsSet.has("editor")) {
				HibUser editor = fieldContainer.getEditor();
				if (editor != null) {
					restNode.setEditor(editor.transformToReference());
				}
			}
			if (fieldsSet.has("edited")) {
				restNode.setEdited(fieldContainer.getLastEditedDate());
			}

			if (fieldsSet.has("fields")) {
				// Iterate over all fields and transform them to rest
				com.gentics.mesh.core.rest.node.FieldMap fields = new FieldMapImpl();
				for (FieldSchema fieldEntry : schema.getFields()) {
					// boolean expandField =
					// fieldsToExpand.contains(fieldEntry.getName()) ||
					// ac.getExpandAllFlag();
					Field restField = fieldContainer.getRestField(ac, fieldEntry.getName(), fieldEntry, containerLanguageTags, level);
					if (fieldEntry.isRequired() && restField == null) {
						// TODO i18n
						// throw error(BAD_REQUEST, "The field {" +
						// fieldEntry.getName()
						// + "} is a required field but it could not be found in the
						// node. Please add the field using an update call or change
						// the field schema and
						// remove the required flag.");
						fields.put(fieldEntry.getName(), null);
					}
					if (restField == null) {
						if (log.isDebugEnabled()) {
							log.debug("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
						}
					} else {
						fields.put(fieldEntry.getName(), restField);
					}

				}
				restNode.setFields(fields);
			}
		}
	}

	private String getLanguageInfo(List<String> languageTags) {
		Iterator<String> it = languageTags.iterator();

		String langInfo = "[";
		while (it.hasNext()) {
			langInfo += it.next();
			if (it.hasNext()) {
				langInfo += ",";
			}
		}
		langInfo += "]";
		return langInfo;
	}

	/**
	 * Set the children info to the rest model.
	 *
	 * @param ac
	 * @param branch
	 *            Branch which will be used to identify the branch specific child nodes
	 * @param restNode
	 *            Rest model which will be updated
	 */
	private void setChildrenInfo(HibNode node, InternalActionContext ac, HibBranch branch, NodeResponse restNode) {
		Map<String, NodeChildrenInfo> childrenInfo = new HashMap<>();
		UserDao userDao = Tx.get().userDao();

		for (HibNode child : node.getChildren(branch.getUuid())) {
			if (userDao.hasPermission(ac.getUser(), child, READ_PERM)) {
				String schemaName = child.getSchemaContainer().getName();
				NodeChildrenInfo info = childrenInfo.get(schemaName);
				if (info == null) {
					info = new NodeChildrenInfo();
					String schemaUuid = child.getSchemaContainer().getUuid();
					info.setSchemaUuid(schemaUuid);
					info.setCount(1);
					childrenInfo.put(schemaName, info);
				} else {
					info.setCount(info.getCount() + 1);
				}
			}
		}
		restNode.setChildrenInfo(childrenInfo);
	}

	/**
	 * Set the tag information to the rest model.
	 *
	 * @param ac
	 * @param restNode
	 *            Rest model which will be updated
	 * @param branch
	 *            Branch which will be used to identify the branch specific tags
	 * @return
	 */
	private void setTagsToRest(HibNode node, InternalActionContext ac, NodeResponse restNode, HibBranch branch) {
		List<TagReference> list = node.getTags(branch).stream()
			.map(HibTag::transformToReference)
			.collect(Collectors.toList());
		restNode.setTags(list);
	}

	/**
	 * Add the branch specific webroot and language paths to the given rest node.
	 *
	 * @param ac
	 * @param restNode
	 *            Rest model which will be updated
	 * @param branch
	 *            Branch which will be used to identify the nodes relations and thus the correct path can be determined
	 * @return
	 */
	private void setPathsToRest(HibNode node, InternalActionContext ac, NodeResponse restNode, HibBranch branch) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {

			String branchUuid = Tx.get().getBranch(ac, node.getProject()).getUuid();
			ContainerType type = forVersion(versioiningParameters.getVersion());

			LinkType linkType = ac.getNodeParameters().getResolveLinks();

			// Path
			WebRootLinkReplacer linkReplacer = Tx.get().data().webRootLinkReplacer();
			String path = linkReplacer.resolve(ac, branchUuid, type, node.getUuid(), linkType, node.getProject().getName(), true, restNode.getLanguage());
			restNode.setPath(path);

			// languagePaths
			restNode.setLanguagePaths(getLanguagePaths(node, ac, linkType, branch));
		}
	}

	private Map<String, String> getLanguagePaths(HibNode node, InternalActionContext ac, LinkType linkType, HibBranch branch) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		String branchUuid = Tx.get().getBranch(ac, node.getProject()).getUuid();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		Map<String, String> languagePaths = new HashMap<>();
		WebRootLinkReplacer linkReplacer = Tx.get().data().webRootLinkReplacer();
		for (HibNodeFieldContainer currentFieldContainer : node.getFieldContainers(branch, forVersion(versioiningParameters.getVersion()))) {
			String currLanguage = currentFieldContainer.getLanguageTag();
			String languagePath = linkReplacer.resolve(ac, branchUuid, type, node, linkType, true, currLanguage);
			languagePaths.put(currLanguage, languagePath);
		}
		return languagePaths;
	}
}
