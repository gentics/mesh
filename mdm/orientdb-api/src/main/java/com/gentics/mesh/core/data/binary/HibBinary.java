package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.HibBaseElement;

public interface HibBinary extends HibBaseElement {

	HibBinary setImageHeight(Integer height);

	HibBinary setImageWidth(Integer width);

	HibBinary setSize(long sizeInBytes);

}
