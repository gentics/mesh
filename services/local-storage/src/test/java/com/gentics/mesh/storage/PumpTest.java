package com.gentics.mesh.storage;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.gentics.mesh.util.RxUtil;

import io.reactivex.Observable;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.streams.Pump;

public class PumpTest {

	@Test
	public void testPump() throws IOException {
		Vertx vertx = Vertx.vertx();
		File file = new File("target/test");
		file.delete();

		Observable<Buffer> rs = Observable.just(Buffer.buffer("test123"));

		vertx.fileSystem().rxOpen(file.getAbsolutePath(), new OpenOptions()).map(f -> {
			Pump pump = RxUtil.pump(rs, f);
			pump.start();
			return f;
		}).map(AsyncFile::flush).toCompletable().blockingAwait();
		assertEquals("test123", FileUtils.readFileToString(file));
	}

//	/**
//	 * Creates a pump which applies a workaround for vertx-rxjava#123.
//	 * 
//	 * @param stream
//	 * @param file
//	 * @return
//	 */
//	public static Pump pump(Observable<Buffer> stream, AsyncFile file) {
//		ReadStream<io.vertx.core.buffer.Buffer> rss = ReadStreamSubscriber.asReadStream(stream.map(Buffer::getDelegate), Function.identity());
//		io.vertx.core.streams.Pump pump = io.vertx.core.streams.Pump.pump(rss, file.getDelegate());
//		return Pump.newInstance(pump);
//	}
}
