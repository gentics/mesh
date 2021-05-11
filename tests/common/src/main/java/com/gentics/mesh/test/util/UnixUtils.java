package com.gentics.mesh.test.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public final class UnixUtils {

	public static int getUid() throws IOException {
		InputStream in = null;
		try {
			String userName = System.getProperty("user.name");
			String command = "id -u " + userName;
			Process child = Runtime.getRuntime().exec(command);

			// Get the input stream and read from it
			in = child.getInputStream();
			String output = IOUtils.toString(in);
			return Integer.valueOf(output.trim());
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

}
