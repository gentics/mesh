package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Image manipulation variant graph edge.
 * 
 * @see HibImageVariant
 * 
 * @author plyhun
 *
 */
public interface ImageVariant extends MeshVertex, HibImageVariant {

	String VARIANT_FILESIZE_PROPERTY_KEY = "binaryFileSize";
	String WIDTH_KEY = "width";
	String HEIGHT_KEY = "height";
	String FOCAL_POINT_X_KEY = "fpx";
	String FOCAL_POINT_Y_KEY = "fpy";
	String FOCAL_POINT_ZOOM_KEY = "fpz";
	String CROP_X_KEY = "cropX";
	String CROP_Y_KEY = "cropY";
	String CROP_MODE_KEY = "cropMode";
	String RESIZE_MODE_KEY = "resizeMode";
	String AUTO_KEY = "auto";

	@Override
	default ImageVariant fillFromManipulation(HibBinary binary, ImageManipulation variant) {
		if (variant.getRect() != null) {
			setCropStartX(variant.getRect().getStartX());
			setCropStartY(variant.getRect().getStartY());
			setWidth(variant.getRect().getWidth());
			setHeight(variant.getRect().getHeight());
		} else {
			setCropStartX(null);
			setCropStartY(null);			
			if (variant.getWidth() != null) {
				if ("auto".equals(variant.getWidth())) {
					setAuto(true);
					setHeight(Integer.parseInt(variant.getHeight()));
					Point originalSize = binary.getImageSize();
					float ratio = ((float) originalSize.getX()) / ((float) originalSize.getY());
					setWidth((int) ((float) getHeight() * ratio));
				} else {
					setWidth(Integer.parseInt(variant.getWidth()));
				}
			} else {
				setWidth(null);
			}
			if (variant.getHeight() != null) {
				if ("auto".equals(variant.getHeight())) {
					setAuto(true);
					setWidth(Integer.parseInt(variant.getWidth()));
					Point originalSize = binary.getImageSize();
					float ratio = ((float) originalSize.getX()) / ((float) originalSize.getY());
					setHeight((int) ((float) getWidth() * ratio));
				} else {
					setHeight(Integer.parseInt(variant.getHeight()));
				}
			} else {
				setHeight(null);
			}
		}
		if (variant.getFocalPoint() != null) {
			setFocalPointX(variant.getFocalPoint().getX());
			setFocalPointY(variant.getFocalPoint().getY());
		} else {
			setFocalPointX(null);
			setFocalPointY(null);
		}
		setCropMode(variant.getCropMode());
		setFocalPointZoom(variant.getFocalPointZoom());
		setResizeMode(variant.getResizeMode());
		return this;
	}

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

	private ImageVariant setAuto(boolean auto) {
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

	private ImageVariant setWidth(Integer width) {
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

	private ImageVariant setHeight(Integer height) {
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

	private ImageVariant setFocalPointX(Float fpx) {
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

	private ImageVariant setFocalPointY(Float fpy) {
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

	private ImageVariant setFocalPointZoom(Float fpz) {
		if (fpz == null) {
			removeProperty(FOCAL_POINT_ZOOM_KEY);
		} else {
			property(FOCAL_POINT_ZOOM_KEY, fpz);
		}
		return this;
	}

	@Override
	default Integer getCropStartX() {
		return property(CROP_X_KEY);
	}

	private ImageVariant setCropStartX(Integer cropX) {
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

	private ImageVariant setCropStartY(Integer cropY) {
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

	private ImageVariant setCropMode(CropMode crop) {
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

	private ImageVariant setResizeMode(ResizeMode resize) {
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
