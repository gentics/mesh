package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.binary.impl.ImageVariantImpl;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.ImageVariantDao;
import com.gentics.mesh.core.data.dao.ImageVariantDaoWrapper;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;

import dagger.Lazy;

/**
 * @See {@link ImageVariantDao}
 */
@Singleton
public class ImageVariantDaoWrapperImpl extends AbstractDaoWrapper<HibImageVariant> implements ImageVariantDaoWrapper {

	private final ImageManipulator imageManipulator;
	private final BinaryStorage binaryStorage;

	@Inject
	public ImageVariantDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, ImageManipulator imageManipulator, BinaryStorage binaryStorage) {
		super(boot);
		this.imageManipulator = imageManipulator;
		this.binaryStorage = binaryStorage;
	}

	@Override
	public Result<? extends ImageVariant> getVariants(HibBinary binary, InternalActionContext ac) {
		return toGraph(binary).getVariants();
	}

	@Override
	public Result<? extends HibImageVariant> getVariants(HibBinaryField binaryField, InternalActionContext ac) {
		return toGraph(binaryField).getImageVariants();
	}

	@Override
	public boolean deletePersistedVariant(HibBinary binary, HibImageVariant variant, boolean throwOnInUse) {
		if (variant.findFields().hasNext()) {
			if (throwOnInUse) {
				throw error(BAD_REQUEST, "image_error_variant_in_use", variant.getKey());
			} else {
				log.info("The variant {} is in use and cannot be deleted", variant.getKey());
				return false;
			}
		}
		String variantUuid = variant.getUuid();
		ImageVariant imageVariant = toGraph(variant);
		toGraph(binary).unlinkOut(imageVariant, GraphRelationships.HAS_VARIANTS);
		imageVariant.remove();
		binaryStorage.delete(variantUuid).blockingGet();
		return true;
	}

	@Override
	public ImageVariant createPersistedVariant(HibBinary binary, ImageVariantRequest request, Consumer<HibImageVariant> inflater) {
		FramedGraph graph = toGraph(binary).getGraph();
		ImageVariantImpl variant = graph.addFramedVertex(ImageVariantImpl.class);
		toGraph(binary).linkOut(variant, GraphRelationships.HAS_VARIANTS);
		inflater.accept(variant);
		
		String variantUuid = variant.getUuid();
		
		long filesize = imageManipulator.handleResize(binary, request)
				.flatMap(cachePath -> {
					long size = new File(cachePath).length();
					return binaryStorage.moveInPlace(variantUuid, cachePath, false).toSingleDefault(size);
				})
				.blockingGet();

		variant.setSize(filesize);
		return variant;
	}

	@Override
	public void attachVariant(HibBinaryField binaryField, ImageVariantRequest request, InternalActionContext ac, boolean throwOnExisting) {
		BinaryGraphField graphField = toGraph(binaryField);
		ImageVariant variant = toGraph(getVariant(binaryField.getBinary(), request, ac));
		graphField.attachImageVariant(variant, throwOnExisting);
	}

	@Override
	public void detachVariant(HibBinaryField binaryField, ImageVariantRequest request, InternalActionContext ac, boolean throwOnAbsent) {
		BinaryGraphField graphField = toGraph(binaryField);
		ImageVariant variant = toGraph(getVariant(binaryField.getBinary(), request, ac));
		graphField.detachImageVariant(variant, throwOnAbsent);
	}

	@Override
	public ImageVariant getVariant(HibBinary binary, ImageManipulation request, InternalActionContext ac) {
		//return getVariants(binary, ac).stream().filter(variant -> doesVariantMatchRequest(variant, request)).findAny().orElse(null);

		VertexTraversal<?, ?, ?> edge = toGraph(binary).out(GraphRelationships.HAS_VARIANTS);
		if (request.getRect() != null) {
			edge = edge.has(ImageVariant.CROP_X_KEY, request.getRect().getStartX()).has(ImageVariant.CROP_Y_KEY, request.getRect().getStartY());
			if (request.getRect().getWidth() > 0) {
				edge = edge.has(ImageVariant.CROP_WIDTH_KEY, request.getRect().getWidth());
			}
			if (request.getRect().getHeight() > 0) {
				edge = edge.has(ImageVariant.CROP_HEIGHT_KEY, request.getRect().getHeight());
			}
		} else {
			edge = edge.hasNot(ImageVariant.CROP_X_KEY).hasNot(ImageVariant.CROP_Y_KEY).hasNot(ImageVariant.CROP_WIDTH_KEY).hasNot(ImageVariant.CROP_HEIGHT_KEY);
		}
		if (StringUtils.isBlank(request.getWidth())) {
			//edge = edge.hasNot(ImageVariant.AUTO_KEY);
			//edge = edge.hasNot(ImageVariant.WIDTH_KEY);
		} else {
			if ("auto".equals(request.getWidth())) {
				edge = edge.has(ImageVariant.AUTO_KEY, true);
			} else {
				edge = edge.has(ImageVariant.WIDTH_KEY, Integer.parseInt(request.getWidth()));
			}
		}
		if (StringUtils.isBlank(request.getHeight())) {
			//edge = edge.hasNot(ImageVariant.AUTO_KEY);
			//edge = edge.hasNot(ImageVariant.HEIGHT_KEY);
		} else {
			if ("auto".equals(request.getHeight())) {
				edge = edge.has(ImageVariant.AUTO_KEY, true);
			} else {
				edge = edge.has(ImageVariant.HEIGHT_KEY, Integer.parseInt(request.getHeight()));
			}
		}
		if (request.getCropMode() != null) {
			edge = edge.has(ImageVariant.CROP_MODE_KEY, request.getCropMode().getKey());
		} else {
			edge = edge.hasNot(ImageVariant.CROP_MODE_KEY);
		}
		if (request.hasFocalPoint()) {
			edge = edge.has(ImageVariant.FOCAL_POINT_X_KEY, request.getFocalPoint().getX()).has(ImageVariant.FOCAL_POINT_Y_KEY, request.getFocalPoint().getY());
		} else {
			edge = edge.hasNot(ImageVariant.FOCAL_POINT_X_KEY).hasNot(ImageVariant.FOCAL_POINT_Y_KEY);
		}
		if (request.getFocalPointZoom() != null) {
			edge = edge.has(ImageVariant.FOCAL_POINT_ZOOM_KEY, request.getFocalPointZoom());
		} else {
			edge = edge.hasNot(ImageVariant.FOCAL_POINT_ZOOM_KEY);
		}
		if (request.getResizeMode() != null) {
			edge = edge.has(ImageVariant.RESIZE_MODE_KEY, request.getResizeMode().getKey());
		} else {
			edge = edge.hasNot(ImageVariant.RESIZE_MODE_KEY);
		}
		return edge.nextOrDefaultExplicit(ImageVariantImpl.class, null);
	}

	@Override
	public HibImageVariant getVariant(HibBinaryField binaryField, ImageManipulation request, InternalActionContext ac) {
		ImageVariant variant = getVariant(binaryField.getBinary(), request, ac);
		return toGraph(binaryField).getParentContainer().findImageVariant(binaryField.getFieldKey(), variant).getVariant();
	}
}
