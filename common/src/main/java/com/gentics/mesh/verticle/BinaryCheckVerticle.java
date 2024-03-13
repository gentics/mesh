package com.gentics.mesh.verticle;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.binary.BinaryCheckRequest;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Regularly checks for binaries with a check status of POSTPONED and sends
 * the check request to the check service specified in the schema.
 */
@Singleton
public class BinaryCheckVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(BinaryCheckVerticle.class);

	/**
	 * Wrapper class for necessary information for check requests.
	 */
	private static class BinaryCheckContext {

		public final String checkServiceUrl;
		public final String binaryBaseUrl;
		public final String parameters;
		public final String filename;
		public final String contentType;

		private BinaryCheckContext(String checkServiceUrl, String binaryBaseUrl, String parameters, String filename, String contentType) {
			this.checkServiceUrl = checkServiceUrl;
			this.binaryBaseUrl = binaryBaseUrl;
			this.parameters = parameters;
			this.filename = filename;
			this.contentType = contentType;
		}
	}

	private final Database db;
	private final String meshBaseUrl;
	private final long checkIntervall;

	private boolean stopped;

	@Inject
	public BinaryCheckVerticle(Database db, MeshOptions options) {
		this.db = db;

		HttpServerConfig serverOptions = options.getHttpServerOptions();
		int port = serverOptions.getPort();
		String host = serverOptions.getHost();

		if ("0.0.0.0".equals(host)) {
			host = "127.0.0.1";
		}

		this.meshBaseUrl = new MeshRestClientConfig.Builder().setHost(host).setPort(port).build().getBaseUrl();
		this.checkIntervall = options.getUploadOptions().getCheckInterval();
	}

	@Override
	public void start() throws Exception {
		if (checkIntervall <= 0) {
			log.debug("Binary check disabled via check interval setting");

			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("Starting verticle {" + getClass().getName() + "}, checking every " + checkIntervall + "ms");
		}

		stopped = false;

		vertx.setPeriodic(checkIntervall, this::performBinaryCheck);
		super.start();
	}

	@Override
	public void stop() throws Exception {
		stopped = true;
	}

	/**
	 * Perform the check request for all binaries marked as POSTPONED.
	 *
	 * @param id The timer ID.
	 */
	private void performBinaryCheck(Long id) {
		if (stopped) {
			return;
		}

		List<BinaryCheckContext> checkContexts = new ArrayList<>();

		db.tx(tx -> {
			ContentDao contentDao = tx.contentDao();

			tx.binaryDao().findByCheckStatus(BinaryCheckStatus.POSTPONED).runInExistingTx(tx).forEach(binary -> {
				Optional<? extends HibBinaryField> field = tx.binaryDao().findFields(binary).stream().findFirst();

				if (field.isEmpty()) {
					return;
				}

				HibBinaryField binaryField = field.get();
				HibNodeFieldContainer nodeFieldContainer = contentDao.getNodeFieldContainer(binaryField);
				BinaryFieldSchema fieldSchema = (BinaryFieldSchema) nodeFieldContainer.getFieldSchema(binaryField.getFieldKey());
				String lang = nodeFieldContainer.getLanguageTag();
				HibNode node = nodeFieldContainer.getNode();
				String project = node.getProject().getName();
				String nodeUuid = node.getUuid();
				String branchName = null;
				String version = nodeFieldContainer.getVersion().toString();

				for (ContainerType type : ContainerType.values()) {
					Set<String> branches = contentDao.getBranches(nodeFieldContainer, type);

					if (branches.size() > 0) {
						branchName = branches.iterator().next();

						break;
					}
				}

				if (branchName == null) {
					log.warn("No branch found for binary {} (fieldKey: {}, lang: {})", binary.getUuid(), binaryField.getFieldKey(), lang);

					return;
				}

				String binaryBaseUrl = String.format("%s/%s/nodes/%s/binary/%s", meshBaseUrl, project, nodeUuid, binaryField.getFieldKey());
				String parameters = String.format("?secret=%s&lang=%s&branch=%s&version=%s", binary.getCheckSecret(), lang, branchName, version);

				checkContexts.add(new BinaryCheckContext(fieldSchema.getCheckServiceUrl(), binaryBaseUrl, parameters, binaryField.getFileName(), binaryField.getMimeType()));
			});
		});

		for (BinaryCheckContext ctx : checkContexts) {
			checkBinary(ctx);
		}
	}

	/**
	 * If the check service URL is set for the binary field, the check request is performed and the
	 * {@link com.gentics.mesh.core.rest.node.field.BinaryField#setCheckStatus(BinaryCheckStatus) check status} is
	 * updated accordingly.
	 *
	 * @param ctx The binary check context to use
	 */
	public void checkBinary(BinaryCheckContext ctx) {
		BinaryCheckRequest checkRequest = new BinaryCheckRequest()
			.setFilename(ctx.filename)
			.setMimeType(ctx.contentType)
			.setDownloadUrl(ctx.binaryBaseUrl + ctx.parameters)
			.setCallbackUrl(ctx.binaryBaseUrl + "/checkCallback" + ctx.parameters);

		OkHttpClient client = new OkHttpClient.Builder().build();
		Request request = new Request.Builder()
			.url(ctx.checkServiceUrl)
			.post(RequestBody.create(MediaType.parse("application/json"), checkRequest.toJson(true)))
			.build();

		if (log.isTraceEnabled()) {
			log.trace("Performing binary check: POST {}\n{}", ctx.checkServiceUrl, new JsonObject(checkRequest.toJson(false)).encodePrettily());
		}

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.warn("Request to binary check service returned unexpected status: {}", response.code());
			}
		} catch (Exception e) {
			log.debug("Error while posting request to binary check service: {}", e.getMessage());
		}
	}
}
