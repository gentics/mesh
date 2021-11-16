package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.event.node.NodeMovedEventModel;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NavigationParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;

public interface PersistingNodeDao extends NodeDao, PersistingRootDao<HibProject, HibNode> {

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
			tx.roleDao().setRolePermissions(node, ac, restNode);
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
	 * Stream the hierarchical path of the node. 
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	@Override
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
	@Override
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

	@Override
	default HibNode loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		Tx tx = Tx.get();
		UserDao userDao = tx.userDao();
		ContentDao contentDao = tx.contentDao();

		HibNode element = findByUuidGlobal(uuid);
		boolean notFound = element == null || !element.getProject().getUuid().equals(project.getUuid());
		if (!errorIfNotFound && notFound) {
			return null;
		}
		if (notFound) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		HibUser requestUser = ac.getUser();
		if (perm == READ_PUBLISHED_PERM) {
			HibBranch branch = tx.getBranch(ac, element.getProject());

			List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList(CommonTx.get().data().options());
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
			requestedLanguageTags = nodeParameters.getLanguageList(CommonTx.get().data().options());
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

		for (HibNode child : getChildren(node, branch.getUuid())) {
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
			WebRootLinkReplacer linkReplacer = CommonTx.get().data().webRootLinkReplacer();
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
		WebRootLinkReplacer linkReplacer = CommonTx.get().data().webRootLinkReplacer();
		for (HibNodeFieldContainer currentFieldContainer : node.getFieldContainers(branch, forVersion(versioiningParameters.getVersion()))) {
			String currLanguage = currentFieldContainer.getLanguageTag();
			String languagePath = linkReplacer.resolve(ac, branchUuid, type, node, linkType, true, currLanguage);
			languagePaths.put(currLanguage, languagePath);
		}
		return languagePaths;
	}

	@Override
	default void onRootDeleted(HibProject root, BulkActionContext bac) {
		// Remove all nodes in this project, including root.
		for (HibNode node : findAll(root)) {
			delete(node, bac, true, false);
			bac.inc();
		}
	}

	@Override
	default void moveTo(HibNode sourceNode, InternalActionContext ac, HibNode targetNode, EventQueueBatch batch) {
		Tx tx = Tx.get();

		// TODO should we add a guard that terminates this loop when it runs to
		// long?

		// Check whether the target node is part of the subtree of the source
		// node.
		// We must detect and prevent such actions because those would
		// invalidate the tree structure
		HibBranch branch = tx.getBranch(ac, sourceNode.getProject());
		String branchUuid = branch.getUuid();
		HibNode parent = getParentNode(targetNode, branchUuid);
		while (parent != null) {
			if (parent.getUuid().equals(sourceNode.getUuid())) {
				throw error(BAD_REQUEST, "node_move_error_not_allowed_to_move_node_into_one_of_its_children");
			}
			parent = getParentNode(parent, branchUuid);
		}

		if (!targetNode.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
			throw error(BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		}

		if (sourceNode.getUuid().equals(targetNode.getUuid())) {
			throw error(BAD_REQUEST, "node_move_error_same_nodes");
		}

		sourceNode.setParentNode(branchUuid, targetNode);

		// Update published graph field containers
		sourceNode.getFieldContainers(branchUuid, PUBLISHED).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});

		// Update draft graph field containers
		sourceNode.getFieldContainers(branchUuid, DRAFT).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});
		batch.add(onNodeMoved(sourceNode, branchUuid, targetNode));
		assertPublishConsistency(sourceNode, ac, branch);
	}

	private NodeMovedEventModel onNodeMoved(HibNode node, String branchUuid, HibNode target) {
		NodeMovedEventModel model = new NodeMovedEventModel();
		model.setEvent(NODE_MOVED);
		model.setBranchUuid(branchUuid);
		model.setProject(node.getProject().transformToReference());
		node.fillEventInfo(model);
		model.setTarget(target.transformToMinimalReference());
		return model;
	}

	@Override
	default NavigationResponse transformToNavigation(HibNode node, InternalActionContext ac) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		if (parameters.getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		Tx tx = Tx.get();
		// TODO assure that the schema version is correct
		if (!node.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
			throw error(BAD_REQUEST, "navigation_error_no_container");
		}
		String etagKey = buildNavigationEtagKey(ac, node, parameters.getMaxDepth(), 0, tx.getBranch(ac, node.getProject()).getUuid(), forVersion(ac
				.getVersioningParameters().getVersion()));
		String etag = ETag.hash(etagKey);
		ac.setEtag(etag, true);
		if (ac.matches(etag, true)) {
			throw new NotModifiedException();
		} else {
			NavigationResponse response = new NavigationResponse();
			return buildNavigationResponse(ac, node, parameters.getMaxDepth(), 0, response, response, tx.getBranch(ac, node.getProject()).getUuid(),
					forVersion(ac.getVersioningParameters().getVersion()));
		}
	}

	/**
	 * Generate the etag key for the requested navigation.
	 *
	 * @param ac
	 * @param node       Current node to start building the navigation
	 * @param maxDepth   Maximum depth of navigation
	 * @param level      Current level of recursion
	 * @param branchUuid Branch uuid used to extract selected tree structure
	 * @param type
	 * @return
	 */
	private String buildNavigationEtagKey(InternalActionContext ac, HibNode node, int maxDepth, int level, String branchUuid, ContainerType type) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		StringBuilder builder = new StringBuilder();
		builder.append(node.getETag(ac));

		List<HibNode> nodes = getChildren(node, ac.getUser(), branchUuid, null, type).collect(Collectors.toList());

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return builder.toString();
		}
		for (HibNode child : nodes) {
			if (child.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				builder.append(buildNavigationEtagKey(ac, child, maxDepth, level + 1, branchUuid, type));
			} else if (parameters.isIncludeAll()) {
				builder.append(buildNavigationEtagKey(ac, child, maxDepth, level, branchUuid, type));
			}
		}
		return builder.toString();
	}

	private Stream<? extends HibNode> getChildren(HibNode node, HibUser requestUser, String branchUuid, List<String> languageTags, ContainerType type) {
		InternalPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		UserDao userRoot = Tx.get().userDao();

		Predicate<HibNode> languageFilter = languageTags == null || languageTags.isEmpty()
				? item -> true
				: item -> languageTags.stream().anyMatch(languageTag -> item.getFieldContainer(languageTag, branchUuid, type) != null);

		return getChildren(node, branchUuid)
				.stream()
				.filter(languageFilter.and(item -> userRoot.hasPermission(requestUser, item, perm)));
	}

	/**
	 * Recursively build the navigation response.
	 *
	 * @param ac             Action context
	 * @param node           Current node that should be handled in combination with the given navigation element
	 * @param maxDepth       Maximum depth for the navigation
	 * @param level          Zero based level of the current navigation element
	 * @param navigation     Current navigation response
	 * @param currentElement Current navigation element for the given level
	 * @param branchUuid     Branch uuid to be used for loading children of nodes
	 * @param type           container type to be used for transformation
	 * @return
	 */
	private NavigationResponse buildNavigationResponse(InternalActionContext ac, HibNode node, int maxDepth, int level,
													   NavigationResponse navigation, NavigationElement currentElement, String branchUuid, ContainerType type) {
		List<HibNode> nodes = getChildren(node, ac.getUser(), branchUuid, null, type).collect(Collectors.toList());
		List<NavigationResponse> responses = new ArrayList<>();

		NodeResponse response = Tx.get().nodeDao().transformToRestSync(node, ac, 0);
		currentElement.setUuid(response.getUuid());
		currentElement.setNode(response);
		responses.add(navigation);

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return responses.get(responses.size() - 1);
		}
		NavigationParameters parameters = new NavigationParametersImpl(ac);
		// Add children
		for (HibNode child : nodes) {
			// TODO assure that the schema version is correct?
			// TODO also allow navigations over containers
			if (child.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				NavigationElement childElement = new NavigationElement();
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				currentElement.getChildren().add(childElement);
				responses.add(buildNavigationResponse(ac, child, maxDepth, level + 1, navigation, childElement, branchUuid, type));
			} else if (parameters.isIncludeAll()) {
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				NavigationElement childElement = new NavigationElement();
				currentElement.getChildren().add(childElement);
				responses.add(buildNavigationResponse(ac, child, maxDepth, level, navigation, childElement, branchUuid, type));
			}
		}
		return responses.get(responses.size() - 1);
	}

	@Override
	default PublishStatusModel transformToPublishStatus(HibNode node, InternalActionContext ac, String languageTag) {
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac, node.getProject());

		HibNodeFieldContainer container = node.getFieldContainer(languageTag, branch.getUuid(), PUBLISHED);
		if (container != null) {
			String date = container.getLastEditedDate();
			PublishStatusModel status = new PublishStatusModel();
			status.setPublished(true);
			status.setVersion(container.getVersion().toString());
			HibUser editor = container.getEditor();
			if (editor != null) {
				status.setPublisher(editor.transformToReference());
			}
			status.setPublishDate(date);
			return status;
		} else {
			container = node.getFieldContainer(languageTag, branch.getUuid(), DRAFT);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			return new PublishStatusModel().setPublished(false).setVersion(container.getVersion().toString());
		}
	}

	@Override
	default void publish(HibNode node, InternalActionContext ac, BulkActionContext bac, String languageTag) {
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac, node.getProject());
		String branchUuid = branch.getUuid();

		// get the draft version of the given language
		HibNodeFieldContainer draftVersion = node.getFieldContainer(languageTag, branchUuid, DRAFT);

		// if not existent -> NOT_FOUND
		if (draftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		// If the located draft version was already published we are done
		if (draftVersion.isPublished(branchUuid)) {
			return;
		}

		// TODO check whether all required fields are filled, if not -> unable to publish
		HibNodeFieldContainer publishedContainer = publish(node, ac, draftVersion.getLanguageTag(), branch, ac.getUser());
		// Invoke a store of the document since it must now also be added to the published index
		bac.add(publishedContainer.onPublish(branchUuid));
	}

	@Override
	default HibNodeFieldContainer publish(HibNode node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
		String branchUuid = branch.getUuid();

		// create published version
		HibNodeFieldContainer newVersion = node.createFieldContainer(languageTag, branch, user);
		newVersion.setVersion(newVersion.getVersion().nextPublished());

		node.setPublished(ac, newVersion, branchUuid);
		return newVersion;
	}

	@Override
	default Result<? extends HibNode> getBreadcrumbNodes(HibNode node, InternalActionContext ac) {
		return new TraversalResult<>(() -> Tx.get().nodeDao().getBreadcrumbNodeStream(node, ac).iterator());
	}

	@Override
	default NodeVersionsResponse transformToVersionList(HibNode node, InternalActionContext ac) {
		NodeVersionsResponse response = new NodeVersionsResponse();
		Map<String, List<VersionInfo>> versions = new HashMap<>();
		node.getFieldContainers(Tx.get().getBranch(ac), DRAFT).forEach(c -> {
			versions.put(c.getLanguageTag(), c.versions().stream()
					.map(v -> v.transformToVersionInfo(ac))
					.collect(Collectors.toList()));
		});

		response.setVersions(versions);
		return response;
	}

	@Override
	default PublishStatusResponse transformToPublishStatus(HibNode node, InternalActionContext ac) {
		PublishStatusResponse publishStatus = new PublishStatusResponse();
		Map<String, PublishStatusModel> languages = Tx.get().nodeDao().getLanguageInfo(node, ac);
		publishStatus.setAvailableLanguages(languages);
		return publishStatus;
	}

	@Override
	default void publish(HibNode node, InternalActionContext ac, BulkActionContext bac) {
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac, node.getProject());
		String branchUuid = branch.getUuid();

		List<HibNodeFieldContainer> unpublishedContainers = node.getFieldContainers(branch, ContainerType.DRAFT).stream().filter(c -> !c
				.isPublished(branchUuid)).collect(Collectors.toList());

		// publish all unpublished containers and handle recursion
		unpublishedContainers.stream().forEach(c -> {
			HibNodeFieldContainer newVersion = publish(node, ac, c.getLanguageTag(), branch, ac.getUser());
			bac.add(newVersion.onPublish(branchUuid));
		});
		assertPublishConsistency(node, ac, branch);

		// Handle recursion after publishing the current node.
		// This is done to ensure the publish consistency.
		// Even if the publishing process stops at the initial
		// level the consistency is correct.
		PublishParameters parameters = ac.getPublishParameters();
		if (parameters.isRecursive()) {
			for (HibNode nodeToPublish : getChildren(node, branchUuid)) {
				publish(nodeToPublish, ac, bac);
			}
		}
		bac.process();
	}
}
