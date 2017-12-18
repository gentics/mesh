package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.io.IOUtils;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

public class RxUtilTest {

	@Test
	public void testThen() {
		Observable.just(1, 2, 3).doOnNext(item -> {
			System.out.println("Current item " + item.intValue());
		}).last().compose(RxUtil.then(() -> {
			System.out.println("After all");
			return Observable.just(this);
		})).doOnNext(item -> {
			System.out.println(item.getClass());
		}).subscribe();
	}

	@Test
	public void testToInputStream() throws IOException {
		Observable<Buffer> buf = Observable.just(Buffer.buffer("text"));
		try (InputStream ins = RxUtil.toInputStream(buf, Vertx.vertx())) {
			String text = IOUtils.toString(ins);
			assertEquals("text", text);
		}
	}

}