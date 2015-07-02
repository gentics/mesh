package com.gentics.mesh.test;

import org.junit.Before;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.demo.UserInfo;

public abstract class AbstractBasicObjectTest extends AbstractDBTest implements BasicObjectTestcases {

	private MeshAuthUser requestUser;
	private User user;
	private Role role;
	private Group group;
	private Node content;
	private Node folder;
	private MeshRoot meshRoot;
	private SchemaContainer schemaContainer;

	@Before
	public void setup() throws Exception {
		setupData();
		requestUser = data().getUserInfo().getUser().getImpl().reframe(MeshAuthUserImpl.class);
		group = data().getUserInfo().getGroup();
		user = data().getUserInfo().getUser();
		schemaContainer = data().getSchemaContainer("content");
		role = data().getUserInfo().getRole();

	}

	public User getUser() {
		return user;
	}

	public Role getRole() {
		return role;
	}

	public Group getGroup() {
		return group;
	}

	public Node getContent() {
		return content;
	}

	public Node getFolder() {
		return folder;
	}

	public MeshAuthUser getRequestUser() {
		return requestUser;
	}

	public MeshRoot getMeshRoot() {
		return meshRoot;
	}

	public SchemaContainer getSchemaContainer() {
		return schemaContainer;
	}

}
