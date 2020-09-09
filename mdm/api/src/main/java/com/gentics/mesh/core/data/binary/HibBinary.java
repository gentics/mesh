package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.node.field.image.Point;

public interface HibBinary extends HibBaseElement {

	HibBinary setImageHeight(Integer height);

	HibBinary setImageWidth(Integer width);

	HibBinary setSize(long sizeInBytes);

	String getSHA512Sum();

	long getSize();

	Integer getImageHeight();

	Integer getImageWidth();

	Point getImageSize();

	HibBinary setSHA512Sum(String sha512sum);

	void setUuid(String uuid);

}
