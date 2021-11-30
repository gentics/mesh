package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Set;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.data.Taggable;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.VersioningParameters;

public interface HibNode extends HibCoreElement<NodeResponse>, HibCreatorTracking, 
		HibBucketableElement, HibTransformableElement<NodeResponse>, Taggable {

	static final TypeInfo TYPE_INFO = new TypeInfo(ElementType.NODE, NODE_CREATED, NODE_UPDATED, NODE_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

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

	@Override
	default String getSubETag(InternalActionContext ac) {
		return Tx.get().nodeDao().getSubETag(this, ac);
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
