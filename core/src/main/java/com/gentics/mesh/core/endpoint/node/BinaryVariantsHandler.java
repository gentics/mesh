package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantResponse;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.ImageManipulationRetrievalParameters;

/**
 * Handler for the web requests of binary variants manipulation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class BinaryVariantsHandler extends AbstractHandler {

	private final BinaryDao binaryDao;
	private final Database db;
	private final MeshOptions options;

	@Inject
	public BinaryVariantsHandler(MeshOptions options, Database db, BinaryDao binaryDao) {
		super();
		this.options = options;
		this.db = db;
		this.binaryDao = binaryDao;
	}

	public void handleDeleteBinaryFieldVariants(InternalActionContext ac, String uuid, String fieldName) {
		wrapVariantsCall(ac, uuid, fieldName, binary -> {
			ImageManipulationRequest request = ac.fromJson(ImageManipulationRequest.class);
			Result<? extends HibImageVariant> result = binaryDao.deleteVariants(binary, request.getVariants(), ac, request.isDeleteOther());
			ImageManipulationRetrievalParameters retrievalParams = ac.getImageManipulationRetrievalParameters();
			int level = retrievalParams.retrieveFilesize() ? 1 : 0;
			List<ImageVariantResponse> variants = result.stream().map(variant -> binaryDao.transformToRestSync(variant, ac, level)).collect(Collectors.toList());
			ImageVariantsResponse response = new ImageVariantsResponse(variants);
			ac.send(response, OK);
		});
	}

	public void handleAddBinaryFieldVariants(InternalActionContext ac, String uuid, String fieldName) {
		wrapVariantsCall(ac, uuid, fieldName, binary -> {
			ImageManipulationRequest request = ac.fromJson(ImageManipulationRequest.class);
			Result<? extends HibImageVariant> result = binaryDao.createVariants(binary, request.getVariants(), ac, request.isDeleteOther());
			ImageManipulationRetrievalParameters retrievalParams = ac.getImageManipulationRetrievalParameters();
			int level = retrievalParams.retrieveFilesize() ? 1 : 0;
			List<ImageVariantResponse> variants = result.stream().map(variant -> binaryDao.transformToRestSync(variant, ac, level)).collect(Collectors.toList());
			ImageVariantsResponse response = new ImageVariantsResponse(variants);
			ac.send(response, OK);
		});
	}

	public void handleListBinaryFieldVariants(InternalActionContext ac, String uuid, String fieldName) {
		wrapVariantsCall(ac, uuid, fieldName, binary -> {
			Result<? extends HibImageVariant> result = binaryDao.getVariants(binary, ac);
			ImageManipulationRetrievalParameters retrievalParams = ac.getImageManipulationRetrievalParameters();
			int level = retrievalParams.retrieveFilesize() ? 1 : 0;
			List<ImageVariantResponse> variants = result.stream().map(variant -> binaryDao.transformToRestSync(variant, ac, level)).collect(Collectors.toList());
			ImageVariantsResponse response = new ImageVariantsResponse(variants);
			ac.send(response, OK);
		});
	}

	protected void wrapVariantsCall(InternalActionContext ac, String uuid, String fieldName, Consumer<HibBinary> consumer) {
		db.tx(tx -> {
			HibProject project = tx.getProject(ac);
			HibNode node = tx.nodeDao().loadObjectByUuid(project, ac, uuid, READ_PUBLISHED_PERM);
			// Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			// if (language == null) {
			// throw error(NOT_FOUND, "error_language_not_found", languageTag);
			// }

			HibBranch branch = tx.getBranch(ac, node.getProject());
			HibNodeFieldContainer fieldContainer = tx.contentDao().findVersion(node, ac.getNodeParameters().getLanguageList(options),
				branch.getUuid(),
				ac.getVersioningParameters().getVersion());
			if (fieldContainer == null) {
				throw error(NOT_FOUND, "object_not_found_for_version", ac.getVersioningParameters().getVersion());
			}
			FieldSchema fieldSchema = fieldContainer.getSchemaContainerVersion().getSchema().getField(fieldName);
			if (fieldSchema == null) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}
			if ((fieldSchema instanceof BinaryFieldSchema)) {
				HibBinaryField field = fieldContainer.getBinary(fieldName);
				if (field == null) {
					throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
				}
				consumer.accept(field.getBinary());
			} else {
				// TODO own error field_unsupported
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}
		});
	}
}
