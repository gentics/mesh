package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.node.BinaryCheckUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Base class for binary upload handler and S3 binary upload handler.
 */
public class AbstractBinaryUploadHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractBinaryUploadHandler.class);

	protected final Database db;
	protected final Binaries binaries;
	protected final BinaryStorage binaryStorage;

	protected final MeshOptions options;

	public AbstractBinaryUploadHandler(Database db, Binaries binaries, BinaryStorage binaryStorage, MeshOptions options) {
		this.db = db;
		this.binaries = binaries;
		this.binaryStorage = binaryStorage;
		this.options = options;
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

		String languageTag = ac.getParameter("lang");

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
				CommonTx ctx = tx.unwrap();
				HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);

				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				HibProject project = tx.getProject(ac);
				HibBranch branch = tx.getBranch(ac);
				NodeDao nodeDao = tx.nodeDao();
				HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);
				HibNodeFieldContainer nodeFieldContainer = ctx.contentDao().findVersion(node, languageTag, branch.getUuid(), nodeVersion);

				if (nodeFieldContainer == null) {
					throw error(BAD_REQUEST, "object_not_found_for_uuid_version", nodeUuid, nodeVersion);
				}

				FieldSchema fieldSchema = nodeFieldContainer.getSchemaContainerVersion().getSchema().getField(fieldName);

				if (fieldSchema == null) {
					throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
				}

				if (!(fieldSchema instanceof BinaryFieldSchema)) {
					throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
				}

				HibBinaryField binaryField = nodeFieldContainer.getBinary(fieldName);

				if (binaryField == null) {
					throw error(BAD_REQUEST, "error_binaryfield_not_found_with_name", fieldName);
				}

				if (binaryField.getBinary().getCheckStatus() != BinaryCheckStatus.POSTPONED) {
					throw error(BAD_REQUEST, "error_binaryfield_check_already_performed", fieldName);
				}

				if (!checkSecret.equals(binaryField.getBinary().getCheckSecret())) {
					throw error(BAD_REQUEST, "error_binaryfield_invalid_check_secret", fieldName);
				}

				BinaryCheckUpdateRequest request = ac.fromJson(BinaryCheckUpdateRequest.class);
				HibBinary binary = binaryField.getBinary();

				binary.setCheckStatus(request.getStatus());

				if (request.getStatus() == BinaryCheckStatus.DENIED) {
					binaryStorage.delete(binary.getUuid())
						.andThen(binaryStorage.store(Flowable.empty(), binary.getUuid()))
						.subscribe();
				}

				ctx.persist(binary);

				return nodeDao.transformToRestSync(node, ac, 0);
			})
			.subscribe(model -> ac.send(model, OK), ac::fail);
	}
}
