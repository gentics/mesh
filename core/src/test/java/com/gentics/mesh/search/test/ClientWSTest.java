package com.gentics.mesh.search.test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

public class ClientWSTest {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultPort(4444).setDefaultHost("localhost");

		HttpClient client = vertx.createHttpClient(options);
		client.websocket("/test/websocket", ws -> {
			System.out.println("WS Connected");

			// Register to migration events
			JsonObject msg = new JsonObject().put("type", "register").put("address", "dummy");
			ws.writeFinalTextFrame(msg.encode());

			ws.handler(buff -> {
				String str = buff.toString();
				System.out.println("Got event on client: " + str);
			});
		});
	}

}
