package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Setters for the {@link ImageVariant} instance
 * 
 * @author plyhun
 *
 */
public interface HibImageVariantSetter extends ImageVariant {

	ImageVariant setAuto(boolean auto);

	ImageVariant setResizeMode(ResizeMode resize);

	ImageVariant setCropMode(CropMode crop);

	ImageVariant setCropStartY(Integer cropY);

	ImageVariant setCropStartX(Integer cropX);

	ImageVariant setCropWidth(Integer cropWidth);

	ImageVariant setCropHeight(Integer cropHeight);

	ImageVariant setFocalPointZoom(Float fpz);

	ImageVariant setFocalPointY(Float fpy);

	ImageVariant setFocalPointX(Float fpx);

	ImageVariant setHeight(Integer height);

	ImageVariant setWidth(Integer width);

	@Override
	default ImageVariant fillFromManipulation(Binary binary, ImageManipulation variant) {
		if (variant.getRect() != null) {
			setCropStartX(variant.getRect().getStartX());
			setCropStartY(variant.getRect().getStartY());
			setCropWidth(variant.getRect().getWidth());
			setCropHeight(variant.getRect().getHeight());
		} else {
			setCropStartX(null);
			setCropStartY(null);			
			setCropWidth(null);
			setCropHeight(null);
		}
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
}
