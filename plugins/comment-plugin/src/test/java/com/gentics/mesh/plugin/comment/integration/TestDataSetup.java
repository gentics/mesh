package com.gentics.mesh.plugin.comment.integration;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ADMIN;
import static com.gentics.mesh.plugin.comment.TestCommentClient.User.EDITOR;
import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.user.UserAPITokenCreateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.plugin.comment.TestClient;
import com.gentics.mesh.plugin.comment.TestCommentClient;
import com.gentics.mesh.rest.client.MeshRestClient;

public class TestDataSetup {

	public static final String PROJECT_NAME = "test";
	private final MeshRestClient client;
	private final TestClient testClient;

	public TestDataSetup(MeshRestClient client) {
		this.client = client;
		testClient = new TestClient(client);
	}

	public TestData setup() {
		return setup(null);
	}

	public TestData setup(String nodeConfigJson) {
		TestData.Builder builder = new TestData.Builder();
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(PROJECT_NAME);
		request.setSchemaRef("folder");
		builder.setProject(call(() -> client.createProject(request)));

		testClient.updateFolderSchema();

		builder.setCommentFolder(testClient.createFolder(builder.getProject(), "comments"));

		UserCreateRequest userCreateRequest = new UserCreateRequest()
			.setUsername("editor")
			.setPassword("test")
			.setEmailAddress("test@example.com");

		builder.setEditorUser(call(() -> client.createUser(userCreateRequest)));
		builder.setAdminUser(call(() -> client.me()));
		builder.setAnonymousUser(testClient.getUser("anonymous"));

		HashMap<TestCommentClient.User, String> tokens = new HashMap<>(TestCommentClient.User.values().length - 1);

		tokens.put(EDITOR, call(() -> client.issueAPIToken(builder.getEditorUser().getUuid(), new UserAPITokenCreateRequest().setName("Test Token"))).getToken());
		tokens.put(ADMIN, call(() -> client.issueAPIToken(builder.getAdminUser().getUuid(), new UserAPITokenCreateRequest().setName("Test Token"))).getToken());
		builder.setTokens(tokens);

		testClient.setEditorPermissions(builder.getProject(), builder.getEditorUser());

		client.setAPIKey(tokens.get(EDITOR));
		builder.setPublicNode(testClient.createFolder(builder.getProject(), "someNode", nodeConfigJson));
		client.setAPIKey(tokens.get(ADMIN));
		testClient.grantPermission(builder.getPublicNode(), testClient.getRole("anonymous"), Permission.READ, Permission.READ_PUBLISHED);

		return builder.build();
	}

	public static class TestData {
		public final ProjectResponse project;

		public final NodeResponse commentFolder;
		public final NodeResponse publicNode;
		public final NodeResponse playground;

		public final UserResponse anonymousUser;
		public final UserResponse editorUser;
		public final UserResponse adminUser;

		public final Map<TestCommentClient.User, String> tokens;

		public TestData(Builder builder) {
			this.project = builder.project;
			this.publicNode = builder.publicNode;
			this.commentFolder = builder.commentFolder;
			this.playground = builder.playground;
			this.anonymousUser = builder.anonymousUser;
			this.editorUser = builder.editorUser;
			this.adminUser = builder.adminUser;
			this.tokens = builder.tokens;
		}

		public static class Builder {
			private ProjectResponse project;

			private NodeResponse commentFolder;
			private NodeResponse publicNode;
			private NodeResponse playground;

			private UserResponse anonymousUser;
			private UserResponse editorUser;
			private UserResponse adminUser;
			private HashMap<TestCommentClient.User, String> tokens;

			public TestData build() {
				return new TestData(this);
			}

			public ProjectResponse getProject() {
				return project;
			}

			public Builder setProject(ProjectResponse project) {
				this.project = project;
				return this;
			}

			public NodeResponse getCommentFolder() {
				return commentFolder;
			}

			public Builder setCommentFolder(NodeResponse commentFolder) {
				this.commentFolder = commentFolder;
				return this;
			}

			public NodeResponse getPublicNode() {
				return publicNode;
			}

			public Builder setPublicNode(NodeResponse publicNode) {
				this.publicNode = publicNode;
				return this;
			}

			public NodeResponse getPlayground() {
				return playground;
			}

			public Builder setPlayground(NodeResponse playground) {
				this.playground = playground;
				return this;
			}

			public UserResponse getAnonymousUser() {
				return anonymousUser;
			}

			public Builder setAnonymousUser(UserResponse anonymousUser) {
				this.anonymousUser = anonymousUser;
				return this;
			}

			public UserResponse getEditorUser() {
				return editorUser;
			}

			public Builder setEditorUser(UserResponse editorUser) {
				this.editorUser = editorUser;
				return this;
			}

			public UserResponse getAdminUser() {
				return adminUser;
			}

			public Builder setAdminUser(UserResponse adminUser) {
				this.adminUser = adminUser;
				return this;
			}

			public void setTokens(HashMap<TestCommentClient.User, String> tokens) {
				this.tokens = tokens;
			}

			public HashMap<TestCommentClient.User, String> getTokens() {
				return tokens;
			}
		}
	}
}
