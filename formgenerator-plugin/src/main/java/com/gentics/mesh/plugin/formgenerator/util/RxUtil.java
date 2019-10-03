package com.gentics.mesh.plugin.formgenerator.util;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

public final class RxUtil {

	/**
	 * Read the flowable and return a single buffer with the read data.
	 * 
	 * @param stream
	 * @return
	 */
	public static Single<Buffer> readEntireData(Flowable<byte[]> stream) {
		return stream.map(b -> Buffer.buffer(b)).reduce((a, b) -> a.appendBuffer(b)).toSingle();
	}
}
