package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.io.InputStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBBinaryDao;
import com.gentics.mesh.core.data.db.spi.Supplier;
import com.gentics.mesh.core.data.db.spi.Transactional;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.result.Result;

import dagger.Lazy;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

@Singleton
public class BinaryDaoWrapperImpl extends AbstractDaoWrapper<HibBinary> implements OrientDBBinaryDao {

	private final Binaries binaries;

	@Inject
	public BinaryDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, Binaries binaries) {
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

	@Override
	public HibBinary findByUuidGlobal(String uuid) {
		throw new NotImplementedException("Not supported");
	}

	@Override
	public long globalCount() {
		throw new NotImplementedException("Not supported");
	}
}
