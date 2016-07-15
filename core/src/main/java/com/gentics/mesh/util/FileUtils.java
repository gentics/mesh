package com.gentics.mesh.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import com.gentics.mesh.Mesh;

import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.buffer.Buffer;
import rx.Scheduler;
import rx.Single;

public final class FileUtils {

	protected static final char[] hexArray = "0123456789abcdef".toCharArray();

	private FileUtils() {
	}

	/**
	 * Generate a SHA 512 checksum from the given file and asynchronously return the hex encoded hash as a string.
	 * 
	 * @param path
	 */
	public static Single<String> generateSha512Sum(String path) {
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		Single<String> obs = Single.create(sub -> {
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-512");
				try (InputStream is = Files.newInputStream(Paths.get(path)); DigestInputStream mis = new DigestInputStream(is, md)) {
					byte[] buffer = new byte[4096];
					while (mis.read(buffer) >= 0) {
					}
				}
				byte[] digest = md.digest();
				sub.onSuccess(bytesToHex(digest));
			} catch (Exception e) {
				sub.onError(e);
			}
		});
		obs = obs.observeOn(scheduler);
		return obs;
	}

	/**
	 * Generate a SHA 512 checksum from the data in the given buffer and asynchronously return the hex encoded hash as a string.
	 * 
	 * @param buffer
	 * @return Observable emitting the SHA 512 checksum
	 */
	public static Single<String> generateSha512Sum(Buffer buffer) {
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		Single<String> obs = Single.create(sub -> {
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-512");
				try (InputStream is = new ByteArrayInputStream(((io.vertx.core.buffer.Buffer) buffer.getDelegate()).getBytes());
						DigestInputStream mis = new DigestInputStream(is, md)) {
					byte[] b = new byte[4096];
					while (mis.read(b) >= 0) {
					}
				}
				byte[] digest = md.digest();
				sub.onSuccess(bytesToHex(digest));
			} catch (Exception e) {
				sub.onError(e);
			}
		});
		obs = obs.observeOn(scheduler);
		return obs;
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
