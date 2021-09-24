package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.io.InputStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibBinaryField;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.spi.Supplier;
import com.gentics.mesh.graphdb.spi.Transactional;

import dagger.Lazy;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * @See {@link BinaryDaoWrapper}
 */
@Singleton
public class BinaryDaoWrapperImpl extends AbstractDaoWrapper<HibBinary> implements BinaryDaoWrapper {

	private final Binaries binaries;

	@Inject
	public BinaryDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, Binaries binaries) {
		super(boot, permissions);
		this.binaries = binaries;
	}

	@Override
	public Transactional<? extends HibBinary> findByHash(String hash) {
		return binaries.findByHash(hash);
	}

	@Override
	public Transactional<? extends HibBinary> create(String uuid, String hash, Long size) {
		return binaries.create(uuid, hash, size);
	}

	@Override
	public Transactional<Stream<HibBinary>> findAll() {
		return binaries.findAll();
	}

	@Override
	public Flowable<Buffer> getStream(HibBinary binary) {
		return toGraph(binary).getStream();
	}

	@Override
	public Supplier<InputStream> openBlockingStream(HibBinary binary) {
		return toGraph(binary).openBlockingStream();
	}

	@Override
	public String getBase64ContentSync(HibBinary binary) {
		return toGraph(binary).getBase64ContentSync();
	}

	@Override
	public Result<HibBinaryField> findFields(HibBinary binary) {
		return toGraph(binary).findFields();
	}
}
