package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see GroupRoot
 */
public class GroupRootImpl extends AbstractRootVertex<Group> implements GroupRoot {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(GroupRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_GROUP));
		index.createIndex(edgeIndex(HAS_GROUP).withInOut().withOut());
	}

	@Override
	public Class<? extends Group> getPersistanceClass() {
		return GroupImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_GROUP;
	}

	@Override
	public Result<? extends User> getUsers(HibGroup group) {
		return toGraph(group).in(HAS_USER, UserImpl.class);
	}

	@Override
	public Result<? extends Role> getRoles(HibGroup group) {
		return toGraph(group).in(HAS_ROLE, RoleImpl.class);
	}

	@Override
	public long globalCount() {
		return db().count(GroupImpl.class);
	}

	@Override
	public Page<? extends User> getVisibleUsers(Group group, HibUser user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = group.in(HAS_USER);
		return new DynamicTransformablePageImpl<User>(user, traversal, pagingInfo, READ_PERM, UserImpl.class);
	}

	@Override
	public Page<? extends Role> getRoles(Group group, HibUser user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = group.in(HAS_ROLE);
		return new DynamicTransformablePageImpl<Role>(user, traversal, pagingInfo, READ_PERM, RoleImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The group root node can't be deleted");
	}

	@Override
	public Group create() {
		return getGraph().addFramedVertex(GroupImpl.class);
	}
}
