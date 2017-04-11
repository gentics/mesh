package com.gentics.mesh.core.console;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provider for console specific interactions. (e.g. Read Input, Password etc)
 */
public interface ConsoleProvider {

	/**
	 * Read next byte from console.
	 * 
	 * @see {@link InputStream#read()}
	 * @return the next byte of data, or -1 if the end of the stream is reached.
	 * @throws IOException
	 */
	int read() throws IOException;

}
