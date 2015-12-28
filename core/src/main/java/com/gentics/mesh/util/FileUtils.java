package com.gentics.mesh.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import com.gentics.mesh.Mesh;

import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.Scheduler;

public final class FileUtils {

	protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	private FileUtils() {
	}

	/**
	 * Generate a SHA 512 checksum from the given file and asynchronously return the hex encoded hash as a string.
	 * 
	 * @param path
	 */
	public static Observable<String> generateSha512Sum(String path) {
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		Observable<String> obs = Observable.create(sub -> {
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-512");
				try (InputStream is = Files.newInputStream(Paths.get(path))) {
					new DigestInputStream(is, md);
					/* Read stream to EOF as normal... */
				}
				byte[] digest = md.digest();
				sub.onNext(bytesToHex(digest));
				sub.onCompleted();
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
