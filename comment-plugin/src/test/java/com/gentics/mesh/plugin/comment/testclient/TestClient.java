package com.gentics.mesh.plugin.comment.testclient;

import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.util.UUIDUtil;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.gentics.mesh.plugin.comment.TestUtil.loadUserResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestClient extends AbstractTestClient {

	private static final Logger log = LoggerFactory.getLogger(TestClient.class);

	private List<SchemaResponse> schemas = new ArrayList<>();
	private List<SchemaCreateRequest> createdSchemas = new ArrayList<>();
	private UserResponse user = loadUserResponse("anonymous");

	public List<SchemaResponse> getSchemas() {
		return schemas;
	}

	public TestClient setSchemas(List<SchemaResponse> schemas) {
		this.schemas = schemas;
		return this;
	}

	public UserResponse getUser() {
		return user;
	}

	public TestClient setUser(UserResponse user) {
		this.user = user;
		return this;
	}

	public List<SchemaCreateRequest> getCreatedSchemas() {
		return createdSchemas;
	}

	@Override
	public MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters) {
		SchemaListResponse response = new SchemaListResponse();
		response.getData().addAll(schemas);
		response.setMetainfo(generateMetaInfo(schemas));
		return mockResponse(response);
	}

	@Override
	public MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest request, ParameterProvider... parameters) {
		SchemaResponse response = TestClientUtil.createSchemaResponse(request);
		createdSchemas.add(request);
		return mockResponse(response);
	}

	@Override
	public MeshRequest<SchemaResponse> assignSchemaToProject(String projectName, String schemaUuid) {
		return mockResponse();
	}

	@Override
	public MeshRequest<UserResponse> me(ParameterProvider ...parameters) {
		return mockResponse(user);
	}

	@Override
	public MeshRequest<GraphQLResponse> graphql(String projectName, GraphQLRequest request, ParameterProvider... parameters) {
		log.info("Handling graphql");
		GraphQLResponse graphql = new GraphQLResponse();
		JsonObject data = getResponse("comments.json");
		graphql.setData(data);
		return mockResponse(graphql);
	}

	@Override
	public MeshRequest<EmptyResponse> deleteNode(String projectName, String uuid, ParameterProvider... parameters) {
		log.info("Handling deleteNode");
		return mockResponse();
	}

	@Override
	public MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters) {
		log.info("Handling publish");
		return mockResponse();
	}

	@Override
	public MeshRequest<EmptyResponse> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters) {
		log.info("Handling takeNodeOffline");
		return mockResponse();
	}

	@Override
	public MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters) {
		log.info("Handling findNodeByUuid");
		return mockResponse(createNodeResponse(uuid));
	}

	@Override
	public MeshRequest<MeshWebrootResponse> webroot(String projectName, String path, ParameterProvider... parameters) {
		log.info("Handling webroot");
		MeshWebrootResponse response = mock(MeshWebrootResponse.class);
		when(response.getNodeResponse()).thenReturn(createNodeResponse(UUIDUtil.randomUUID()));
		return mockResponse(response);
	}

	@Override
	public MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest,
		ParameterProvider... parameters) {
		log.info("Handling updateNode");
		return mockResponse(createNodeResponse(uuid));
	}

	@Override
	public MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters) {
		log.info("Handling createNode");
		return mockResponse(createNodeResponse(UUIDUtil.randomUUID()));
	}

	private PagingMetaInfo generateMetaInfo(Collection<?> items) {
		long size = items.size();
		return new PagingMetaInfo()
			.setCurrentPage(1)
			.setPageCount(1)
			.setPerPage(size)
			.setTotalCount(size);
	}

	private NodeResponse createNodeResponse(String uuid) {
		NodeResponse response = new NodeResponse();

		response.setFields(new FieldMapImpl());
		response.setLanguage("en");
		response.setUuid(uuid);
		response.setVersion("1.0");
		response.getFields().put("hidden", new BooleanFieldImpl().setValue(false));
		//response.getFields().put("email", new StringFieldImpl().setString("some@where.tld"));
		return response;
	}

	private <T> MeshRequest<T> mockResponse() {
		@SuppressWarnings("unchecked")
		MeshRequest<T> mock = mock(MeshRequest.class);
		when(mock.toCompletable()).thenReturn(Completable.complete());
		return mock;
	}

	private <T> MeshRequest<T> mockResponse(T value) {
		@SuppressWarnings("unchecked")
		MeshRequest<T> mock = mock(MeshRequest.class);
		when(mock.toSingle()).thenReturn(Single.just(value));
		when(mock.toCompletable()).thenReturn(Completable.complete());
		when(mock.toObservable()).thenReturn(Observable.just(value));
		return mock;
	}

	/**
	 * Load the response JSON.
	 */
	private JsonObject getResponse(String name) {
		try {
			return new JsonObject(IOUtils.toString(getClass().getResource("/responses/" + name), Charset.defaultCharset()));
		} catch (IOException e) {
			log.error("Error while loading example response {" + name + "}", e);
			return null;
		}
	}

}
