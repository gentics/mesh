package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.io.IOUtils;
import org.junit.Test;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.core.Vertx;

public class RxUtilTest {

	@Test
	public void testToInputStream() throws IOException {
		Observable<Buffer> buf = Observable.just(Buffer.buffer("text"));
		try (InputStream ins = RxUtil.toInputStream(buf, Vertx.vertx())) {
			String text = IOUtils.toString(ins);
			assertEquals("text", text);
		}
	}

}