package com.gentics.mesh.core.data.dao.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.S3BinaryDaoWrapper;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;

import dagger.Lazy;

/**
 * @See {@link S3BinaryDaoWrapper}
 */
@Singleton
public class S3BinaryDaoWrapperImpl extends AbstractDaoWrapper<S3HibBinary> implements S3BinaryDaoWrapper {

	private final S3Binaries s3binaries;

	@Inject
	public S3BinaryDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, S3Binaries s3binaries) {
		super(boot);
		this.s3binaries = s3binaries;
	}

	@Override
	public S3Binaries getS3HibBinaries() {
		return s3binaries;
	}
}