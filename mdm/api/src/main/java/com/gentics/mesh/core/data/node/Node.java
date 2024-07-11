package com.gentics.mesh.core.data.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.BucketableElement;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.Taggable;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.user.CreatorTracking;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;

public interface Node extends CoreElement<NodeResponse>, CreatorTracking, 
		BucketableElement, TransformableElement<NodeResponse>, ProjectElement, Taggable {

	static final TypeInfo TYPE_INFO = new TypeInfo(ElementType.NODE, NODE_CREATED, NODE_UPDATED, NODE_DELETED);

	@Override
	default boolean hasPublishPermissions() {
		return true;
	}

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
	void addTag(Tag tag, Branch branch);

	/**
	 * Remove the given tag from the list of tags for this node in the given branch.
	 *
	 * @param tag
	 * @param branch
	 */
	void removeTag(Tag tag, Branch branch);

	/**
	 * Remove all tags for the given branch.
	 *
	 * @param branch
	 */
	void removeAllTags(Branch branch);

	/**
	 * Return a list of all tags that were assigned to this node in the given branch.
	 *
	 * @param branch
	 * @return
	 */
	Result<Tag> getTags(Branch branch);

	/**
	 * Set the project of the node.
	 *
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Returns the parent node of this node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Node getParentNode(String branchUuid);

	/**
	 * Return the schema container for the node.
	 *
	 * @return
	 */
	Schema getSchemaContainer();

	/**
	 * Set the schema container of the node.
	 *
	 * @param container
	 */
	void setSchemaContainer(Schema container);

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
	default PermissionChangedProjectElementEventModel onPermissionChanged(Role role) {
		PermissionChangedProjectElementEventModel model = new PermissionChangedProjectElementEventModel();
		fillPermissionChanged(model, role);
		return model;
	}

	@Override
	default boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, Role role, boolean recursive,
                                     Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		UserDao userDao = Tx.get().userDao();
		boolean permissionChanged = false;
		if (recursive) {
			// We don't need to filter by branch. Branch nodes can't have dedicated perms
			for (Node child : Tx.get().nodeDao().getChildren(this).stream().filter(e -> userDao.hasPermission(authUser.getDelegate(), this, InternalPermission.READ_PERM)).collect(Collectors.toList())) {
				permissionChanged = child.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = CoreElement.super.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}
}
