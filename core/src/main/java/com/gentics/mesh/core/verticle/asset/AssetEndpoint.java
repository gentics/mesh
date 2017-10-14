package com.gentics.mesh.core.verticle.asset;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractProjectEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AssetEndpoint extends AbstractProjectEndpoint {

	private static final Logger log = LoggerFactory.getLogger(AssetEndpoint.class);

	private AssetCrudHandler assetCrudHandler;

	public AssetEndpoint() {
		super("assets", null, null);
	}

	public AssetEndpoint(BootstrapInitializer boot, RouterStorage routerStorage, AssetCrudHandler assetCrudHandler) {
		super("assets", boot, routerStorage);
		this.assetCrudHandler = assetCrudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow operations on assets";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addCreateHandler();
		addReadHandler();
		addDeleteHandler();
		addDownloadHandler();
	}

	private void addDownloadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:assetUuid/bin");
		readOne.addUriParameter("assetUuid", "Uuid of the asset.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Download the asset binary with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, assetExamples.createAssetResponse("flower.jpg"), "Loaded asset.");
		readOne.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("assetUuid");
			assetCrudHandler.handleDownload(ac, uuid);
		});
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.description("Create a new asset.");
		endpoint.path("/:assetUud").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		endpoint.addUriParameter("assetUuid", "Uuid of the asset.", UUIDUtil.randomUUID());
		// endpoint.exampleRequest(assetExamples..createTagCreateRequest("red"));
		endpoint.exampleResponse(OK, assetExamples.createAssetResponse("flower.jpg"), "Created asset");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String assetUuid = ac.getParameter("assetUuid");
			assetCrudHandler.handleCreate(ac, assetUuid);
		});

	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:assetUuid");
		readOne.addUriParameter("assetUuid", "Uuid of the asset.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Read the asset with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, assetExamples.createAssetResponse("flower.jpg"), "Loaded asset.");
		readOne.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("assetUuid");
			assetCrudHandler.handleRead(ac, uuid);
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.description("Load multiple assets and return a paged list response.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.exampleResponse(OK, assetExamples.createAssetListResponse(), "Loaded asset.");
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			assetCrudHandler.handleReadList(ac);
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:assetUuid");
		endpoint.addUriParameter("assetUuid", "Uuid of the asset.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the asset.");
		endpoint.exampleResponse(NO_CONTENT, "Asset was deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("assetUuid");
			assetCrudHandler.handleDelete(ac, uuid);
		});
	}

}
