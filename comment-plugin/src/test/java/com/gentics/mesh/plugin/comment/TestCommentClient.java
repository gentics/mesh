package com.gentics.mesh.plugin.comment;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.comment.integration.TestDataSetup;
import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.CommentStatus;
import com.gentics.mesh.plugin.comment.model.Order;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestCommentClient {

	private final String baseUrl;
	private final String projectName;
	private final NodeResponse commentedNode;
	private final Map<User, String> tokens;
	private User currentUser = User.ANONYMOUS;

	public TestCommentClient(String baseUrl, String projectName, NodeResponse commentedNode, Map<User, String> tokens) {
		this.baseUrl = baseUrl;
		this.projectName = projectName;
		this.commentedNode = commentedNode;
		this.tokens = tokens;
	}

	public TestCommentClient(String host, int port, TestDataSetup.TestData testData) {
		this("http://" + host + ":" + port, testData);
	}

	public TestCommentClient(String baseUrl, TestDataSetup.TestData testData) {
		this(baseUrl, testData.project.getName(), testData.publicNode, testData.tokens);
	}

	private OkHttpClient client() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
//		builder.writeTimeout(2, TimeUnit.MINUTES);
//		builder.readTimeout(2, TimeUnit.MINUTES);
//		builder.connectTimeout(2, TimeUnit.MINUTES);
		return builder.build();
	}

	private Response request(String method, String path, String json) {
		return request(method, path, json, "/" + projectName + "/plugins/comment");
	}

	private Response request(String method, String path, String json, String baseEndpoint) {
		RequestBody body = json == null ? null : RequestBody.create(MediaType.parse("application/json"), json);
		Request.Builder requestBuilder = new Request.Builder()
			.header("Accept", "application/json, application/javascript")
			.url(baseUrl + "/api/v1" + baseEndpoint + path)
			.method(method, body);

		Optional<String> token = Optional.ofNullable(tokens.get(currentUser));
		token.ifPresent(t -> requestBuilder.header("Authorization", "Bearer " + t));

		Request request = requestBuilder.build();

		try {
			Response response = client().newCall(request).execute();
			if (response.code() >= 500) {
				throw new RuntimeException(String.format("Request failed with code %d and message %s", response.code(), response.body().string()));
			}
			return response;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Response get(String path) {
		return request("GET", path, null);
	}

	public Response post(String path, RestModel data) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
		String strData;
		try {
			strData = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return request("POST", path, strData);
	}

	public Response delete(String path) {
		return request("DELETE", path, null);
	}

	public TestCommentClient setUser(User currentUser) {
		this.currentUser = currentUser;
		return this;
	}

	public Comment addComment(String comment) {
		return addComment(comment, null);
	}

	public Comment addComment(CreateCommentRequest request) {
		return toComment(post("/comments", request));
	}

	public Response addCommentResponse(String comment) {
		return addCommentResponse(null, new CreateCommentRequest().setComment(comment));
	}

	public Response addCommentResponse(Comment parent, CreateCommentRequest request) {
		request.setNode(commentedNode.getUuid())
			.setLanguage(commentedNode.getLanguage());

		Optional.ofNullable(parent)
			.map(Comment::getUuid)
			.ifPresent(request::setParentComment);

		return post("/comments", request);
	}

	public Response addCommentResponse(CreateCommentRequest request) {
		return addCommentResponse(null, request);
	}

	public Comment addComment(String comment, Comment parent) {
		return toComment(addCommentResponse(parent, new CreateCommentRequest().setComment(comment)));
	}

	public Response getCommentResponse(String uuid) {
		return get("/comments/" + uuid);
	}

	public Comment getComment(Comment comment) {
		return getComment(comment.getUuid());
	}

	public Comment getComment(String uuid) {
		return toComment(get("/comments/" + uuid));
	}

	public Comment updateComment(String uuid, UpdateCommentRequest request) {
		return toComment(post("/comments/" + uuid, request));
	}

	public Response updateCommentResponse(String uuid, UpdateCommentRequest request) {
		return post("/comments/" + uuid, request);
	}

	private Comment toComment(Response response) {
		try {
			NodeResponse responseBody = JsonUtil.readValue(response.body().string(), NodeResponse.class);
			return new Comment(responseBody, responseBody.getPermissions());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Response deleteComment(String uuid) {
		return delete("/comments/" + uuid);
	}

	public Response getCommentsOfNode() {
		return getCommentsOfNode(commentedNode, null);
	}

	public Response getCommentsOfNode(Order order) {
		return getCommentsOfNode(commentedNode, order);
	}

	public Response getCommentsOfNode(NodeResponse node, Order order) {
		return getCommentsOfNode(node.getUuid(), node.getLanguage(), order);
	}

	public Response getCommentsOfNode(String uuid, String language) {
		return getCommentsOfNode(uuid, language, null);
	}

	public Response getCommentsOfNode(String uuid, String language, Order order) {
		String orderParam = order == null ? "" : "&order=" + order.getParamValue();
		return get(String.format("/node/%s/comments?lang=%s" + orderParam, uuid, language));
	}

	public Response approve(Comment comment) {
		UpdateCommentRequest req = new UpdateCommentRequest()
			.setStatus(CommentStatus.APPROVED);
		return updateCommentResponse(comment.getUuid(), req);
	}

	public UserResponse me() {
		String body;
		try {
			body = request("GET", "/auth/me", null, "")
				.body().string();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return JsonUtil.readValue(body, UserResponse.class);
	}

	public enum User {
		ANONYMOUS,
		EDITOR,
		ADMIN
	}
}
