package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.spi.GraphDatabase;

import dagger.Lazy;

/**
 * @See {@link BinaryDaoWrapper}
 */
@Singleton
public class BinaryDaoWrapperImpl extends AbstractDaoWrapper<HibBinary> implements BinaryDaoWrapper {

	private final Binaries binaries;

	@Inject
	public BinaryDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Binaries binaries, GraphDatabase database, ImageManipulator imageManipulator, BinaryStorage binaryStorage) {
		super(boot);
		this.binaries = binaries;
	}

	@Override
	public Result<? extends HibBinaryField> findFields(HibBinary binary) {
		return toGraph(binary).findFields();
	}

	@Override
	public Binaries binaries() {
		return binaries;
	}
}
