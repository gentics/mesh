package com.gentics.mesh.core.admin;

import java.io.IOException;

import org.junit.Test;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.WebSocketStream;
import rx.Observable;

public class WSTest {

	@Test
	public void testWS() throws IOException {
		Vertx vertx = Vertx.vertx();
		HttpClient client = vertx.createHttpClient();
		client.websocket(8080, "localhost", "/api/v1/admin/eventbus/websocket", ws -> {
			System.out.println("GOT SOCKET");
			ws.writeFinalTextFrame("test");
			ws.handler(rh -> {
				System.out.println("DATA");
			});
			//			ws.toObservable().subscribe(buffer -> {
			//				System.out.println("Got message " + buffer.toString("UTF-8"));
			//			});
		});
//		WebSocketStream stream = client.websocketStream(8080, "localhost", "/api/v1/admin/eventbus/websocket");
//		stream.toObservable().subscribe(socket -> {
//			Observable<Buffer> dataObs = socket.toObservable();
//			dataObs.subscribe(buffer -> {
//				System.out.println("Got message " + buffer.toString("UTF-8"));
//			});
//		} , error -> {
//			System.out.println(error);
//		} , () -> {
//			System.out.println("All done");
//		});

		System.in.read();

	}

}
