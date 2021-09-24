package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.image.ImageInfo;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryFieldTransformRequest;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.storage.BinaryStorage;
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
 * Handler for binary transformer requests.
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

	@Inject
	public BinaryTransformHandler(Database db, HandlerUtilities utils, Vertx rxVertx, ImageManipulator imageManipulator,
		Lazy<BootstrapInitializer> boot,
		BinaryStorage binaryStorage, Binaries binaries) {
		this.db = db;
		this.utils = utils;
		this.rxVertx = rxVertx;
		this.imageManipulator = imageManipulator;
		this.boot = boot;
		this.binaryStorage = binaryStorage;
		this.binaries = binaries;
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

		// Load needed elements
		HibNode node = db.tx(tx -> {
			NodeDaoWrapper nodeDao = tx.nodeDao();
			HibProject project = tx.getProject(ac);
			HibNode n = nodeDao.loadObjectByUuid(project, ac, uuid, UPDATE_PERM);

			HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
			if (language == null) {
				throw error(NOT_FOUND, "error_language_not_found", transformation.getLanguage());
			}
			return n;
		});

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
		HibBinary binaryField = db.tx(() -> {
			NodeGraphFieldContainer container = loadTargetedContent(node, languageTag, fieldName);
			BinaryGraphField field = loadBinaryField(container, fieldName);
			// Use the focal point which is stored along with the binary field if no custom point was included in the query parameters.
			// Otherwise the query parameter focal point will be used and thus override the stored focal point.
			FocalPoint focalPoint = field.getImageFocalPoint();
			if (!parameters.hasFocalPoint() && focalPoint != null) {
				parameters.setFocalPoint(focalPoint);
			}
			return field.getBinary();
		});

		parameters.validate();

		// Read and resize the original image and store the result in the filesystem
		Single<TransformationResult> obsTransformation = db.tx(() -> imageManipulator.handleResize(binaryField, parameters))
			.flatMap(file -> {
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
			return updateNodeInGraph(ac, context, r, node, languageTag, fieldName, parameters);
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

	private NodeResponse updateNodeInGraph(InternalActionContext ac, UploadContext context, TransformationResult result, HibNode node,
		String languageTag, String fieldName, ImageManipulationParameters parameters) {
		return utils.eventAction((tx, batch) -> {
			ContentDaoWrapper contentDao = tx.contentDao();

			NodeGraphFieldContainer latestDraftVersion = loadTargetedContent(node, languageTag, fieldName);

			HibBranch branch = tx.getBranch(ac);

			// Create a new node version field container to store the upload
			NodeGraphFieldContainer newDraftVersion = contentDao.createGraphFieldContainer(node, languageTag, branch, ac.getUser(),
				latestDraftVersion, true);

			// TODO Add conflict checking

			// Now that the binary data has been resized and inspected we can use this information to create a new binary and store it.
			String hash = result.getHash();
			HibBinary binary = binaries.findByHash(hash).runInExistingTx(tx);
			// Check whether the binary was already stored.
			if (binary == null) {
				// Open the file again since we already read from it. We need to read it again in order to store it in the binary storage.
				binary = binaries.create(context.getBinaryUuid(), hash, result.getSize()).runInExistingTx(tx);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Data of resized image with hash {" + hash + "} has already been stored. Skipping store.");
				}
			}

			// Now create the binary field in which we store the information about the file
			BinaryGraphField oldField = newDraftVersion.getBinary(fieldName);
			BinaryGraphField field = newDraftVersion.createBinary(fieldName, binary);
			if (oldField != null) {
				oldField.copyTo(field);
				oldField.remove();
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
			if (field.getFieldKey().equals(newDraftVersion.getSchemaContainerVersion().getSchema().getSegmentField())) {
				contentDao.updateWebrootPathInfo(newDraftVersion, branch.getUuid(), "node_conflicting_segmentfield_upload");
			}
			BranchDaoWrapper branchDao = tx.branchDao();
			// TODO maybe use a fixed method in project?
			String branchUuid = branchDao.getLatestBranch(node.getProject()).getUuid();

			// Purge the old draft
			if (ac.isPurgeAllowed() && newDraftVersion.isAutoPurgeEnabled() && latestDraftVersion.isPurgeable()) {
				contentDao.purge(latestDraftVersion);
			}

			batch.add(newDraftVersion.onCreated(branchUuid, DRAFT));
			return tx.nodeDao().transformToRestSync(node, ac, 0);
		});
	}

	private NodeGraphFieldContainer loadTargetedContent(HibNode node, String languageTag, String fieldName) {
		ContentDaoWrapper contentDao = (ContentDaoWrapper) boot.get().contentDao();
		NodeGraphFieldContainer latestDraftVersion = contentDao.getLatestDraftFieldContainer(node, languageTag);
		if (latestDraftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion().getSchema().getField(fieldName);
		if (fieldSchema == null) {
			throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
		}
		if (!(fieldSchema instanceof BinaryFieldSchema)) {
			throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
		}
		return latestDraftVersion;
	}

	private BinaryGraphField loadBinaryField(NodeGraphFieldContainer container, String fieldName) {
		BinaryGraphField initialField = container.getBinary(fieldName);
		if (initialField == null) {
			throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
		}

		if (!initialField.hasProcessableImage()) {
			throw error(BAD_REQUEST, "error_transformation_non_image", fieldName);
		}
		return initialField;
	}

}
