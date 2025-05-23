package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for s3binary upload requests. This class is responsible only for the creation of the necessary Mesh fields. The real upload is done between the client and the AWS.
 */
public class S3BinaryUploadHandlerImpl extends AbstractBinaryUploadHandler implements S3BinaryUploadHandler {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryUploadHandlerImpl.class);
	private final S3BinaryStorage s3BinaryStorage;
	private final HandlerUtilities utils;
	private final S3Binaries s3binaries;

	@Inject
	public S3BinaryUploadHandlerImpl(
			Database db,
			S3BinaryStorage s3BinaryStorage,
			HandlerUtilities utils,
			MeshOptions options,
			S3Binaries s3binaries,
			BinaryStorage binaryStorage) {
		super(db, null, binaryStorage, options);

		this.s3BinaryStorage = s3BinaryStorage;
		this.utils = utils;
		this.s3binaries = s3binaries;
	}

	/**
	 * Handle a request to create a new field.
	 *
	 * @param ac
	 * @param nodeUuid   UUID of the node which should be updated
	 * @param fieldName  Name of the field which should be created
	 */
	public void handleUpdateField(InternalActionContext ac, String nodeUuid, String fieldName) {
		validateParameter(nodeUuid, "uuid");
		validateParameter(fieldName, "fieldName");

		S3BinaryUploadRequest s3BinaryUploadRequest = JsonUtil.readValue(ac.getBodyAsString(), S3BinaryUploadRequest.class);
		String fileName = s3BinaryUploadRequest.getFilename();
		if (isEmpty(fileName)) {
			throw error(BAD_REQUEST, "upload_error_body_no_filename");
		}

		String languageTag = s3BinaryUploadRequest.getLanguage();
		if (isEmpty(languageTag)) {
			throw error(BAD_REQUEST, "upload_error_no_language");
		}

		String nodeVersion = s3BinaryUploadRequest.getVersion();
		if (isEmpty(nodeVersion)) {
			throw error(BAD_REQUEST, "upload_error_no_version");
		}

		S3UploadContext s3UploadContext = new S3UploadContext();
		s3UploadContext.setS3ObjectKey(S3BinaryStorage.makeObjectKey(nodeUuid, fieldName, languageTag));
		// Create a new s3 binary uuid if the data was not already stored
		s3UploadContext.setS3BinaryUuid(UUIDUtil.randomUUID());
		s3UploadContext.setFileName(fileName);

		s3BinaryStorage.createUploadPresignedUrl(options.getS3Options().getBucket(), s3UploadContext.getS3ObjectKey(), nodeVersion, false).flatMapObservable(s3RestResponse ->
				storeUploadInGraph(ac, s3UploadContext, nodeUuid, languageTag, nodeVersion, fieldName)
						.flatMapObservable((uploadedNode) -> {
							s3RestResponse.setVersion(uploadedNode.getVersion());
							return Observable.just(s3RestResponse);
						})
		).subscribe(model ->
				ac.send(model, CREATED), ac::fail);
	}

	private Single<NodeResponse> storeUploadInGraph(InternalActionContext ac, S3UploadContext context,
													String nodeUuid,
													String languageTag, String nodeVersion,
													String fieldName) {
		String s3binaryUuid = context.getS3BinaryUuid();
		String s3ObjectKey = context.getS3ObjectKey();
		String fileName = context.getFileName();

		return db.singleTxWriteLock(
			(batch, tx) -> {
				ContentDao contentDao = tx.contentDao();
				HibProject project = tx.getProject(ac);
				HibBranch branch = tx.getBranch(ac);
				NodeDao nodeDao = tx.nodeDao();
				HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);

				// Load the current latest draft
				HibNodeFieldContainer latestDraftVersion = contentDao.getFieldContainer(node, languageTag, branch, ContainerType.DRAFT);

				if (latestDraftVersion == null) {
					throw error(NOT_FOUND, "error_language_not_found", languageTag);
				}

				FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);

				if (fieldSchema == null) {
					throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
				}

				if (!(fieldSchema instanceof S3BinaryFieldSchema)) {
					// TODO Add support for other field types
					throw error(BAD_REQUEST, "error_found_field_is_not_s3_binary", fieldName);
				}

				// We need to check whether someone else has stored the s3 binary in the meanwhile
				S3HibBinary s3HibBinary = s3binaries.findByS3ObjectKey(s3ObjectKey).runInExistingTx(tx);
				if (s3HibBinary == null) {
					BinaryCheckStatus checkStatus = StringUtils.isNotBlank(((S3BinaryFieldSchema) fieldSchema).getCheckServiceUrl())
							? BinaryCheckStatus.POSTPONED
							: BinaryCheckStatus.ACCEPTED;						
					s3HibBinary = s3binaries.create(s3binaryUuid, s3ObjectKey, fileName, checkStatus).runInExistingTx(tx);
				}

				HibLanguage language = tx.languageDao().findByLanguageTag(project, languageTag);
				if (language == null) {
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

				// Create a new node version field container to store the upload
				HibNodeFieldContainer newDraftVersion = contentDao.createFieldContainer(node, languageTag, branch, ac.getUser(),
					latestDraftVersion,
					true);
				// Create the new field
				S3HibBinaryField field = newDraftVersion.createS3Binary(fieldName, s3HibBinary);

				// Now get rid of the old field
				// If the s3 binary field is the segment field, we need to update the webroot info in the node
				if (field.getFieldKey().equals(contentDao.getSchemaContainerVersion(newDraftVersion).getSchema().getSegmentField())) {
					contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
				}

				if (ac.isPurgeAllowed() && contentDao.isAutoPurgeEnabled(newDraftVersion) && contentDao.isPurgeable(latestDraftVersion)) {
					contentDao.purge(latestDraftVersion);
				}

				batch.add(contentDao.onUpdated(newDraftVersion, branch.getUuid(), DRAFT));
				batch.add(s3HibBinary.onCreated(nodeUuid, s3ObjectKey));

				if (StringUtils.isNotBlank(((S3BinaryFieldSchema) fieldSchema).getCheckServiceUrl())) {
					field.getBinary().setCheckStatus(BinaryCheckStatus.POSTPONED);
					field.getBinary().setCheckSecret(UUIDUtil.randomUUID());
				} else {
					field.getBinary().setCheckStatus(BinaryCheckStatus.ACCEPTED);
				}

				return nodeDao.transformToRestSync(node, ac, 0);
			});
	}
}
