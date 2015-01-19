package com.gentics.vertx.cailun.linkrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PLinkInputStream extends InputStream {

	private InputStream is;

	private static final byte[] plinkTemplate = new byte[] { '$', '{', 'P', 'a', 'g', 'e' };

	private byte plinkClose = '}';

	private final Queue<Byte> backBuf = new LinkedList<Byte>();

	public PLinkInputStream(InputStream inputStream) {
		is = inputStream;
	}

	@Override
	public int read() throws IOException {
		if (!backBuf.isEmpty()) {
			return backBuf.poll();
		}
		int b = is.read();
		if (b == plinkTemplate[0]) {
			b = peekAndReplace();
		}
		return b;
	}

	private byte peekAndReplace() throws IOException {
		List<Byte> peekFailBuffer = new ArrayList<Byte>();
		int templatePos = 1;
		int c = -1;
		peekFailBuffer.add((byte) plinkTemplate[0]);

		while (templatePos < plinkTemplate.length && (c = is.read()) == plinkTemplate[templatePos]) {
			templatePos++;
			peekFailBuffer.add((byte) c);
		}

		if (templatePos == plinkTemplate.length) {
			// fount PLINK
			StringBuffer plinkBuffer = new StringBuffer();
			c = is.read();
			while (c != plinkClose && c != -1) {
				plinkBuffer.append((char) c);
				c = is.read();
			}
			String currentPLink = plinkBuffer.toString();

			String out = "LINK";
			byte[] bytes = out.getBytes();
			for (byte b : bytes) {
				backBuf.offer(b);
			}
		} else {
			if (c != -1) {
				peekFailBuffer.add((byte) c);
			}
			for (Byte b : peekFailBuffer) {
				backBuf.offer(b);
			}
		}

		Byte ret = null;
		if (backBuf.isEmpty()) {
			ret = (byte) is.read();
		} else {
			ret = backBuf.poll();
		}

		return ret;
	}

}