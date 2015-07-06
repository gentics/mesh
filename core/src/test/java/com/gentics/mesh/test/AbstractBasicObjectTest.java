package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;

public abstract class AbstractBasicObjectTest extends AbstractDBTest implements BasicObjectTestcases {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	public User getUser() {
		return data().getUserInfo().getUser();
	}

	public Role getRole() {
		return data().getUserInfo().getRole();
	}

	public Group getGroup() {
		return data().getUserInfo().getGroup();
	}

	public Node getContent() {
		return data().getContent("news overview");
	}

	public Node getFolder() {
		return data().getFolder("2015");
	}

	public MeshAuthUser getRequestUser() {
		return data().getUserInfo().getUser().getImpl().reframe(MeshAuthUserImpl.class);
	}

	public Project getProject() {
		return data().getProject();
	}

	public MeshRoot getMeshRoot() {
		return data().getMeshRoot();
	}

	public SchemaContainer getSchemaContainer() {
		return data().getSchemaContainer("content");
	}

	protected void testPermission(Permission perm, GenericNode node) {
		getRole().addPermissions(node, perm);
		assertTrue(getRole().hasPermission(perm, node));
		assertTrue(getRequestUser().hasPermission(node, perm));
		getRole().revokePermissions(node, perm);
		assertFalse(getRole().hasPermission(perm, node));
		assertFalse(getRequestUser().hasPermission(node, perm));
	}

}
