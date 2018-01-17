package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.tika.io.IOUtils;
import org.junit.Test;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;
import io.vertx.reactivex.core.Vertx;

public class RxUtilTest {

	@Test
	public void testToInputStream() throws IOException {
		Observable<Buffer> data = Observable.range(1, 5).map(String::valueOf).map(Buffer::buffer);
		Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS);
		Observable<Buffer> buf = Observable.zip(data, interval, (b, i) -> b);
		try (InputStream ins = RxUtil.toInputStream(buf, Vertx.vertx())) {
			String text = IOUtils.toString(ins);
			assertEquals("12345", text);
		}
	}

}