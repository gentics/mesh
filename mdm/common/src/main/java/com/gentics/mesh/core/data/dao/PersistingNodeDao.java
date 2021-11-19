package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.Taggable;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMovedEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.core.webroot.PathPrefixUtil;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NavigationParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

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

		getFieldContainers(node, branch, PUBLISHED).stream().forEach(c -> {

			String date = DateUtils.toISO8601(c.getLastEditedTimestamp(), 0);

			PublishStatusModel status = buildPublishStatusModel(c, date);
			languages.put(c.getLanguageTag(), status);
		});

		getFieldContainers(node, branch, DRAFT).stream().filter(c -> !languages.containsKey(c.getLanguageTag())).forEach(c -> {
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
		ContentDao contentDao = Tx.get().contentDao();
		HibNodeFieldContainer fieldContainer = contentDao.findVersion(node, requestedLanguageTags, branch.getUuid(), versioiningParameters.getVersion());
		if (fieldContainer == null) {
			// If a published version was requested, we check whether any
			// published language variant exists for the node, if not, response
			// with NOT_FOUND
			if (forVersion(versioiningParameters.getVersion()) == PUBLISHED && !getFieldContainers(node, branch, PUBLISHED).iterator().hasNext()) {
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
		for (HibNodeFieldContainer currentFieldContainer : getFieldContainers(node, branch, forVersion(versioiningParameters.getVersion()))) {
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
		getFieldContainers(sourceNode, branchUuid, PUBLISHED).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});

		// Update draft graph field containers
		getFieldContainers(sourceNode, branchUuid, DRAFT).stream().forEach(container -> {
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
		ContentDao contentDao = Tx.get().contentDao();

		Predicate<HibNode> languageFilter = languageTags == null || languageTags.isEmpty()
				? item -> true
				: item -> languageTags.stream().anyMatch(languageTag -> contentDao.getFieldContainer(item, languageTag, branchUuid, type) != null);

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
		ContentDao contentDao = tx.contentDao();
		HibBranch branch = tx.getBranch(ac, node.getProject());

		HibNodeFieldContainer container = contentDao.getFieldContainer(node, languageTag, branch.getUuid(), PUBLISHED);
		if (container != null) {
			String date = container.getLastEditedDate();
			return buildPublishStatusModel(container, date);
		} else {
			container = contentDao.getFieldContainer(node, languageTag, branch.getUuid(), DRAFT);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			return new PublishStatusModel().setPublished(false).setVersion(container.getVersion().toString());
		}
	}

	private PublishStatusModel buildPublishStatusModel(HibNodeFieldContainer container, String date) {
		PublishStatusModel status = new PublishStatusModel();
		status.setPublished(true);
		status.setVersion(container.getVersion().toString());
		HibUser editor = container.getEditor();
		if (editor != null) {
			status.setPublisher(editor.transformToReference());
		}
		status.setPublishDate(date);

		return status;
	}

	@Override
	default void publish(HibNode node, InternalActionContext ac, BulkActionContext bac, String languageTag) {
		Tx tx = Tx.get();
		ContentDao contentDao = tx.contentDao();
		HibBranch branch = tx.getBranch(ac, node.getProject());
		String branchUuid = branch.getUuid();

		// get the draft version of the given language
		HibNodeFieldContainer draftVersion = contentDao.getFieldContainer(node, languageTag, branchUuid, DRAFT);

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
		HibNodeFieldContainer newVersion = Tx.get().contentDao().createFieldContainer(node, languageTag, branch, user);
		newVersion.setVersion(newVersion.getVersion().nextPublished());

		setPublished(node, ac, newVersion, branchUuid);
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
		getFieldContainers(node, Tx.get().getBranch(ac), DRAFT).forEach(c -> {
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

		List<HibNodeFieldContainer> unpublishedContainers = getFieldContainers(node, branch, ContainerType.DRAFT).stream().filter(c -> !c
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

	@Override
	default HibNode create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac);
		UserDao userDao = tx.userDao();
		SchemaDao schemaDao = tx.schemaDao();

		// Override any given version parameter. Creation is always scoped to drafts
		ac.getVersioningParameters().setVersion("draft");

		HibUser requestUser = ac.getUser();

		String body = ac.getBodyAsString();

		// 1. Extract the schema information from the given JSON
		SchemaReferenceInfo schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
		boolean missingSchemaInfo = schemaInfo.getSchema() == null
				|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid())
				&& StringUtils.isEmpty(schemaInfo.getSchema().getName()));
		if (missingSchemaInfo) {
			throw error(BAD_REQUEST, "error_schema_parameter_missing");
		}

		if (!isEmpty(schemaInfo.getSchema().getUuid())) {
			// 2. Use schema reference by uuid first
			HibSchema schemaByUuid = schemaDao.loadObjectByUuid(project, ac, schemaInfo.getSchema().getUuid(), READ_PERM);
			HibSchemaVersion schemaVersion = branch.findLatestSchemaVersion(schemaByUuid);
			if (schemaVersion == null) {
				throw error(BAD_REQUEST, "schema_error_schema_not_linked_to_branch", schemaByUuid.getName(), branch.getName(), project.getName());
			}
			return createNode(ac, schemaVersion, batch, uuid);
		}

		// 3. Or just schema reference by name
		if (!isEmpty(schemaInfo.getSchema().getName())) {
			HibSchema schemaByName = schemaDao.findByName(project, schemaInfo.getSchema().getName());
			if (schemaByName != null) {
				String schemaName = schemaByName.getName();
				String schemaUuid = schemaByName.getUuid();
				if (userDao.hasPermission(requestUser, schemaByName, READ_PERM)) {
					HibSchemaVersion schemaVersion = branch.findLatestSchemaVersion(schemaByName);
					if (schemaVersion == null) {
						throw error(BAD_REQUEST, "schema_error_schema_not_linked_to_branch", schemaByName.getName(), branch.getName(),
								project.getName());
					}
					return createNode(ac, schemaVersion, batch, uuid);
				} else {
					throw error(FORBIDDEN, "error_missing_perm", schemaUuid + "/" + schemaName, READ_PERM.getRestPerm().getName());
				}

			} else {
				throw error(NOT_FOUND, "schema_not_found", schemaInfo.getSchema().getName());
			}
		} else {
			throw error(BAD_REQUEST, "error_schema_parameter_missing");
		}
	}

	/**
	 * Create a new node using the specified schema container.
	 *
	 * @param ac
	 * @param schemaVersion
	 * @param batch
	 * @param uuid
	 * @return
	 */
	// TODO use schema container version instead of container
	private HibNode createNode(InternalActionContext ac, HibSchemaVersion schemaVersion, EventQueueBatch batch,
							String uuid) {
		Tx tx = Tx.get();
		HibProject project = tx.getProject(ac);
		HibUser requestUser = ac.getUser();
		UserDao userRoot = tx.userDao();
		NodeDao nodeDao = tx.nodeDao();

		NodeCreateRequest requestModel = ac.fromJson(NodeCreateRequest.class);
		if (requestModel.getParentNode() == null || isEmpty(requestModel.getParentNode().getUuid())) {
			throw error(BAD_REQUEST, "node_missing_parentnode_field");
		}
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "node_no_languagecode_specified");
		}

		// Load the parent node in order to create the node
		HibNode parentNode = nodeDao.loadObjectByUuid(project, ac, requestModel.getParentNode().getUuid(),
				CREATE_PERM);
		HibBranch branch = tx.getBranch(ac);
		// BUG: Don't use the latest version. Use the version which is linked to the
		// branch!
		HibNode node = nodeDao.create(parentNode, requestUser, schemaVersion, project, branch, uuid);

		// Add initial permissions to the created node
		userRoot.inheritRolePermissions(requestUser, parentNode, node);

		// Create the language specific graph field container for the node
		HibLanguage language = Tx.get().languageDao().findByLanguageTag(requestModel.getLanguage());
		if (language == null) {
			throw error(BAD_REQUEST, "language_not_found", requestModel.getLanguage());
		}
		ContentDao contentDao = Tx.get().contentDao();
		HibNodeFieldContainer container = contentDao.createFieldContainer(node, language.getLanguageTag(), branch, requestUser);
		container.updateFieldsFromRest(ac, requestModel.getFields());

		batch.add(node.onCreated());
		batch.add(container.onCreated(branch.getUuid(), DRAFT));

		// Check for webroot input data consistency (PUT on webroot)
		String webrootSegment = ac.get("WEBROOT_SEGMENT_NAME");
		if (webrootSegment != null) {
			String current = container.getSegmentFieldValue();
			if (!webrootSegment.equals(current)) {
				throw error(BAD_REQUEST, "webroot_error_segment_field_mismatch", webrootSegment, current);
			}
		}

		if (requestModel.getTags() != null) {
			updateTags(node, ac, batch, requestModel.getTags());
		}

		return node;
	}

	default String getAPIPath(HibNode node, InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(node.getProject().getName()) + "/nodes/" + node.getUuid();
	}

	default Result<HibNodeFieldContainer> getFieldContainers(HibNode node, HibBranch branch, ContainerType type) {
		return getFieldContainers(node, branch.getUuid(), type);
	}

	@Override
	default void delete(HibProject root, HibNode element, BulkActionContext bac) {
		delete(element, bac, false, true);
	}

	@Override
	default void delete(HibNode node, BulkActionContext bac, boolean ignoreChecks, boolean recursive) {
		if (!ignoreChecks) {
			// Prevent deletion of basenode
			if (node.getProject().getBaseNode().getUuid().equals(node.getUuid())) {
				throw error(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			}
		}
		// Delete subfolders
		if (log.isDebugEnabled()) {
			log.debug("Deleting node {" + node.getUuid() + "}");
		}
		if (recursive) {
			// No need to check the branch since this delete must affect all branches
			for (HibNode child : getChildren(node)) {
				delete(child, bac, false, true);
				bac.process();
			}
		}

		// Delete all initial containers (which will delete all containers)
		for (HibNodeFieldContainer container : getFieldContainers(node, INITIAL)) {
			container.delete(bac);
		}
		if (log.isDebugEnabled()) {
			log.debug("Deleting node {" + node.getUuid() + "} vertex.");
		}

		addReferenceUpdates(node, bac);

		bac.add(onDeleted(node, null, null, null));
		removeElement(node);
		bac.process();
	}

	private NodeMeshEventModel onDeleted(HibNode node, String branchUuid, ContainerType type, String languageTag) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(node.getTypeInfo().getOnDeleted());
		event.setUuid(node.getUuid());
		event.setLanguageTag(languageTag);
		event.setType(type);
		event.setBranchUuid(branchUuid);
		event.setProject(node.getProject().transformToReference());
		HibSchema schema = node.getSchemaContainer();
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	@Override
	default void deleteFromBranch(HibNode node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks) {
		DeleteParameters parameters = ac.getDeleteParameters();

		// 1. Remove subfolders from branch
		String branchUuid = branch.getUuid();

		for (HibNode child : getChildren(node, branchUuid)) {
			if (!parameters.isRecursive()) {
				throw error(BAD_REQUEST, "node_error_delete_failed_node_has_children");
			}
			deleteFromBranch(child, ac, branch, bac, ignoreChecks);
		}

		// 2. Delete all language containers
		for (HibNodeFieldContainer container : getFieldContainers(node, branch, DRAFT)) {
			deleteLanguageContainer(node, ac, branch, container.getLanguageTag(), bac, false);
		}

		// 3. Now check if the node has no more field containers in any branch. We can delete it in those cases
		if (Tx.get().contentDao().getFieldContainerCount(node) == 0) {
			delete(node, bac, false, true);
		} else {
			removeParent(node, branchUuid);
		}
	}

	@Override
	default void deleteLanguageContainer(HibNode node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
										 boolean failForLastContainer) {

		// 1. Check whether the container has also a published variant. We need to take it offline in those cases
		HibNodeFieldContainer container = getFieldContainer(node, languageTag, branch, PUBLISHED);
		if (container != null) {
			takeOffline(node, ac, bac, branch, languageTag);
		}

		// 2. Load the draft container and remove it from the branch
		container = getFieldContainer(node, languageTag, branch, DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", languageTag);
		}
		container.deleteFromBranch(branch, bac);
		// No need to delete the published variant because if the container was published the take offline call handled it

		// starting with the old draft, delete all GFC that have no next and are not draft (for other branches)
		HibNodeFieldContainer dangling = container;
		while (dangling != null && !dangling.isDraft() && !dangling.hasNextVersion()) {
			HibNodeFieldContainer toDelete = dangling;
			dangling = toDelete.getPreviousVersion();
			toDelete.delete(bac);
		}

		HibNodeFieldContainer initial = getFieldContainer(node, languageTag, branch, INITIAL);
		if (initial != null) {
			// Remove the initial edge
			removeInitialFieldContainerEdge(node, initial, branch.getUuid());

			// starting with the old initial, delete all GFC that have no previous and are not initial (for other branches)
			dangling = initial;
			while (dangling != null && !dangling.isInitial() && !dangling.hasPreviousVersion()) {
				HibNodeFieldContainer toDelete = dangling;
				// since the GFC "toDelete" was only used by this branch, it can not have more than one "next" GFC
				// (multiple "next" would have to belong to different branches, and for every branch, there would have to be
				// an INITIAL, which would have to be either this GFC or a previous)
				dangling = Tx.get().contentDao().getNextVersions(toDelete).iterator().next();
				toDelete.delete(bac, false);
			}
		}

		// 3. Check whether this was be the last container of the node for this branch
		DeleteParameters parameters = ac.getDeleteParameters();
		if (failForLastContainer) {
			Result<HibNodeFieldContainer> draftContainers = getFieldContainers(node, branch.getUuid(), DRAFT);
			Result<HibNodeFieldContainer> publishContainers = getFieldContainers(node, branch.getUuid(), PUBLISHED);
			boolean wasLastContainer = !draftContainers.iterator().hasNext() && !publishContainers.iterator().hasNext();

			if (!parameters.isRecursive() && wasLastContainer) {
				throw error(BAD_REQUEST, "node_error_delete_failed_last_container_for_branch");
			}

			// Also delete the node and children
			if (parameters.isRecursive() && wasLastContainer) {
				deleteFromBranch(node, ac, branch, bac, false);
			}
		}
	}

	@Override
	default void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac) {
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac, node.getProject());
		PublishParameters parameters = ac.getPublishParameters();
		takeOffline(node, ac, bac, branch, parameters);
	}

	private void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, PublishParameters parameters) {
		// Handle recursion first to start at the leaves
		if (parameters.isRecursive()) {
			for (HibNode child : getChildren(node, branch.getUuid())) {
				takeOffline(child, ac, bac, branch, parameters);
			}
		}

		String branchUuid = branch.getUuid();
		removePublishedEdges(node, branchUuid, bac);
		assertPublishConsistency(node, ac, branch);

		bac.process();
	}

	@Override
	default void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		ContentDao contentDao = Tx.get().contentDao();
		String branchUuid = branch.getUuid();

		// Locate the published container
		HibNodeFieldContainer published = contentDao.getFieldContainer(node, languageTag, branchUuid, PUBLISHED);
		if (published == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}
		bac.add(published.onTakenOffline(branchUuid));

		// Remove the "published" edge
		removePublishedEdge(node, languageTag, branchUuid);
		assertPublishConsistency(node, ac, branch);

		bac.process();
	}

	@Override
	default List<String> getAvailableLanguageNames(HibNode node) {
		List<String> languageTags = new ArrayList<>();
		// TODO it would be better to store the languagetag along with the edge
		for (HibNodeFieldContainer container : getDraftFieldContainers(node)) {
			languageTags.add(container.getLanguageTag());
		}
		return languageTags;
	}

	default Result<HibNodeFieldContainer> getDraftFieldContainers(HibNode node) {
		// FIX ME: We should not rely on specific branches.
		return getFieldContainers(node, node.getProject().getLatestBranch(), DRAFT);
	}

	@Override
	default String getPath(HibNode node, ActionContext ac, String branchUuid, ContainerType type, String... languageTag) {
		ContentDao contentDao = Tx.get().contentDao();
		// We want to avoid rending the path again for nodes which we have already handled.
		// Thus utilise the action context data map to retrieve already handled paths.
		String cacheKey = node.getUuid() + branchUuid + type.getCode() + Arrays.toString(languageTag);
		return (String) ac.data().computeIfAbsent(cacheKey, key -> {

			List<String> segments = new ArrayList<>();
			String segment = contentDao.getPathSegment(node, branchUuid, type, languageTag);
			if (segment == null) {
				// Fall back to url fields
				return getUrlFieldPath(node, branchUuid, type, languageTag);
			}
			segments.add(segment);

			// For the path segments of the container, we add all (additional)
			// project languages to the list of languages for the fallback.
			HibNode current = node;
			while (current != null) {
				current = getParentNode(current, branchUuid);
				if (current == null || getParentNode(current, branchUuid) == null) {
					break;
				}
				// For the path segments of the container, we allow ANY language (of the project)
				segment = contentDao.getPathSegment(current, branchUuid, type, true, languageTag);

				// Abort early if one of the path segments could not be resolved.
				// We need to fall back to url fields in those cases.
				if (segment == null) {
					return getUrlFieldPath(node, branchUuid, type, languageTag);
				}
				segments.add(segment);
			}

			Collections.reverse(segments);

			// Finally construct the path from all segments
			StringBuilder builder = new StringBuilder();

			// Append the prefix first
			BranchDao branchDao = Tx.get().branchDao();
			HibBranch branch = branchDao.findByUuid(node.getProject(), branchUuid);
			if (branch != null) {
				String prefix = PathPrefixUtil.sanitize(branch.getPathPrefix());
				if (!prefix.isEmpty()) {
					String[] prefixSegments = prefix.split("/");
					for (String prefixSegment : prefixSegments) {
						if (prefixSegment.isEmpty()) {
							continue;
						}
						builder.append("/").append(URIUtils.encodeSegment(prefixSegment));
					}
				}
			}

			Iterator<String> it = segments.iterator();
			while (it.hasNext()) {
				String currentSegment = it.next();
				builder.append("/").append(URIUtils.encodeSegment(currentSegment));
			}
			return builder.toString();
		});
	}

	/**
	 * Return the first url field path found.
	 *
	 * @param node
	 * @param branchUuid
	 * @param type
	 * @param languages
	 *            The order of languages will be used to search for the url field values.
	 * @return null if no url field could be found.
	 */
	private String getUrlFieldPath(HibNode node, String branchUuid, ContainerType type, String... languages) {
		ContentDao contentDao = Tx.get().contentDao();
		return Stream.of(languages)
				.flatMap(language -> Stream.ofNullable(contentDao.getFieldContainer(node, language, branchUuid, type)))
				.flatMap(HibNodeFieldContainer::getUrlFieldValues)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Update the node language or create a new draft for the specific language. This method will also apply conflict detection and take care of deduplication.
	 *
	 *
	 * <p>
	 * Conflict detection: Conflict detection only occurs during update requests. Two diffs are created. The update request will be compared against base
	 * version graph field container (version which is referenced by the request). The second diff is being created in-between the base version graph field
	 * container and the latest version of the graph field container. This diff identifies previous changes in between those version. These both diffs are
	 * compared in order to determine their intersection. The intersection identifies those fields which have been altered in between both versions and which
	 * would now also be touched by the current request. This situation causes a conflict and the update would abort.
	 *
	 * <p>
	 * Conflict cases
	 * <ul>
	 * <li>Initial creates - No conflict handling needs to be performed</li>
	 * <li>Migration check - Nodes which have not yet migrated can't be updated</li>
	 * </ul>
	 *
	 *
	 * <p>
	 * Deduplication: Field values that have not been changed in between the request data and the last version will not cause new fields to be created in new
	 * version graph field containers. The new version graph field container will instead reference those fields from the previous graph field container
	 * version. Please note that this deduplication only applies to complex fields (e.g.: Lists, Micronode)
	 *
	 * @param ac
	 * @param batch
	 *            Batch which will be used to update the search index
	 * @return
	 */
	@Override
	default boolean update(HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		NodeUpdateRequest requestModel = ac.fromJson(NodeUpdateRequest.class);
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "error_language_not_set");
		}

		// Check whether the tags need to be updated
		List<TagReference> tags = requestModel.getTags();
		if (tags != null) {
			updateTags(node, ac, batch, requestModel.getTags());
		}

		// Set the language tag parameter here in order to return the updated language in the response
		String languageTag = requestModel.getLanguage();
		NodeParameters nodeParameters = ac.getNodeParameters();
		nodeParameters.setLanguages(languageTag);

		Tx tx = Tx.get();
		HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
		if (language == null) {
			throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
		}
		NodeDao nodeDao = tx.nodeDao();
		ContentDao contentDao = tx.contentDao();
		HibBranch branch = tx.getBranch(ac, node.getProject());
		HibNodeFieldContainer latestDraftVersion = contentDao.getFieldContainer(node, languageTag, branch, DRAFT);

		// Check whether this is the first time that an update for the given language and branch occurs. In this case a new container must be created.
		// This means that no conflict check can be performed. Conflict checks only occur for updates on existing contents.
		if (latestDraftVersion == null) {
			// Create a new field container
			latestDraftVersion = contentDao.createFieldContainer(node, languageTag, branch, ac.getUser());

			// Check whether the node has a parent node in this branch, if not, the request is supposed to be a create request
			// and we get the parent node from this create request
			if (getParentNode(node, branch.getUuid()) == null) {
				NodeCreateRequest createRequest = JsonUtil.readValue(ac.getBodyAsString(), NodeCreateRequest.class);
				if (createRequest.getParentNode() == null || isEmpty(createRequest.getParentNode().getUuid())) {
					throw error(BAD_REQUEST, "node_missing_parentnode_field");
				}
				HibNode parentNode = nodeDao.loadObjectByUuid(node.getProject(), ac, createRequest.getParentNode().getUuid(), CREATE_PERM);
				// check whether the parent node is visible in the branch
				if (!parentNode.isBaseNode() && !isVisibleInBranch(parentNode, branch.getUuid())) {
					log.error(
							String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
									branch.getName(), parentNode.getUuid()));
					throw error(NOT_FOUND, "object_not_found_for_uuid", createRequest.getParentNode().getUuid());
				}
				setParentNode(node, branch.getUuid(), parentNode);
			}

			latestDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());
			batch.add(latestDraftVersion.onCreated(branch.getUuid(), DRAFT));
			return true;
		} else {
			String version = requestModel.getVersion();
			if (version == null) {
				log.debug("No version was specified. Assuming 'draft' for latest version");
				version = "draft";
			}

			// Make sure the container was already migrated. Otherwise the update can't proceed.
			HibSchemaVersion schemaVersion = latestDraftVersion.getSchemaContainerVersion();
			if (!latestDraftVersion.getSchemaContainerVersion().equals(branch.findLatestSchemaVersion(schemaVersion
					.getSchemaContainer()))) {
				throw error(BAD_REQUEST, "node_error_migration_incomplete");
			}

			// Load the base version field container in order to create the diff
			HibNodeFieldContainer baseVersionContainer = contentDao.findVersion(node, requestModel.getLanguage(), branch.getUuid(), version);
			if (baseVersionContainer == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", version, requestModel.getLanguage());
			}

			latestDraftVersion.getSchemaContainerVersion().getSchema().assertForUnhandledFields(requestModel.getFields());

			// TODO handle simplified case in which baseContainerVersion and
			// latestDraftVersion are equal
			List<FieldContainerChange> baseVersionDiff = baseVersionContainer.compareTo(latestDraftVersion);
			List<FieldContainerChange> requestVersionDiff = latestDraftVersion.compareTo(requestModel.getFields());

			// Compare both sets of change sets
			List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains).collect(Collectors.toList());

			// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
			if (!latestDraftVersion.getVersion().getFullVersion().equals(version)) {

				// Check whether a conflict has been detected
				if (intersect.size() > 0) {
					NodeVersionConflictException conflictException = new NodeVersionConflictException("node_error_conflict_detected");
					conflictException.setOldVersion(baseVersionContainer.getVersion().toString());
					conflictException.setNewVersion(latestDraftVersion.getVersion().toString());
					for (FieldContainerChange fcc : intersect) {
						conflictException.addConflict(fcc.getFieldCoordinates());
					}
					throw conflictException;
				}
			}

			// Make sure to only update those fields which have been altered in between the latest version and the current request. Remove
			// unaffected fields from the rest request in order to prevent duplicate references. We don't want to touch field that have not been changed.
			// Otherwise the graph field references would no longer point to older revisions of the same field.
			Set<String> fieldsToKeepForUpdate = requestVersionDiff.stream().map(e -> e.getFieldKey()).collect(Collectors.toSet());
			for (String fieldKey : requestModel.getFields().keySet()) {
				if (fieldsToKeepForUpdate.contains(fieldKey)) {
					continue;
				}
				if (log.isDebugEnabled()) {
					log.debug("Removing field from request {" + fieldKey + "} in order to handle deduplication.");
				}
				requestModel.getFields().remove(fieldKey);
			}

			// Check whether the request still contains data which needs to be updated.
			if (!requestModel.getFields().isEmpty()) {

				// Create new field container as clone of the existing
				HibNodeFieldContainer newDraftVersion = contentDao.createFieldContainer(node, language.getLanguageTag(), branch, ac.getUser(),
						latestDraftVersion, true);
				// Update the existing fields
				newDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());

				// Purge the old draft
				if (ac.isPurgeAllowed() && newDraftVersion.isAutoPurgeEnabled() && latestDraftVersion.isPurgeable()) {
					latestDraftVersion.purge();
				}

				latestDraftVersion = newDraftVersion;
				batch.add(newDraftVersion.onUpdated(branch.getUuid(), DRAFT));
				return true;
			}
		}
		return false;
	}

	@Override
	default Page<? extends HibTag> updateTags(HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		Tx tx = Tx.get();
		List<HibTag> tags = Taggable.getTagsToSet(node.getProject(), ac, batch);
		HibBranch branch = tx.getBranch(ac);
		applyTags(node, branch, tags, batch);
		HibUser user = ac.getUser();
		return tx.tagDao().getTags(node, user, ac.getPagingParameters(), branch);
	}

	@Override
	default void updateTags(HibNode node, InternalActionContext ac, EventQueueBatch batch, List<TagReference> list) {
		Tx tx = Tx.get();
		List<HibTag> tags = Taggable.getTagsToSet(node.getProject(), list, ac, batch);
		HibBranch branch = tx.getBranch(ac);
		applyTags(node, branch, tags, batch);
	}

	private void applyTags(HibNode node, HibBranch branch, List<? extends HibTag> tags, EventQueueBatch batch) {
		List<HibTag> currentTags = node.getTags(branch).list();

		List<HibTag> toBeAdded = tags.stream()
				.filter(StreamUtil.not(new HashSet<>(currentTags)::contains))
				.collect(Collectors.toList());
		toBeAdded.forEach(tag -> {
			node.addTag(tag, branch);
			batch.add(onTagged(node, tag, branch, ASSIGNED));
		});

		List<HibTag> toBeRemoved = currentTags.stream()
				.filter(StreamUtil.not(new HashSet<>(tags)::contains))
				.collect(Collectors.toList());
		toBeRemoved.forEach(tag -> {
			node.removeTag(tag, branch);
			batch.add(onTagged(node, tag, branch, UNASSIGNED));
		});
	}

	@Override
	default NodeTaggedEventModel onTagged(HibNode node, HibTag tag, HibBranch branch, Assignment assignment) {
		NodeTaggedEventModel model = new NodeTaggedEventModel();
		model.setTag(tag.transformToReference());

		model.setBranch(branch.transformToReference());
		model.setProject(node.getProject().transformToReference());
		model.setNode(node.transformToMinimalReference());

		switch (assignment) {
			case ASSIGNED:
				model.setEvent(NODE_TAGGED);
				break;

			case UNASSIGNED:
				model.setEvent(NODE_UNTAGGED);
				break;
		}

		return model;
	}
}
