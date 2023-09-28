package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingImageVariantDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
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

	private final PersistingImageVariantDao imageVariantDao;
	private final Database db;
	private final MeshOptions options;

	@Inject
	public BinaryVariantsHandler(MeshOptions options, Database db, PersistingImageVariantDao imageVariantDao) {
		super();
		this.options = options;
		this.db = db;
		this.imageVariantDao = imageVariantDao;
	}

	/**
	 * Handle deletion of the image variants for the node with given UUID.
	 * 
	 * @param ac
	 * @param uuid
	 * @param fieldName
	 */
	public void handleDeleteBinaryFieldVariants(InternalActionContext ac, String uuid, String fieldName) {
		wrapVariantsCall(ac, uuid, fieldName, binaryField -> {
			ImageManipulationRequest request = StringUtils.isNotBlank(ac.getBodyAsString()) ? ac.fromJson(ImageManipulationRequest.class) : new ImageManipulationRequest().setVariants(Collections.emptyList()).setDeleteOther(true);
			if (request.isDeleteOther()) {
				imageVariantDao.retainVariants(binaryField, request.getVariants(), ac);
			} else {
				imageVariantDao.deleteVariants(binaryField, request.getVariants(), ac);
			}
			ac.send(NO_CONTENT);
		});
	}

	/**
	 * Handle creation of the image variants for the node with given UUID.
	 * 
	 * @param ac
	 * @param nodeUuid
	 * @param fieldName
	 */
	public void handleUpsertBinaryFieldVariants(InternalActionContext ac, String nodeUuid, String fieldName) {
		wrapVariantsCall(ac, nodeUuid, fieldName, binaryField -> {
			HibBinary binary = binaryField.getBinary();
			ImageManipulationRequest request = ac.fromJson(ImageManipulationRequest.class);
			Result<? extends HibImageVariant> result = imageVariantDao.createVariants(binaryField, request.getVariants(), ac, request.isDeleteOther());
			ImageManipulationRetrievalParameters retrievalParams = ac.getImageManipulationRetrievalParameters();
			int level = retrievalParams.retrieveFilesize() ? 1 : 0;
			List<ImageVariantResponse> variants = result.stream().map(variant -> imageVariantDao.transformToRestSync(variant, ac, level)).collect(Collectors.toList());
			if (retrievalParams.retrieveOriginal()) {
				variants = Stream.of(
						Stream.of(imageVariantDao.transformBinaryToRestVariantSync(binary, ac, retrievalParams.retrieveFilesize())),
						variants.stream()
					).flatMap(Function.identity()).collect(Collectors.toList());
			}
			ImageVariantsResponse response = new ImageVariantsResponse(variants);
			ac.send(response, OK);
		});
	}

	/**
	 * Handle list of the image variants for the node with given UUID.
	 * 
	 * @param ac
	 * @param uuid
	 * @param fieldName
	 */
	public void handleListBinaryFieldVariants(InternalActionContext ac, String uuid, String fieldName) {
		wrapVariantsCall(ac, uuid, fieldName, binaryField -> {
			Result<? extends HibImageVariant> result = binaryField.getImageVariants();
			ImageManipulationRetrievalParameters retrievalParams = ac.getImageManipulationRetrievalParameters();
			int level = retrievalParams.retrieveFilesize() ? 1 : 0;
			List<ImageVariantResponse> variants = result.stream().map(variant -> imageVariantDao.transformToRestSync(variant, ac, level)).collect(Collectors.toList());
			if (retrievalParams.retrieveOriginal()) {
				variants = Stream.of(
						Stream.of(imageVariantDao.transformBinaryToRestVariantSync(binaryField.getBinary(), ac, retrievalParams.retrieveFilesize())),
						variants.stream()
					).flatMap(Function.identity()).collect(Collectors.toList());
			}
			ImageVariantsResponse response = new ImageVariantsResponse(variants);
			ac.send(response, OK);
		});
	}

	/**
	 * Pick all the variant manipulation parameters from the action context.
	 * 
	 * @param ac
	 * @param nodeUuid
	 * @param fieldName
	 * @param consumer
	 */
	protected void wrapVariantsCall(InternalActionContext ac, String nodeUuid, String fieldName, Consumer<HibBinaryField> consumer) {
		db.tx(tx -> {
			ContainerType version = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
			HibProject project = tx.getProject(ac);
			HibNode node = tx.nodeDao().loadObjectByUuid(project, ac, nodeUuid, version == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM);
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
				consumer.accept(field);
			} else {
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}
		});
	}
}
