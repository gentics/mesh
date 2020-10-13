package com.gentics.mesh.plugin.comment;

import static com.gentics.mesh.plugin.comment.integration.TestDataSetup.PROJECT_NAME;
import static com.gentics.mesh.test.ClientHelper.call;

import java.util.function.Consumer;
import java.util.function.Function;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class TestClient {
	private final MeshRestClient client;

	public TestClient(MeshRestClient client) {
		this.client = client;
	}

	public void updateFolderSchema() {
		client.findSchemas(PROJECT_NAME).toSingle()
			.flatMapObservable(schemas -> Observable.fromIterable(schemas.getData()))
			.filter(schema -> schema.getName().equals("folder"))
			.singleOrError()
			.flatMapCompletable(this::doFolderUpdate)
			.blockingAwait();
	}

	public Completable doFolderUpdate(SchemaResponse schemaResponse) {
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setFields(schemaResponse.getFields());
		request.getFields().add(new StringFieldSchemaImpl().setName("commentConfig"));
		request.setName(schemaResponse.getName());
		request.setSegmentField(schemaResponse.getSegmentField());
		return client.updateSchema(schemaResponse.getUuid(), request).toCompletable();
	}

	public RoleResponse getRole(String roleName) {
		return client.findRoles().toSingle()
			.flatMapObservable(response -> Observable.fromIterable(response.getData()))
			.filter(role -> role.getName().equals(roleName))
			.singleOrError()
			.blockingGet();
	}

	public void grantPermission(NodeResponse node, RoleResponse role, Permission... permissions) {
		RolePermissionRequest req = new RolePermissionRequest();
		req.setRecursive(true);
		for (Permission p : permissions) {
			req.getPermissions().add(p);
		}
		req.getPermissions().setOthers(false);
		client.updateRolePermissions(role.getUuid(), PROJECT_NAME + "/nodes/" + node.getUuid(), req).toCompletable().blockingAwait();
	}

	public NodeResponse createFolder(ProjectResponse project, String name) {
		return createFolder(project, name, null);
	}

	public NodeResponse createFolder(ProjectResponse project, String name, String nodeConfigJson) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setLanguage("en")
			.setParentNodeUuid(project.getRootNode().getUuid())
			.setSchemaName("folder");
		request.getFields().put("name", new StringFieldImpl().setString(name));
		request.getFields().put("slug", new StringFieldImpl().setString(name));
		if (nodeConfigJson != null) {
			request.getFields().put("commentConfig", new StringFieldImpl().setString(nodeConfigJson));
		}
		return call(() -> client.createNode(PROJECT_NAME, request));
	}

	public NodeResponse createFolder(NodeResponse parent, String name) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setLanguage("en")
			.setParentNodeUuid(parent.getUuid())
			.setSchemaName("folder");
		request.getFields().put("name", new StringFieldImpl().setString(name));
		request.getFields().put("slug", new StringFieldImpl().setString(name));
		return call(() -> client.createNode(PROJECT_NAME, request));
	}

	public void setEditorPermissions(ProjectResponse project, UserResponse user) {
		RoleResponse role = createRole();
		createAndAssignGroup(role, user);

		RolePermissionRequest allPermissions = new RolePermissionRequest();
		allPermissions.setRecursive(true);
		allPermissions.getPermissions().setOthers(true);

		Function<String, Completable> grantAll = path -> client.updateRolePermissions(role.getUuid(), path, allPermissions).toCompletable();
		Consumer<String> grantAllNow = path -> grantAll.apply(path).blockingAwait();

		client.findSchemas(PROJECT_NAME).toSingle()
			.flatMapObservable(schemas -> Observable.fromIterable(schemas.getData()))
			.flatMapCompletable(schema -> grantAll.apply("/schemas/" + schema.getUuid()))
			.blockingAwait();

		grantAllNow.accept(PROJECT_NAME + "/nodes");
	}

	public RoleResponse createRole() {
		return call(() -> client.createRole(new RoleCreateRequest()
			.setName("editor")));
	}

	public GroupResponse createAndAssignGroup(RoleResponse role, UserResponse user) {
		GroupResponse group = call(() -> client.createGroup(new GroupCreateRequest()
			.setName("editor")));

		call(() -> client.addRoleToGroup(group.getUuid(), role.getUuid()));
		call(() -> client.addUserToGroup(group.getUuid(), user.getUuid()));

		return group;
	}

	public UserResponse getUser(String userName) {
		return client.findUsers().toSingle()
			.flatMapObservable(response -> Observable.fromIterable(response.getData()))
			.filter(user -> user.getUsername().equals(userName))
			.singleOrError()
			.blockingGet();
	}

}
