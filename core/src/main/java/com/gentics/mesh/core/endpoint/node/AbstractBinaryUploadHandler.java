package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.binary.BinaryCheckRequest;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

/**
 * Base class for binary upload handler and S3 binary upload handler.
 */
public class AbstractBinaryUploadHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractBinaryUploadHandler.class);

	protected final MeshOptions options;

	public AbstractBinaryUploadHandler(MeshOptions options) {
		this.options = options;
	}

	/**
	 * If the check service URL is set for the binary field, the check request is performed and the
	 * {@link com.gentics.mesh.core.rest.node.field.BinaryField#setCheckStatus(BinaryCheckStatus) check status} is
	 * updated accordingly.
	 *
	 * @param nodeUuid The UUID of the node.
	 * @param fieldName The binary field name
	 * @param ctx Needed data for performing a binary check request.
	 */
	protected void performBinaryCheck(String nodeUuid, String fieldName, BinaryCheckContext ctx) throws IOException {
		HttpServerConfig serverOptions = options.getHttpServerOptions();
		int port = serverOptions.getPort();
		String host = serverOptions.getHost();

		if ("0.0.0.0".equals(host)) {
			host = "127.0.0.1";
		}

		String baseUrl = new MeshRestClientConfig.Builder().setHost(host).setPort(port).build().getBaseUrl();
		BinaryCheckRequest checkRequest = new BinaryCheckRequest()
			.setFilename(ctx.getFilename())
			.setMimeType(ctx.getContentType())
			.setDownloadUrl(String.format("%s/nodes/%s/binary/%s?secret=%s", baseUrl, nodeUuid, fieldName, ctx.getCheckSecret()))
			.setCallbackUrl(String.format("%s/nodes/%s/binary/%s/checkCallback?secret=%s", baseUrl, nodeUuid, fieldName, ctx.getCheckSecret()));

		OkHttpClient client = new OkHttpClient.Builder().build();
		Request request = new Request.Builder()
			.url(ctx.getCheckServiceUrl())
			.post(RequestBody.create(MediaType.parse("application/json"), checkRequest.toJson()))
			.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.warn("Request to binary check service returned unexpected status: {}", response.code());
			}
		}
	}

}
