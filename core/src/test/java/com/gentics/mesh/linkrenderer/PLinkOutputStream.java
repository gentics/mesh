package com.gentics.mesh.linkrenderer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PLinkOutputStream extends OutputStream {

	private OutputStream os;

	private static final byte[] plinkTemplate = new byte[] { '$', '{', 'P', 'a', 'g', 'e' };

	private int nextPlinkPos = 0;

	private byte plinkClose = '}';

	private List<Byte> backBuffer;

	private StringBuffer plinkBuffer = null;

	private boolean plinkOpen = false;

	public PLinkOutputStream(OutputStream outputStream) {
		os = outputStream;
	}

	@Override
	public void write(int b) throws IOException {

		if (plinkOpen) {
			if (b == plinkClose) {
				String currentPLink = plinkBuffer.toString();

				String out = "LINK";
				os.write(out.getBytes());

				nextPlinkPos = 0;
				plinkOpen = false;
				plinkBuffer = null;
			} else {
				plinkBuffer.append((char) b);
			}

		} else {
			if (nextPlinkPos == 0 && b == plinkTemplate[nextPlinkPos]) {
				backBuffer = new ArrayList<Byte>();
			}

			if (b == plinkTemplate[nextPlinkPos]) {
				backBuffer.add((byte) b);
				if (nextPlinkPos == plinkTemplate.length - 1) {
					plinkOpen = true;
					plinkBuffer = new StringBuffer();
				}
				nextPlinkPos++;
			} else if (nextPlinkPos != 0 && b != plinkTemplate[nextPlinkPos] && nextPlinkPos != (plinkTemplate.length - 1)) {
				for (byte bb : backBuffer) {
					os.write(bb);
				}
				os.write(b);
				backBuffer = null;
				nextPlinkPos = 0;
			} else {
				os.write(b);
			}
		}
	}

}
