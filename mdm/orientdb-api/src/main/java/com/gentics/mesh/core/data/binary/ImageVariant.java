package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Image manipulation variant graph edge.
 * 
 * @see HibImageVariant
 * 
 * @author plyhun
 *
 */
public interface ImageVariant extends MeshVertex, HibImageVariantSetter {

	String VARIANT_FILESIZE_PROPERTY_KEY = "binaryFileSize";
	String WIDTH_KEY = "width";
	String HEIGHT_KEY = "height";
	String FOCAL_POINT_X_KEY = "fpx";
	String FOCAL_POINT_Y_KEY = "fpy";
	String FOCAL_POINT_ZOOM_KEY = "fpz";
	String CROP_X_KEY = "cropX";
	String CROP_Y_KEY = "cropY";
	String CROP_WIDTH_KEY = "cropWidth";
	String CROP_HEIGHT_KEY = "cropHeight";
	String CROP_MODE_KEY = "cropMode";
	String RESIZE_MODE_KEY = "resizeMode";
	String AUTO_KEY = "auto";

	@Override
	default long getSize() {
		Long size = property(VARIANT_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	@Override
	default ImageVariant setSize(long sizeInBytes) {
		property(VARIANT_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	@Override
	default boolean isAuto() {
		Boolean auto = property(AUTO_KEY);
		return auto != null ? auto : false;
	}

	@Override
	default ImageVariant setAuto(boolean auto) {
		if (auto) {
			property(AUTO_KEY, auto);
		} else {
			removeProperty(AUTO_KEY);
		}
		return this;
	}

	@Override
	default Integer getWidth() {
		return property(WIDTH_KEY);
	}

	@Override
	default ImageVariant setWidth(Integer width) {
		if (width == null) {
			removeProperty(WIDTH_KEY);
		} else {
			property(WIDTH_KEY, width);
		}
		return this;
	}

	@Override
	default Integer getHeight() {
		return property(HEIGHT_KEY);
	}

	@Override
	default ImageVariant setHeight(Integer height) {
		if (height == null) {
			removeProperty(HEIGHT_KEY);
		} else {
			property(HEIGHT_KEY, height);
		}
		return this;
	}

	@Override
	default Float getFocalPointX() {
		return property(FOCAL_POINT_X_KEY);
	}

	@Override 
	default ImageVariant setFocalPointX(Float fpx) {
		if (fpx == null) {
			removeProperty(FOCAL_POINT_X_KEY);
		} else {
			property(FOCAL_POINT_X_KEY, fpx);
		}
		return this;
	}

	@Override
	default Float getFocalPointY() {
		return property(FOCAL_POINT_Y_KEY);
	}

	@Override 
	default ImageVariant setFocalPointY(Float fpy) {
		if (fpy == null) {
			removeProperty(FOCAL_POINT_Y_KEY);
		} else {
			property(FOCAL_POINT_Y_KEY, fpy);
		}
		return this;
	}

	@Override
	default Float getFocalPointZoom() {
		return property(FOCAL_POINT_ZOOM_KEY);
	}

	@Override 
	default ImageVariant setFocalPointZoom(Float fpz) {
		if (fpz == null) {
			removeProperty(FOCAL_POINT_ZOOM_KEY);
		} else {
			property(FOCAL_POINT_ZOOM_KEY, fpz);
		}
		return this;
	}

	@Override
	default Integer getCropWidth() {
		return property(CROP_WIDTH_KEY);
	}

	@Override
	default HibImageVariant setCropWidth(Integer cropWidth) {
		if (cropWidth == null) {
			removeProperty(CROP_WIDTH_KEY);
		} else {
			property(CROP_WIDTH_KEY, cropWidth);
		}
		return this;
	}

	@Override
	default Integer getCropHeight() {
		return property(CROP_HEIGHT_KEY);
	}

	@Override
	default HibImageVariant setCropHeight(Integer cropHeight) {
		if (cropHeight == null) {
			removeProperty(CROP_HEIGHT_KEY);
		} else {
			property(CROP_HEIGHT_KEY, cropHeight);
		}
		return this;
	}

	@Override
	default Integer getCropStartX() {
		return property(CROP_X_KEY);
	}

	@Override 
	default ImageVariant setCropStartX(Integer cropX) {
		if (cropX == null) {
			removeProperty(CROP_X_KEY);
		} else {
			property(CROP_X_KEY, cropX);
		}
		return this;
	}

	@Override
	default Integer getCropStartY() {
		return property(CROP_Y_KEY);
	}

	@Override 
	default ImageVariant setCropStartY(Integer cropY) {
		if (cropY == null) {
			removeProperty(CROP_Y_KEY);
		} else {
			property(CROP_Y_KEY, cropY);
		}
		return this;
	}

	@Override
	default CropMode getCropMode() {
		return CropMode.get(property(CROP_MODE_KEY));
	}

	@Override 
	default ImageVariant setCropMode(CropMode crop) {
		if (crop == null) {
			removeProperty(CROP_MODE_KEY);
		} else {
			property(CROP_MODE_KEY, crop.getKey());
		}
		return this;
	}

	@Override
	default ResizeMode getResizeMode() {
		return ResizeMode.get(property(RESIZE_MODE_KEY));
	}

	@Override 
	default ImageVariant setResizeMode(ResizeMode resize) {
		if (resize == null) {
			removeProperty(RESIZE_MODE_KEY);
		} else {
			property(RESIZE_MODE_KEY, resize.getKey());
		}
		return this;
	}

	@Override
	default HibImageDataElement setImageHeight(Integer height) {
		return setHeight(height);
	}

	@Override
	default HibImageDataElement setImageWidth(Integer width) {
		return setWidth(width);
	}

	@Override
	Binary getBinary();

	@Override
	Result<? extends BinaryGraphField> findFields();
}
