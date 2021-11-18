package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.*;
import static com.gentics.mesh.core.rest.common.ContainerType.*;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Arrays;
import java.util.Set;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
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
		ContentDao contentDao = Tx.get().contentDao();

		HibNodeFieldContainer container = contentDao.findVersion(this, nodeParameters.getLanguageList(Tx.get().data().options()), Tx.get().getBranch(ac, getProject()).getUuid(),
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
		ContentDao contentDao = tx.contentDao();

		StringBuilder keyBuilder = new StringBuilder();

		// Parameters
		HibBranch branch = tx.getBranch(ac, getProject());
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		HibNode parentNode = nodeDao.getParentNode(this, branch.getUuid());
		HibNodeFieldContainer container = contentDao.findVersion(this, ac.getNodeParameters().getLanguageList(tx.data().options()), branch.getUuid(),
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
		for (HibNodeFieldContainer c : nodeDao.getFieldContainers(this, branch.getUuid(), PUBLISHED)) {
			keyBuilder.append(c.getLanguageTag() + "published");
		}
		for (HibNodeFieldContainer c : nodeDao.getFieldContainers(this, branch.getUuid(), DRAFT)) {
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
			for (HibNodeFieldContainer currentFieldContainer : nodeDao.getFieldContainers(this, branch.getUuid(), forVersion(versioiningParameters.getVersion()))) {
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

	@Override
	default boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive,
									Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		boolean permissionChanged = false;
		if (recursive) {
			// We don't need to filter by branch. Branch nodes can't have dedicated perms
			for (HibNode child : Tx.get().nodeDao().getChildren(this)) {
				permissionChanged = child.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = HibCoreElement.super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}
}
