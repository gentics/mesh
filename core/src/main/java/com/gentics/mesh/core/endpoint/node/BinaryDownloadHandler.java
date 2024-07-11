package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Objects;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.AntivirableBinaryElement;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.ext.web.RoutingContext;

/**
 * Handler for binary or s3binary download requests.
 */
@Singleton
public class BinaryDownloadHandler extends AbstractHandler {

	private final MeshOptions options;
	private final BinaryFieldResponseHandler binaryFieldResponseHandler;
	private final S3BinaryFieldResponseHandler s3binaryFieldResponseHandler;
	private final Database db;

	@Inject
	public BinaryDownloadHandler(MeshOptions options, Database db, BinaryFieldResponseHandler binaryFieldResponseHandler, S3BinaryFieldResponseHandler s3binaryFieldResponseHandler) {
		this.options = options;
		this.db = db;
		this.binaryFieldResponseHandler = binaryFieldResponseHandler;
		this.s3binaryFieldResponseHandler = s3binaryFieldResponseHandler;
	}

	/**
	 * Handle the binary or s3binary download request.
	 *
	 * @param rc
	 *            The routing context for the request.
	 * @param uuid
	 * @param fieldName
	 */
	public void handleReadBinaryField(RoutingContext rc, String uuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.tx(tx -> {
			Project project = tx.getProject(ac);
			Node node = tx.nodeDao().loadObjectByUuid(project, ac, uuid, READ_PUBLISHED_PERM);
			// Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			// if (language == null) {
			// throw error(NOT_FOUND, "error_language_not_found", languageTag);
			// }

			Branch branch = tx.getBranch(ac, node.getProject());
			NodeFieldContainer fieldContainer = tx.contentDao().findVersion(node, ac.getNodeParameters().getLanguageList(options),
				branch.getUuid(),
				ac.getVersioningParameters().getVersion());
			if (fieldContainer == null) {
				throw error(NOT_FOUND, "object_not_found_for_version", ac.getVersioningParameters().getVersion());
			}
			FieldSchema fieldSchema = fieldContainer.getSchemaContainerVersion().getSchema().getField(fieldName);
			if (fieldSchema == null) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}

			Predicate<AntivirableBinaryElement> notAccepted = binary -> binary.getCheckStatus() != BinaryCheckStatus.ACCEPTED
				&& !Objects.equals(binary.getCheckSecret(), rc.queryParams().get("secret"));

			if ((fieldSchema instanceof BinaryFieldSchema)) {
				BinaryField field = fieldContainer.getBinary(fieldName);
				if (field == null) {
					throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
				}

				// The binary can only be downloaded if it has been accepted, or the current request provided the check
				// secret (which should only be known to the check service).
				if (notAccepted.test(field.getBinary())) {
					throw error(NOT_FOUND, "error_binaryfield_not_accepted", fieldName);
				}

				binaryFieldResponseHandler.handle(rc, field);
			} else if ((fieldSchema instanceof S3BinaryFieldSchema)) {
				S3BinaryField field = fieldContainer.getS3Binary(fieldName);
				if (field == null) {
					throw error(NOT_FOUND, "error_s3binaryfield_not_found_with_name", fieldName);
				}

				// The binary can only be downloaded if it has been accepted, or the current request provided the check
				// secret (which should only be known to the check service).
				if (notAccepted.test(field.getBinary())) {
					throw error(NOT_FOUND, "error_binaryfield_not_accepted", fieldName);
				}

				s3binaryFieldResponseHandler.handle(rc, node, field);
			} else {
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}
		});
	}
}
