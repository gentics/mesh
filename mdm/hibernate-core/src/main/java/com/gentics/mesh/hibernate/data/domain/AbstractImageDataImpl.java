package com.gentics.mesh.hibernate.data.domain;

import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.ImageDataElement;

/**
 * Common part of a binary entity.
 *
 * @author plyhun
 *
 */
@MappedSuperclass
public abstract class AbstractImageDataImpl extends AbstractHibBaseElement implements ImageDataElement {

	protected long fileSize;
	protected Integer imageHeight;
	protected Integer imageWidth;

	@Override
	public long getSize() {
		return fileSize;
	}

	@Override
	public AbstractImageDataImpl setSize(long sizeInBytes) {
		this.fileSize = sizeInBytes;
		return this;
	}

	@Override
	public Integer getImageHeight() {
		return imageHeight;
	}

	@Override
	public AbstractImageDataImpl setImageHeight(Integer height) {
		imageHeight = height;
		return this;
	}

	@Override
	public Integer getImageWidth() {
		return imageWidth;
	}

	@Override
	public AbstractImageDataImpl setImageWidth(Integer width) {
		imageWidth = width;
		return this;
	}
}
