package com.gentics.mesh.core.data.dao.impl;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.S3BinaryDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.result.Result;
import dagger.Lazy;
import org.apache.commons.lang3.NotImplementedException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

/**
 * @See {@link S3BinaryDaoWrapper}
 */
@Singleton
public class S3BinaryDaoWrapperImpl extends AbstractDaoWrapper<S3HibBinary> implements S3BinaryDaoWrapper {

	private final S3Binaries s3binaries;

	@Inject
	public S3BinaryDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, S3Binaries s3binaries) {
		super(boot, permissions);
		this.s3binaries = s3binaries;
	}

	@Override
	public Transactional<? extends S3HibBinary> findByS3ObjectKey(String s3ObjectKey) {
		return s3binaries.findByS3ObjectKey(s3ObjectKey);
	}

	@Override
	public Transactional<? extends S3HibBinary> create(String uuid, String objectKey, String fileName) {
		return s3binaries.create(uuid, objectKey,fileName);
	}

	@Override
	public Transactional<Stream<S3HibBinary>> findAll() {
		return s3binaries.findAll();
	}

	@Override
	public Result<S3BinaryGraphField> findFields(S3HibBinary s3binary) {
		return toGraph(s3binary).findFields();
	}
}