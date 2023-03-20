package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.BinaryCheckUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.binary.BinaryCheckRequest;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for binary upload handler and S3 binary upload handler.
 */
public class AbstractBinaryUploadHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractBinaryUploadHandler.class);

	protected final Database db;
	protected final MeshOptions options;

	public AbstractBinaryUploadHandler(Database db, MeshOptions options) {
		this.db = db;
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

	/**
	 * Update the check status field of the specified binary field with the
	 * status from the JSON in the request body.
	 *
	 * @param ac The current request.
	 * @param nodeUuid The nodes UUID.
	 * @param fieldName The binary field name.
	 */
	public void handleBinaryCheckResult(InternalActionContext ac, String nodeUuid, String fieldName) {
		validateParameter(nodeUuid, "uuid");
		validateParameter(fieldName, "fieldName");

		String languageTag = ac.getParameter("language");

		if (isEmpty(languageTag)) {
			throw error(BAD_REQUEST, "upload_error_no_language");
		}

		String nodeVersion = ac.getParameter("version");

		if (isEmpty(nodeVersion)) {
			throw error(BAD_REQUEST, "upload_error_no_version");
		}

		String checkSecret = ac.getParameter("secret");

		if (isEmpty(checkSecret)) {
			throw error(BAD_REQUEST, "error_binaryfield_invalid_check_secret", fieldName);
		}

		db.singleTxWriteLock(
				(batch, tx) -> {
					PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();
					HibProject project = tx.getProject(ac);
					HibBranch branch = tx.getBranch(ac);
					NodeDao nodeDao = tx.nodeDao();
					HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);
					HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);

					if (language == null) {
						throw error(NOT_FOUND, "error_language_not_found", languageTag);
					}

					// Load the current latest draft
					HibNodeFieldContainer latestDraftVersion = contentDao.getFieldContainer(node, languageTag, branch, ContainerType.DRAFT);

					if (latestDraftVersion == null) {
						// latestDraftVersion = node.createGraphFieldContainer(language, branch, ac.getUser());
						// TODO Maybe it would be better to just create a new field container for the language?
						// In that case we would also need to:
						// * check for segment field conflicts
						// * update display name
						// * fail if mandatory fields are missing
						throw error(NOT_FOUND, "error_language_not_found", languageTag);
					}

					// Load the base version field container in order to create the diff
					HibNodeFieldContainer baseVersionContainer = contentDao.findVersion(node, languageTag, branch.getUuid(), nodeVersion);

					if (baseVersionContainer == null) {
						throw error(BAD_REQUEST, "node_error_draft_not_found", nodeVersion, languageTag);
					}

					List<FieldContainerChange> baseVersionDiff = contentDao.compareTo(baseVersionContainer, latestDraftVersion);
					List<FieldContainerChange> requestVersionDiff = Arrays.asList(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
					// Compare both sets of change sets
					List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains)
						.collect(Collectors.toList());

					// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
					if (!latestDraftVersion.getVersion().equals(nodeVersion)) {
						// Check whether a conflict has been detected
						if (intersect.size() > 0) {
							NodeVersionConflictException conflictException = new NodeVersionConflictException("node_error_conflict_detected");

							conflictException.setOldVersion(baseVersionContainer.getVersion().toString());
							conflictException.setNewVersion(latestDraftVersion.getVersion().toString());

							for (FieldContainerChange fcc : intersect) {
								conflictException.addConflict(fcc.getFieldCoordinates());
							}

							throw conflictException;
						}
					}

					FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
					if (fieldSchema == null) {
						throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
					}

					if (!(fieldSchema instanceof BinaryFieldSchema)) {
						throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
					}

					// Create a new node version field container to store the upload
					HibNodeFieldContainer newDraftVersion = contentDao.createFieldContainer(node, languageTag, branch, ac.getUser(),
						latestDraftVersion,
						true);

					// Get the potential existing field
					HibBinaryField oldField = (HibBinaryField) contentDao.detachField(newDraftVersion.getBinary(fieldName));

					if (oldField == null) {
						throw error(BAD_REQUEST, "error_binaryfield_not_found_with_name", fieldName);
					}

					// Create the new field
					HibBinaryField field = newDraftVersion.createBinary(fieldName, oldField.getBinary());

					// Reuse the existing properties
					oldField.copyTo(field);

					if (!checkSecret.equals(field.getCheckSecret())) {
						throw error(BAD_REQUEST, "error_binaryfield_invalid_check_secret", fieldName);
					}

					// Now get rid of the old field
					newDraftVersion.removeField(oldField);

					// If the binary field is the segment field, we need to update the webroot info in the node
					// TODO FIXME This is already called in `PersistingContentDao.connectFieldContainer()`. Normally one should not update a container without reconnecting versions,
					// but currently MeshLocalClient does this, which may be illegal. The check and call below should be removed, once MeshLocalClientImpl is improved.
					if (field.getFieldKey().equals(contentDao.getSchemaContainerVersion(newDraftVersion).getSchema().getSegmentField())) {
						contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
					}

					if (ac.isPurgeAllowed() && contentDao.isAutoPurgeEnabled(newDraftVersion) && contentDao.isPurgeable(latestDraftVersion)) {
						contentDao.purge(latestDraftVersion);
					}

					batch.add(contentDao.onUpdated(newDraftVersion, branch.getUuid(), DRAFT));

					BinaryCheckUpdateRequest request = ac.fromJson(BinaryCheckUpdateRequest.class);

					field.setCheckStatus(request.getStatus());

					return nodeDao.transformToRestSync(node, ac, 0);
				})
			.subscribe(model -> ac.send(model, OK), ac::fail);
	}
}
