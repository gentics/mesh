package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.image.ImageInfo;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileProps;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * Handler for binary or s3binary transformer requests.
 */
@Singleton
public class BinaryTransformHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(BinaryTransformHandler.class);

	private final ImageManipulator imageManipulator;
	private final HandlerUtilities utils;
	private final Vertx rxVertx;
	private final Lazy<BootstrapInitializer> boot;
	private final BinaryStorage binaryStorage;
	private final Database db;
	private final Binaries binaries;
	private final S3Binaries s3binaries;
	private final MeshOptions options;

	@Inject
	public BinaryTransformHandler(Database db, HandlerUtilities utils, Vertx rxVertx, ImageManipulator imageManipulator,
								  Lazy<BootstrapInitializer> boot,
								  BinaryStorage binaryStorage, Binaries binaries, S3Binaries s3binaries, MeshOptions options) {
		this.db = db;
		this.utils = utils;
		this.rxVertx = rxVertx;
		this.imageManipulator = imageManipulator;
		this.boot = boot;
		this.binaryStorage = binaryStorage;
		this.binaries = binaries;
		this.s3binaries = s3binaries;
		this.options = options;
	}

	/**
	 * Handle image transformation. This method does a filtering of the required field and
	 * calls the specific binary or s3binary field
	 *
	 * @param rc
	 *            routing context
	 * @param uuid
	 * @param fieldName
	 */
	public void handle(RoutingContext rc, String uuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc, boot.get().mesh().getOptions().getHttpServerOptions());
		BinaryFieldTransformRequest transformation = JsonUtil.readValue(ac.getBodyAsString(), BinaryFieldTransformRequest.class);
		if (isEmpty(transformation.getLanguage())) {
			throw error(BAD_REQUEST, "image_error_language_not_set");
		}
		String languageTag = transformation.getLanguage();

		// Load needed elements
		HibNode node = db.tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			HibProject project = tx.getProject(ac);
			HibNode n = nodeDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);

			HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
			}
			return n;
		});

		db.tx(tx -> {
			HibNodeFieldContainer fieldContainer = loadTargetedContent(tx, node, languageTag, fieldName);
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
				handleTransformImage(rc, uuid, fieldName);
			} else if ((fieldSchema instanceof S3BinaryFieldSchema)) {
				S3HibBinaryField field = fieldContainer.getS3Binary(fieldName);
				if (field == null) {
					throw error(NOT_FOUND, "error_s3binaryfield_not_found_with_name", fieldName);
				}
				handleS3TransformImage(rc, uuid, fieldName);
			}
		});
	}

	/**
	 * Handle S3 image transformation. This operation will utilize the S3 binary data of the existing field and apply the transformation options. The new binary data
	 * will be stored and the field will be updated accordingly.
	 *
	 * @param rc
	 *            routing context
	 * @param uuid
	 * @param fieldName
	 */
	public void handleS3TransformImage(RoutingContext rc, String uuid, String fieldName) {
		validateParameter(uuid, "uuid");
		validateParameter(fieldName, "fieldName");

		FileSystem fs = rxVertx.fileSystem();
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		BinaryFieldTransformRequest transformation = JsonUtil.readValue(ac.getBodyAsString(), BinaryFieldTransformRequest.class);
		if (isEmpty(transformation.getLanguage())) {
			throw error(BAD_REQUEST, "image_error_language_not_set");
		}

		String languageTag = transformation.getLanguage();

		// Prepare the imageManipulationParameter using the transformation request as source
		ImageManipulationParameters parameters = new ImageManipulationParametersImpl();
		parameters.setWidth(transformation.getWidth());
		parameters.setHeight(transformation.getHeight());
		parameters.setRect(transformation.getCropRect());
		parameters.setCropMode(transformation.getCropMode());
		parameters.setResizeMode(transformation.getResizeMode());
		parameters.setFocalPoint(transformation.getFocalPoint());
		if (parameters.getRect() != null) {
			parameters.setCropMode(CropMode.RECT);
		}
		if (parameters.getResizeMode() == null) {
			parameters.setResizeMode(ResizeMode.SMART);
		}
		// Lookup the s3 binary and set the focal point parameters
		S3HibBinaryField s3binaryField = db.tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			HibProject project = tx.getProject(ac);
			HibNode node = nodeDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);

			HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
			}

			HibNodeFieldContainer container = loadTargetedContent(tx, node, languageTag, fieldName);
			S3HibBinaryField field = loadS3BinaryField(container, fieldName);
			// Use the focal point which is stored along with the s3 binary field if no custom point was included in the query parameters.
			// Otherwise the query parameter focal point will be used and thus override the stored focal point.
			FocalPoint focalPoint = field.getImageFocalPoint();
			if (!parameters.hasFocalPoint() && focalPoint != null) {
				parameters.setFocalPoint(focalPoint);
			}
			return field;
		});

		parameters.validate();
		S3UploadContext s3UploadContext = new S3UploadContext();
		String s3ObjectKey = s3binaryField.getBinary().getS3ObjectKey();
		String fileName = s3binaryField.getBinary().getFileName();
		s3UploadContext.setFileName(fileName);
		s3UploadContext.setS3BinaryUuid(UUIDUtil.randomUUID());
		s3UploadContext.setS3ObjectKey(s3ObjectKey);
		imageManipulator
				.handleS3Resize(options.getS3Options().getBucket(), s3ObjectKey, fileName, parameters)
				.flatMap(file -> {
					// The image was stored and hashed. Now we need to load the stored file again and check the image properties
					Single<ImageInfo> info = imageManipulator.readImageInfo(file.getName());
					Single<FileProps> fileProps = fs.rxProps(file.getName());
					return Single.zip(info, fileProps, (infoV, props) -> {
						// Return a POJO which hold all information that is needed to update the field
						return new TransformationResult(null, props.size(), infoV, file.getName());
					});
				})
				.flatMap(transformationResult ->  Single.just(updateNodeInGraph(ac, s3UploadContext, transformationResult, uuid, languageTag, fieldName, parameters)))
				.subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle image transformation. This operation will utilize the binary data of the existing field and apply the transformation options. The new binary data
	 * will be stored and the field will be updated accordingly.
	 *
	 * @param rc
	 *            routing context
	 * @param uuid
	 * @param fieldName
	 */
	public void handleTransformImage(RoutingContext rc, String uuid, String fieldName) {
		validateParameter(uuid, "uuid");
		validateParameter(fieldName, "fieldName");
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		BinaryFieldTransformRequest transformation = JsonUtil.readValue(ac.getBodyAsString(), BinaryFieldTransformRequest.class);
		if (isEmpty(transformation.getLanguage())) {
			throw error(BAD_REQUEST, "image_error_language_not_set");
		}

		FileSystem fs = rxVertx.fileSystem();
		String temporaryId = UUIDUtil.randomUUID();
		String languageTag = transformation.getLanguage();

		// Prepare the imageManipulationParameter using the transformation request as source
		ImageManipulationParameters parameters = new ImageManipulationParametersImpl();
		parameters.setWidth(transformation.getWidth());
		parameters.setHeight(transformation.getHeight());
		parameters.setRect(transformation.getCropRect());
		parameters.setCropMode(transformation.getCropMode());
		parameters.setResizeMode(transformation.getResizeMode());
		parameters.setFocalPoint(transformation.getFocalPoint());
		if (parameters.getRect() != null) {
			parameters.setCropMode(CropMode.RECT);
		}
		if (parameters.getResizeMode() == null) {
			parameters.setResizeMode(ResizeMode.SMART);
		}
		UploadContext context = new UploadContext();
		// Lookup the binary and set the focal point parameters

		parameters.validate();

		// Read and resize the original image and store the result in the filesystem
		Single<TransformationResult> obsTransformation = db.tx(tx -> {
				NodeDao nodeDao = tx.nodeDao();
				HibProject project = tx.getProject(ac);
				HibNode node = nodeDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);

				HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
				if (language == null) {
					throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
				}

				HibNodeFieldContainer container = loadTargetedContent(tx, node, languageTag, fieldName);
				HibBinaryField field = loadBinaryField(container, fieldName);
				// Use the focal point which is stored along with the binary field if no custom point was included in the query parameters.
				// Otherwise the query parameter focal point will be used and thus override the stored focal point.
				FocalPoint focalPoint = field.getImageFocalPoint();
				if (!parameters.hasFocalPoint() && focalPoint != null) {
					parameters.setFocalPoint(focalPoint);
				}
				return imageManipulator.handleResize(field.getBinary(), parameters);
			}).flatMap(file -> {
				// Hash the resized image data and store it using the computed fieldUuid + hash
				Flowable<Buffer> stream = fs.rxOpen(file, new OpenOptions()).flatMapPublisher(RxUtil::toBufferFlow);
				Single<String> hash = FileUtils.hash(stream);

				// The image was stored and hashed. Now we need to load the stored file again and check the image properties
				Single<ImageInfo> info = imageManipulator.readImageInfo(file);

				Single<FileProps> fileProps = fs.rxProps(file);

				return Single.zip(hash, info, fileProps, (hashV, infoV, props) -> {
					// Return a POJO which hold all information that is needed to update the field
					TransformationResult result = new TransformationResult(hashV, props.size(), infoV, file);
					return Single.just(result);
				}).flatMap(e -> e);
			});

		obsTransformation.flatMap(r -> {
			db.tx(tx -> {
				String hash = r.getHash();
				HibBinary binary = binaries.findByHash(hash).runInExistingTx(tx);

				// Set the info that the store operation is needed
				if (binary == null) {
					context.setBinaryUuid(UUIDUtil.randomUUID());
					context.setInvokeStore();
				}
			});
			// Store the binary in a temporary location
			Flowable<Buffer> data = fs.rxOpen(r.getFilePath(), new OpenOptions()).toFlowable()
				.flatMap(RxUtil::toBufferFlow);
			return binaryStorage.storeInTemp(data, temporaryId).andThen(Single.just(r));
		}).map(r -> {
			// Update graph with the new image information
			return updateNodeInGraph(ac, context, r, uuid, languageTag, fieldName, parameters);
		}).onErrorResumeNext(e -> {
			if (context.isInvokeStore()) {
				if (log.isDebugEnabled()) {
					log.debug("Error detected. Purging previously stored upload for tempId {}", temporaryId, e);
				}
				return binaryStorage.purgeTemporaryUpload(temporaryId).doOnError(e1 -> {
					log.error("Error while purging temporary upload for tempId {}", temporaryId, e1);
				}).onErrorComplete().andThen(Single.error(e));
			} else {
				return Single.error(e);
			}
		}).flatMap(n -> {
			if (context.isInvokeStore()) {
				String binaryUuid = context.getBinaryUuid();
				if (log.isDebugEnabled()) {
					log.debug("Moving upload with uuid {} and tempId {} into place", binaryUuid, temporaryId);
				}
				return binaryStorage.moveInPlace(binaryUuid, temporaryId).andThen(Single.just(n));
			} else {
				return Single.just(n);
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	private NodeResponse updateNodeInGraph(InternalActionContext ac, S3UploadContext s3UploadContext, TransformationResult result, String nodeUuid,
										   String languageTag, String fieldName, ImageManipulationParameters parameters) {
		return utils.eventAction((tx, batch) -> {
			PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();
			NodeDao nodeDao = tx.nodeDao();
			HibProject project = tx.getProject(ac);

			HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);
			HibNodeFieldContainer latestDraftVersion = loadTargetedContent(tx, node, languageTag, fieldName);
			HibBranch branch = tx.getBranch(ac);

			// Create a new node version field container to store the upload
			HibNodeFieldContainer newDraftVersion = contentDao.createFieldContainer(node, languageTag, branch, ac.getUser(),
					latestDraftVersion, true);

			// TODO Add conflict checking

			// Now that the binary data has been resized and inspected we can use this information to create a new binary and store it.
			S3HibBinary s3HibBinary = s3binaries.create(s3UploadContext.getS3BinaryUuid(), s3UploadContext.getS3ObjectKey(), s3UploadContext.getFileName()).runInExistingTx(tx);

			// Now create the binary field in which we store the information about the file
			S3HibBinaryField oldField = (S3HibBinaryField) contentDao.detachField(newDraftVersion.getS3Binary(fieldName));
			S3HibBinaryField field = newDraftVersion.createS3Binary(fieldName, s3HibBinary);
			if (oldField != null) {
				oldField.copyTo(field);
				newDraftVersion.removeField(oldField);
			}
			S3HibBinary currentS3Binary = field.getBinary();

			// TODO should we rename the image, if the extension is wrong?
			if (nonNull(result)) {
				currentS3Binary.setSize(result.getSize());
				if (nonNull(result.getMimeType())) {
					field.setMimeType(result.getMimeType());
				}
				if (nonNull(result.getImageInfo())) {
					currentS3Binary.setImageHeight(result.getImageInfo().getHeight());
					currentS3Binary.setImageWidth(result.getImageInfo().getWidth());
				}
			}

			if (parameters.hasFocalPoint()) {
				field.setImageFocalPoint(parameters.getFocalPoint());
			}
			// If the s3 binary field is the segment field, we need to update the webroot info in the node
			if (field.getFieldKey().equals(contentDao.getSchemaContainerVersion(newDraftVersion).getSchema().getSegmentField())) {
				contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
			}
			BranchDao branchDao = tx.branchDao();
			// TODO maybe use a fixed method in project?
			String branchUuid = branchDao.getLatestBranch(node.getProject()).getUuid();

			// Purge the old draft
			if (ac.isPurgeAllowed() && contentDao.isAutoPurgeEnabled(newDraftVersion) && contentDao.isPurgeable(latestDraftVersion)) {
				contentDao.purge(latestDraftVersion);
			}

			batch.add(contentDao.onCreated(newDraftVersion, branchUuid, DRAFT));
			return tx.nodeDao().transformToRestSync(node, ac, 0);
		});
	}

	private NodeResponse updateNodeInGraph(InternalActionContext ac, UploadContext context, TransformationResult result, String nodeUuid,
		String languageTag, String fieldName, ImageManipulationParameters parameters) {
		return utils.eventAction((tx, batch) -> {
			NodeDao nodeDao = tx.nodeDao();
			HibProject project = tx.getProject(ac);
			HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, UPDATE_PERM);
			PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();

			HibNodeFieldContainer latestDraftVersion = loadTargetedContent(tx, node, languageTag, fieldName);

			HibBranch branch = tx.getBranch(ac);

			// Create a new node version field container to store the upload
			HibNodeFieldContainer newDraftVersion = contentDao.createFieldContainer(node, languageTag, branch, ac.getUser(),
				latestDraftVersion, true);

			// TODO Add conflict checking

			// Now that the binary data has been resized and inspected we can use this information to create a new binary and store it.
			String hash = result.getHash();
			HibBinary binary = binaries.findByHash(hash).runInExistingTx(tx);
			// Check whether the binary was already stored.
			if (binary == null) {
				// Open the file again since we already read from it. We need to read it again in order to store it in the binary storage.
				BinaryFieldSchema fieldSchema = (BinaryFieldSchema) latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
				BinaryCheckStatus checkStatus = StringUtils.isBlank(fieldSchema.getCheckServiceUrl())
					? BinaryCheckStatus.ACCEPTED
					: BinaryCheckStatus.POSTPONED;

				binary = binaries.create(context.getBinaryUuid(), hash, result.getSize(), checkStatus).runInExistingTx(tx);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Data of resized image with hash {" + hash + "} has already been stored. Skipping store.");
				}
			}

			// Now create the binary field in which we store the information about the file
			HibBinaryField oldField = (HibBinaryField) contentDao.detachField( newDraftVersion.getBinary(fieldName));
			HibBinaryField field = newDraftVersion.createBinary(fieldName, binary);
			if (oldField != null) {
				oldField.copyTo(field);
				newDraftVersion.removeField(oldField);
			}
			HibBinary currentBinary = field.getBinary();
			currentBinary.setSize(result.getSize());
			field.setMimeType(result.getMimeType());
			// TODO should we rename the image, if the extension is wrong?
			currentBinary.setImageHeight(result.getImageInfo().getHeight());
			currentBinary.setImageWidth(result.getImageInfo().getWidth());

			if (parameters.hasFocalPoint()) {
				field.setImageFocalPoint(parameters.getFocalPoint());
			}
			// If the binary field is the segment field, we need to update the webroot info in the node
			if (field.getFieldKey().equals(contentDao.getSchemaContainerVersion(newDraftVersion).getSchema().getSegmentField())) {
				contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
			}
			BranchDao branchDao = tx.branchDao();
			// TODO maybe use a fixed method in project?
			String branchUuid = branchDao.getLatestBranch(node.getProject()).getUuid();

			// Purge the old draft
			if (ac.isPurgeAllowed() && contentDao.isAutoPurgeEnabled(newDraftVersion) && contentDao.isPurgeable(latestDraftVersion)) {
				contentDao.purge(latestDraftVersion);
			}

			batch.add(contentDao.onCreated(newDraftVersion, branchUuid, DRAFT));
			return tx.nodeDao().transformToRestSync(node, ac, 0);
		});
	}

	private HibNodeFieldContainer loadTargetedContent(Tx tx, HibNode node, String languageTag, String fieldName) {
		ContentDao contentDao = tx.contentDao();
		HibNodeFieldContainer latestDraftVersion = contentDao.getLatestDraftFieldContainer(node, languageTag);
		if (latestDraftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
		if (fieldSchema == null) {
			throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
		}
		if (!(fieldSchema instanceof BinaryFieldSchema)&&!(fieldSchema instanceof S3BinaryFieldSchema)) {
			throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
		}
		return latestDraftVersion;
	}

	private HibBinaryField loadBinaryField(HibNodeFieldContainer container, String fieldName) {
		HibBinaryField initialField = container.getBinary(fieldName);
		if (initialField == null) {
			throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
		}

		if (!initialField.hasProcessableImage()) {
			throw error(BAD_REQUEST, "error_transformation_non_image", fieldName);
		}
		return initialField;
	}


	private S3HibBinaryField loadS3BinaryField(HibNodeFieldContainer container, String fieldName) {
		S3HibBinaryField initialField = container.getS3Binary(fieldName);
		if (initialField == null) {
			throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
		}

		if (!initialField.hasProcessableImage()) {
			throw error(BAD_REQUEST, "error_transformation_non_image", fieldName);
		}
		return initialField;
	}
}
