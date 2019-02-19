package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class FileUtils {

	private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

	protected static final char[] hexArray = "0123456789abcdef".toCharArray();

	private FileUtils() {
	}

	/**
	 * Generate a SHA 512 checksum from the given file and asynchronously return the hex encoded hash as a string.
	 * 
	 * @param path
	 */
	public static String hash(String path) {
		// TODO refactor this implementation to use buffers and process the file data async using the vertx fs methods.
		// This way the processing of the data will not permanently block the execution
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			try (InputStream is = Files.newInputStream(Paths.get(path)); DigestInputStream mis = new DigestInputStream(is, md)) {
				byte[] buffer = new byte[4096];
				while (mis.read(buffer) >= 0) {
				}
			}
			byte[] digest = md.digest();
			return bytesToHex(digest);
		} catch (Exception e) {
			log.error("Error while hashing file {" + path + "}", e);
			throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", e);
		}
	}

	public static Single<String> hash(Flowable<Buffer> stream) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			return stream.reduce(md, (digest, buffer) -> {
				digest.update(buffer.getBytes());
				return digest;
			}).map(digest -> digest.digest()).map(FileUtils::bytesToHex);
		} catch (Exception e) {
			log.error("Error while hashing data", e);
			return Single.error(error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", e));
		}
	}

	/**
	 * Generate a SHA 512 checksum from the data in the given buffer and asynchronously return the hex encoded hash as a string.
	 * 
	 * @param buffer
	 * @return Observable returning the SHA 512 checksum
	 */
	public static Single<String> hash(Buffer buffer) {
		return hash(Flowable.just(buffer));
	}

	/**
	 * Convert the byte array to a hex formatted string.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
