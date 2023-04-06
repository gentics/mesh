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
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.node.BinaryCheckUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.etc.config.MeshOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Base class for binary upload handler and S3 binary upload handler.
 */
public class AbstractBinaryUploadHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractBinaryUploadHandler.class);

	private static final String EMPTY_SHA_512_HASH = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";

	protected final Database db;
	protected final Binaries binaries;
	protected final MeshOptions options;

	public AbstractBinaryUploadHandler(Database db, Binaries binaries, MeshOptions options) {
		this.db = db;
		this.binaries = binaries;
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
				PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();
				HibProject project = tx.getProject(ac);
				HibBranch branch = tx.getBranch(ac);
				NodeDao nodeDao = tx.nodeDao();
				HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);
				HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);

				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				HibNodeFieldContainer nodeFieldContainer = contentDao.findVersion(node, languageTag, branch.getUuid(), nodeVersion);

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
				HibBinary newBinary;

				if (request.getStatus() == BinaryCheckStatus.ACCEPTED) {
					newBinary = binaryField.getBinary();
					newBinary.setCheckStatus(BinaryCheckStatus.ACCEPTED);
				} else {
					HibBinary existingBinary = binaries.findByHash(EMPTY_SHA_512_HASH).runInExistingTx(tx);

					newBinary = existingBinary != null
						? existingBinary
						: binaries.create(EMPTY_SHA_512_HASH, 0, BinaryCheckStatus.DENIED).runInExistingTx(tx);
				}

				if (newBinary != binaryField.getBinary()) {
					// Create the new field
					HibBinaryField field = nodeFieldContainer.createBinary(fieldName, newBinary);

					// Reuse the existing properties
					binaryField.copyTo(field);

					// Now get rid of the old field
					nodeFieldContainer.removeField(binaryField);

					// If the binary field is the segment field, we need to update the webroot info in the node
					// TODO FIXME This is already called in `PersistingContentDao.connectFieldContainer()`. Normally one should not update a container without reconnecting versions,
					// but currently MeshLocalClient does this, which may be illegal. The check and call below should be removed, once MeshLocalClientImpl is improved.
					if (field.getFieldKey().equals(contentDao.getSchemaContainerVersion(nodeFieldContainer).getSchema().getSegmentField())) {
						contentDao.updateWebrootPathInfo(nodeFieldContainer, branch.getUuid(), "node_conflicting_segmentfield_upload");
					}

					if (ac.isPurgeAllowed() && contentDao.isAutoPurgeEnabled(nodeFieldContainer) && contentDao.isPurgeable(nodeFieldContainer)) {
						contentDao.purge(nodeFieldContainer);
					}
				}

				// TODO: Do we need this update, when only the check status of the binary was changed? This was not necessarily done on the DRAFT version.
				// batch.add(contentDao.onUpdated(nodeFieldContainer, branch.getUuid(), DRAFT));

				return nodeDao.transformToRestSync(node, ac, 0);
			})
			.subscribe(model -> ac.send(model, OK), ac::fail);
	}
}
