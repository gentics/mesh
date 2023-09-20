package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Setters for the {@link HibImageVariant} instance
 * 
 * @author plyhun
 *
 */
public interface HibImageVariantSetter extends HibImageVariant {

	HibImageVariant setAuto(boolean auto);

	HibImageVariant setResizeMode(ResizeMode resize);

	HibImageVariant setCropMode(CropMode crop);

	HibImageVariant setCropStartY(Integer cropY);

	HibImageVariant setCropStartX(Integer cropX);

	HibImageVariant setCropWidth(Integer cropWidth);

	HibImageVariant setCropHeight(Integer cropHeight);

	HibImageVariant setFocalPointZoom(Float fpz);

	HibImageVariant setFocalPointY(Float fpy);

	HibImageVariant setFocalPointX(Float fpx);

	HibImageVariant setHeight(Integer height);

	HibImageVariant setWidth(Integer width);

	@Override
	default HibImageVariant fillFromManipulation(HibBinary binary, ImageManipulation variant) {
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
