package com.gentics.cailun.test;

import java.io.IOException;
import java.net.ServerSocket;

public final class TestUtil {

	private TestUtil() {

	}
	
	/**
	 * Creates a random hash
	 * 
	 * @return
	 */
	public static String getRandomHash(int len) {
		String hash = new String();

		while (hash.length() < len) {
			int e = (int) (Math.random() * 62 + 48);

			// Only use 0-9 and a-z characters
			if (e >= 58 && e <= 96) {
				continue;
			}
			hash += (char) e;
		}
		return hash;
	}


	/**
	 * Not the most elegant or efficient solution, but works.
	 * 
	 * @param port
	 * @return
	 */
	public static int getRandomPort() {
		ServerSocket socket = null;

		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException ioe) {
			return -1;
		} finally {
			// if we did open it cause it's available, close it
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
					e.printStackTrace();
				}
			}
		}
	}
}
