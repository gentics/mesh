package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Utility for debug info operations.
 */
@Singleton
public class DebugInfoUtil {

	private final Vertx vertx;
	private static final Logger log = LoggerFactory.getLogger(DebugInfoUtil.class);

	/**
	 * Runtime architecture pointer size
	 */
	public static final int POINTER_SIZE;

	static {
		POINTER_SIZE = Optional.ofNullable(System.getProperty("sun.arch.data.model"))
				.filter(StringUtils::isNotBlank)
				.filter(StringUtils::isNumeric)
				.map(Integer::parseInt)
				.orElse(64) / 8;
	}

	@Inject
	public DebugInfoUtil(Vertx vertx) {
		this.vertx = vertx;
	}

	/**
	 * Read the file for the path and return the buffer flow.
	 * 
	 * @param path
	 * @return
	 */
	public Flowable<Buffer> readFileOrEmpty(String path) {
		return vertx.fileSystem().rxReadFile(path)
			.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate)
			.toFlowable()
			.onErrorResumeNext(err -> {
				log.debug(String.format("Could not read file {%s}", path), err);
				log.info(String.format("Could not read file {%s}", path));
				return Flowable.empty();
			});
	}

	/**
	 * Read the file for the path and return the debug info entry for it.
	 * 
	 * @param path
	 * @return
	 */
	public Flowable<DebugInfoEntry> readDebugInfoEntryOrEmpty(String path) {
		return readFileOrEmpty(path)
			.map(buf -> DebugInfoBufferEntry.fromBuffer(path, buf));
	}

	/**
	 * Transform the byte count into a human readable format.
	 * 
	 * @param bytes
	 *            Amount of bytes to be transformed
	 * @return Human readable format
	 */
	public static String humanReadableByteCount(long bytes) {
		return humanReadableByteCount(bytes, true);
	}

	/**
	 * Transform the byte count into a human readable format.
	 * 
	 * @param bytes
	 *            Amount of bytes to be transformed
	 * @param si
	 *            true: base 1000, otherwise base 1024
	 * @return Human readable format
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
