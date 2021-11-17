package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.*;
import static com.gentics.mesh.core.rest.common.ContainerType.*;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.VersioningParameters;

public interface HibNode extends HibCoreElement<NodeResponse>, HibCreatorTracking, HibBucketableElement, HibTransformableElement<NodeResponse> {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.NODE, NODE_CREATED, NODE_UPDATED, NODE_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the element version string.
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Return the project of the node.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Maximum depth for transformations: {@value #MAX_TRANSFORMATION_LEVEL}
	 */
	public static final int MAX_TRANSFORMATION_LEVEL = 3;

	/**
	 * Add the given tag to the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void addTag(HibTag tag, HibBranch branch);

	/**
	 * Remove the given tag from the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void removeTag(HibTag tag, HibBranch branch);

	/**
	 * Remove all tags for the given branch.
	 *
	 * @param branch
	 */
	void removeAllTags(HibBranch branch);

	/**
	 * Return a list of all tags that were assigned to this node in the given branch.
	 *
	 * @param branch
	 * @return
	 */
	Result<HibTag> getTags(HibBranch branch);

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
	 * Create a new graph field container for the given language and assign the schema version of the branch to the container. The graph field container will be
	 * the (only) DRAFT version for the language/branch. If this is the first container for the language, it will also be the INITIAL version. Otherwise the
	 * container will be a clone of the last draft and will have the next version number.
	 *
	 * @param languageTag
	 * @param branch
	 *            branch
	 * @param user
	 *            user
	 * @return
	 */
	HibNodeFieldContainer createFieldContainer(String languageTag, HibBranch branch, HibUser user);

	/**
	 * Return the draft field containers of the node in the latest branch.
	 *
	 * @return
	 */
	default Result<HibNodeFieldContainer> getDraftFieldContainers() {
		// FIX ME: We should not rely on specific branches.
		return getFieldContainers(getProject().getLatestBranch(), DRAFT);
	}

	/**
	 * Return a traversal of graph field containers of given type for the node in the given branch.
	 *
	 * @param branch
	 * @param type
	 * @return
	 */
	default Result<HibNodeFieldContainer> getFieldContainers(HibBranch branch, ContainerType type) {
		return getFieldContainers(branch.getUuid(), type);
	}

	/**
	 * Return traversal of graph field containers of given type for the node in the given branch.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	Result<HibNodeFieldContainer> getFieldContainers(String branchUuid, ContainerType type);

	/**
	 * Return a list of language names for draft versions in the latest branch
	 *
	 * @return
	 */
	List<String> getAvailableLanguageNames();

	/**
	 * Set the project of the node.
	 *
	 * @param project
	 */
	void setProject(HibProject project);

	/**
	 * Returns the parent node of this node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	HibNode getParentNode(String branchUuid);

	/**
	 * Set the parent node of this node.
	 *
	 * @param branchUuid
	 * @param parentNode
	 */
	void setParentNode(String branchUuid, HibNode parentNode);

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 *
	 * @param ac
	 * @return
	 */
	default String getDisplayName(InternalActionContext ac) {
			NodeParameters nodeParameters = ac.getNodeParameters();
			VersioningParameters versioningParameters = ac.getVersioningParameters();

			HibNodeFieldContainer container = findVersion(nodeParameters.getLanguageList(Tx.get().data().options()), Tx.get().getBranch(ac, getProject()).getUuid(),
					versioningParameters
							.getVersion());
			if (container == null) {
				if (log.isDebugEnabled()) {
					log.debug("Could not find any matching i18n field container for node {" + getUuid() + "}.");
				}
				return null;
			} else {
				// Determine the display field name and load the string value
				// from that field.
				return container.getDisplayFieldValue();
			}

	}

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter. When a user requests a node using ?lang=de,en and there
	 * is no de version the en version will be selected and returned.
	 *
	 * @param languageTags
	 * @param branchUuid
	 *            branch Uuid
	 * @param version
	 *            requested version. This must either be "draft" or "published" or a version number with pattern [major.minor]
	 * @return Next matching field container or null when no language matches
	 */
	HibNodeFieldContainer findVersion(List<String> languageTags, String branchUuid, String version);

	/**
	 * Iterate the version chain from the back in order to find the given version.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param version
	 * @return Found version or null when no version could be found.
	 */
	default HibNodeFieldContainer findVersion(String languageTag, String branchUuid, String version) {
		return findVersion(Arrays.asList(languageTag), branchUuid, version);
	}

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter.
	 *
	 * @param ac
	 * @param languageTags
	 * @return Next matching field container or null when no language matches
	 */
	default HibNodeFieldContainer findVersion(InternalActionContext ac, List<String> languageTags, String version) {
		return findVersion(languageTags, Tx.get().getBranch(ac).getUuid(), version);
	}

	/**
	 * Find the content that matches the given parameters (languages, type).
	 *
	 * @param ac
	 * @param languageTags
	 * @param type
	 * @return
	 */
	default HibNodeFieldContainer findVersion(InternalActionContext ac, List<String> languageTags, ContainerType type) {
		return findVersion(ac, languageTags, type.getHumanCode());
	}

	/**
	 * Transform the node into a node reference rest model.
	 *
	 * @param ac
	 */
	default NodeReference transformToReference(InternalActionContext ac) {
		Tx tx = Tx.get();
		HibBranch branch = tx.getBranch(ac, getProject());

		NodeReference nodeReference = new NodeReference();
		nodeReference.setUuid(getUuid());
		nodeReference.setDisplayName(getDisplayName(ac));
		nodeReference.setSchema(getSchemaContainer().transformToReference());
		nodeReference.setProjectName(getProject().getName());
		if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
			WebRootLinkReplacer linkReplacer = tx.data().webRootLinkReplacer();
			ContainerType type = forVersion(ac.getVersioningParameters().getVersion());
			String url = linkReplacer.resolve(ac, branch.getUuid(), type, this, ac.getNodeParameters().getResolveLinks(), ac.getNodeParameters()
					.getLanguages());
			nodeReference.setPath(url);
		}
		return nodeReference;
	}

	/**
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(InternalActionContext ac, HibNodeFieldContainer container, String branchUuid);

	/**
	 * Return the schema container for the node.
	 *
	 * @return
	 */
	HibSchema getSchemaContainer();

	/**
	 * Set the schema container of the node.
	 *
	 * @param container
	 */
	void setSchemaContainer(HibSchema container);

	/**
	 * Check whether the node is the base node of its project
	 *
	 * @return true for base node
	 */
	boolean isBaseNode();

	/**
	 * Transform the node information to a minimal reference which does not include language or type information.
	 *
	 * @return
	 */
	default NodeReference transformToMinimalReference() {
		NodeReference ref = new NodeReference();
		ref.setUuid(getUuid());
		ref.setSchema(getSchemaContainer().transformToReference());
		return ref;
	}

	default String getSubETag(InternalActionContext ac) {
		Tx tx = Tx.get();
		UserDao userDao = tx.userDao();
		TagDao tagDao = tx.tagDao();
		NodeDao nodeDao = tx.nodeDao();

		StringBuilder keyBuilder = new StringBuilder();

		// Parameters
		HibBranch branch = tx.getBranch(ac, getProject());
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		HibNode parentNode = nodeDao.getParentNode(this, branch.getUuid());
		HibNodeFieldContainer container = findVersion(ac.getNodeParameters().getLanguageList(tx.data().options()), branch.getUuid(),
				ac.getVersioningParameters()
						.getVersion());

		/**
		 * branch uuid
		 */
		keyBuilder.append(branch.getUuid());
		keyBuilder.append("-");

		// TODO version, language list

		// We can omit further etag keys since this would return a 404 anyhow
		// since the requested container could not be found.
		if (container == null) {
			keyBuilder.append("404-no-container");
			return keyBuilder.toString();
		}

		/**
		 * Parent node
		 *
		 * The node can be moved and this would also affect the response. The etag must also be changed when the node is moved.
		 */
		if (parentNode != null) {
			keyBuilder.append("-");
			keyBuilder.append(parentNode.getUuid());
		}

		// fields version
		if (container != null) {
			keyBuilder.append("-");
			keyBuilder.append(container.getETag(ac));
		}

		/**
		 * Expansion (all)
		 *
		 * The expandAll parameter changes the json response and thus must be included in the etag computation.
		 */
		if (ac.getNodeParameters().getExpandAll()) {
			keyBuilder.append("-");
			keyBuilder.append("expand:true");
		}

		// expansion (selective)
		String expandedFields = Arrays.toString(ac.getNodeParameters().getExpandedFieldNames());
		keyBuilder.append("-");
		keyBuilder.append("expandFields:");
		keyBuilder.append(expandedFields);

		// branch specific tags
		for (HibTag tag : getTags(branch)) {
			// Tags can't be moved across branches thus we don't need to add the
			// tag family etag
			keyBuilder.append(tagDao.getETag(tag, ac));
		}

		// branch specific children
		for (HibNode child : tx.nodeDao().getChildren(this, branch.getUuid())) {
			if (userDao.hasPermission(ac.getUser(), child, READ_PUBLISHED_PERM)) {
				keyBuilder.append("-");
				keyBuilder.append(child.getSchemaContainer().getName());
			}
		}

		// Publish state & availableLanguages
		for (HibNodeFieldContainer c : getFieldContainers(branch, PUBLISHED)) {
			keyBuilder.append(c.getLanguageTag() + "published");
		}
		for (HibNodeFieldContainer c : getFieldContainers(branch, DRAFT)) {
			keyBuilder.append(c.getLanguageTag() + "draft");
		}

		// breadcrumb
		keyBuilder.append("-");
		HibNode current = nodeDao.getParentNode(this, branch.getUuid());
		if (current != null) {
			while (current != null) {
				String key = current.getUuid() + getDisplayName(ac);
				keyBuilder.append(key);
				if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
					WebRootLinkReplacer linkReplacer = tx.data().webRootLinkReplacer();
					String url = linkReplacer.resolve(ac, branch.getUuid(), type, current.getUuid(), ac.getNodeParameters().getResolveLinks(),
							getProject().getName(), container.getLanguageTag());
					keyBuilder.append(url);
				}
				current = nodeDao.getParentNode(current, branch.getUuid());

			}
		}

		/**
		 * webroot path & language paths
		 *
		 * The webroot and language paths must be included in the etag computation in order to invalidate the etag once a node language gets updated or once the
		 * display name of any parent node changes.
		 */
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {

			WebRootLinkReplacer linkReplacer = tx.data().webRootLinkReplacer();
			String path = linkReplacer.resolve(ac, branch.getUuid(), type, getUuid(), ac.getNodeParameters().getResolveLinks(), getProject()
					.getName(), container.getLanguageTag());
			keyBuilder.append(path);

			// languagePaths
			for (HibNodeFieldContainer currentFieldContainer : getFieldContainers(branch, forVersion(versioiningParameters.getVersion()))) {
				String currLanguage = currentFieldContainer.getLanguageTag();
				keyBuilder.append(currLanguage + "=" + linkReplacer.resolve(ac, branch.getUuid(), type, this, ac.getNodeParameters()
						.getResolveLinks(), currLanguage));
			}

		}

		if (log.isDebugEnabled()) {
			log.debug("Creating etag from key {" + keyBuilder.toString() + "}");
		}
		return keyBuilder.toString();
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/nodes/" + getUuid();
	}

	@Override
	default PermissionChangedProjectElementEventModel onPermissionChanged(HibRole role) {
		PermissionChangedProjectElementEventModel model = new PermissionChangedProjectElementEventModel();
		fillPermissionChanged(model, role);
		return model;
	}

	/**
	 * Update the tags of the node using the provides list of tag references.
	 *
	 * @param ac
	 * @param batch
	 * @param list
	 * @return
	 */
	void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> list);
}
