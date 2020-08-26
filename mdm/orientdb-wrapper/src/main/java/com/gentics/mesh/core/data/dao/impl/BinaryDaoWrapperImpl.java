package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toBinary;

import java.io.InputStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.graphdb.spi.Supplier;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.madl.traversal.TraversalResult;

import dagger.Lazy;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

@Singleton
public class BinaryDaoWrapperImpl extends AbstractDaoWrapper<HibBinary> implements BinaryDaoWrapper {

	private final Binaries binaries;

	@Inject
	public BinaryDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, Binaries binaries) {
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
		return toBinary(binary).getStream();
	}

	@Override
	public Supplier<InputStream> openBlockingStream(HibBinary binary) {
		return toBinary(binary).openBlockingStream();
	}

	@Override
	public String getBase64ContentSync(HibBinary binary) {
		return toBinary(binary).getBase64ContentSync();
	}

	@Override
	public TraversalResult<BinaryGraphField> findFields(HibBinary binary) {
		return toBinary(binary).findFields();
	}

	@Override
	public HibBinary findByUuidGlobal(String uuid) {
		throw new NotImplementedException("Not supported");
	}

	@Override
	public long computeGlobalCount() {
		throw new NotImplementedException("Not supported");
	}
}
