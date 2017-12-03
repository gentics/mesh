package com.gentics.mesh.storage;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import rx.Completable;

public class S3BinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(S3BinaryStorage.class);

	@Override
	public boolean exists(BinaryGraphField field) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ReadStream<Buffer> read(BinaryGraphField field) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Completable store(ReadStream<Buffer> stream, String hashsum) {
		// TODO Auto-generated method stub
		return Completable.error(new Exception("Not implemented"));

	}

}
