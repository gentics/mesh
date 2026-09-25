
package com.gentics.mesh.core.eventbus;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.EventbusEvent;
import com.gentics.mesh.rest.client.MeshRestClientUtil;
import com.gentics.mesh.rest.client.MeshWebsocket;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Completable;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class EventbusEndpointTest extends AbstractMeshTest {
	/**
	 * Address of the "test is finished" event
	 */
	public final static String  TEST_FINISHED_ADDRESS = "custom.testFinished";

	private MeshWebsocket ws;

	@Before
	public void setupEventbus() throws Exception {
		ws = client().eventbus();
		// Wait for initial connection
		ws.connections().blockingFirst();

		// register the "test is finished" event
		ws.registerEvents(TEST_FINISHED_ADDRESS);
	}

	@After
	public void closeEventBus() {
		if (ws != null) {
			ws.close();
		}
	}

	@Test(timeout = 4_000)
	public void testExternalEventbusMessage(TestContext context) throws Exception {

		Async async = context.async();
		MeshEvent allowedAddress = USER_CREATED;

		// Register
		ws.registerEvents(allowedAddress);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			MeshAssertions.assertThat(event.getBodyAsJson().get("test").textValue()).isEqualTo("someValue");
			async.complete();
		});

		Thread.sleep(1000);
		vertx().eventBus().publish(allowedAddress.address, new JsonObject().put("test", "someValue"));
	}

	@Test(timeout = 4_000)
	public void testNodeDeleteEvent(TestContext context) throws Exception {
		Async async = context.async();

		ws.registerEvents(NODE_DELETED);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			NodeMeshEventModel body = JsonUtil.readValue(event.getBodyAsJson().toString(), NodeMeshEventModel.class);
			context.assertNotNull(body.getUuid());
			context.assertEquals("content", body.getSchema().getName());
			async.complete();
		});
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid()));
	}

	@Test(timeout = 4_000)
	public void testNodeDeleteLanguageEvent(TestContext context) throws Exception {
		Async async = context.async();

		ws.registerEvents(NODE_CONTENT_DELETED);

		ws.errors().subscribe(context::fail);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			NodeMeshEventModel body = JsonUtil.readValue(event.getBodyAsJson().toString(), NodeMeshEventModel.class);
			context.assertNotNull(body.getUuid());
			context.assertEquals("content", body.getSchema().getName());
			context.assertEquals("en", body.getLanguageTag());
			async.complete();
		});
		call(() -> client().deleteNode(PROJECT_NAME, contentUuid(), "en"));
	}

	@Test(timeout = 4_000)
	public void testNodeUpdateEvent(TestContext context) {
		Async async = context.async();

		// Register
		ws.registerEvents(NODE_UPDATED);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			NodeMeshEventModel body = JsonUtil.readValue(event.getBodyAsJson().toString(), NodeMeshEventModel.class);
			assertNotNull(body.getUuid());
			assertEquals("content", body.getSchema().getName());
			async.complete();
		});

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.getFields().put("slug", FieldUtil.createStringField("blub"));
		request.setVersion(response.getVersion());
		request.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, contentUuid(), request));

		NodeResponse response2 = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		assertNotEquals(response.getVersion(), response2.getVersion());
	}

	@Test
	public void testCustomEventHandling(TestContext context) {
		Async asyncRec = context.async();

		ws.registerEvents("custom.myEvent");

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			String body = event.getBodyAsString();
			assertEquals("someText", body);
			asyncRec.complete();
		});

		// Send msg
		ws.publishEvent("custom.myEvent", "someText");
	}

	@Test(timeout = 4_000)
	public void testMultipleRegistration(TestContext context) {
		Async asyncRec = context.async();

		// Registering the same event twice must not cause an error.
		ws.registerEvents("custom.myEvent");
		ws.registerEvents("custom.myEvent");

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			String body = event.getBodyAsString();
			assertEquals("someText", body);
			asyncRec.complete();
		});

		// Send msg
		ws.publishEvent("custom.myEvent", "someText");
	}

	@Test
	public void testOverlongAddress(TestContext context) {
		String tooLongAddress = "custom.314159265358979323846264338327950288419716939937510582097494459230781640628620899862803482534211706798214808651328230664709384460955058223172535940812848111745028410270193852110555964462294895493038196";

		Async asyncRec = context.async();

		// Registering an overlong address must not cause an error.
		ws.registerEvents(tooLongAddress);

		// Handle msgs
		ws.events().firstOrError().subscribe(handle(context, asyncRec));

		// Send msg with too long address
		ws.publishEvent(tooLongAddress, "someText");
		sendFinishEvent();
	}

	@Test
	public void testInvalidCustomAddress(TestContext context) {
		String invalidAddress = "invalid.myEvent";

		Async asyncRec = context.async();

		// Registering an invalid custom address must not cause an error.
		ws.registerEvents(invalidAddress);

		// Handle msgs
		ws.events().firstOrError().subscribe(handle(context, asyncRec));

		// Send msg
		ws.publishEvent("invalid.myEvent", "someText");
		sendFinishEvent();
	}

	// TODO Fix this test
//	@Test(timeout = 20_000)
//	public void testAutoReconnect(TestContext context) {
//		Async nodesCreated = context.strictAsync(2);
//		Async connections = context.strictAsync(2);
//		// The first error is the disconnect itself, the second one is the failing first reconnect.
//		Async errors = context.strictAsync(2);
//		CompletableSubject firstReconnect = CompletableSubject.create();
//
//		ws.registerEvents(MeshEvent.NODE_CREATED);
//		ws.events().subscribe(event -> nodesCreated.countDown(), context::fail);
//
//		ws.connections()
//			.doOnNext(ignore -> connections.countDown())
//			// Skip initial connection
//			.skip(1)
//			.subscribe(ignore -> createBinaryContent().subscribe());
//
//		ws.errors().take(2).subscribe(ignore -> {
//			errors.countDown();
//
//			if (errors.count() == 0) {
//				// The first reconnect failed, we can now start the REST verticle again.
//				firstReconnect.onComplete();
//			}
//		});
//
//		createBinaryContent().toCompletable()
//			.andThen(stopRestVerticle())
//			.andThen(verifyStoppedRestVerticle())
//			.andThen(firstReconnect)
//			.andThen(startRestVerticle())
//			.subscribe(() -> {}, context::fail);
//	}

	@Test
	public void testOneOfHelper(TestContext context) {
		Async async = context.async(2);

		// Register
		ws.registerEvents(NODE_UPDATED);

		// Handle msgs
		ws.events().firstOrError().subscribe(event -> {
			NodeMeshEventModel body = JsonUtil.readValue(event.getBodyAsJson().toString(), NodeMeshEventModel.class);
			assertNotNull(body.getUuid());
			assertEquals("content", body.getSchema().getName());
			async.countDown();
		});

		ws.events().filter(MeshRestClientUtil.isOneOf(NODE_UPDATED)).subscribe(ignore -> async.countDown());
		ws.events().filter(MeshRestClientUtil.isOneOf(NODE_CREATED))
			.subscribe(ignore -> context.fail("No node should have been created"));

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.getFields().put("slug", FieldUtil.createStringField("blub"));
		request.setVersion(response.getVersion());
		request.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, contentUuid(), request));

		NodeResponse response2 = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		assertNotEquals(response.getVersion(), response2.getVersion());
	}

	@Test
	public void testHeartbeat() throws InterruptedException {
		// Simply tests if the connections has no errors for 10 seconds.

		ws.errors().subscribe(ignore -> fail());

		Thread.sleep(10000);
	}

	/**
	 * Test that internal mesh events (like {@link MeshEvent#PROJECT_CREATED}) cannot be published over the websocket
	 * @param context test context
	 */
	@Test
	public void testPublishProjectCreatedEvent(TestContext context) {
		Async async = context.async();

		MeshElementEventModelImpl event = new MeshElementEventModelImpl();
		event.setName("bliblablubb");

		ws.registerEvents(PROJECT_CREATED);

		ws.events().firstOrError().subscribe(handle(context, async));

		ws.publishEvent(PROJECT_CREATED.address, event);
		sendFinishEvent();
	}

	/**
	 * Return a handler for {@link EventbusEvent} instances.
	 * The handler will allow all expected events (counting down for async) and end the test when {@link #TEST_FINISHED_ADDRESS} is fired.
	 * All other events will fail the test
	 * @param context test context
	 * @param async Async
	 * @param expected optional expected events
	 * @return handler
	 */
	protected Consumer<? super EventbusEvent> handle(TestContext context, Async async, String...expected) {
		List<String> expectedList = Arrays.asList(expected);

		return event -> {
			if (Strings.CS.equals(event.getAddress(), TEST_FINISHED_ADDRESS)) {
				async.complete();
			} else if (expectedList.contains(event.getAddress())) {
				async.countDown();
			} else {
				context.fail("The unexpected event %s was published".formatted(event.getAddress()));
			}
		};
	}

	/**
	 * Send the "test is finished" event
	 */
	protected void sendFinishEvent() {
		ws.publishEvent(TEST_FINISHED_ADDRESS, "true");
	}

	/**
	 * Verifies that the rest verticle is actually stopped.
	 * @return
	 */
	private Completable verifyStoppedRestVerticle() {
		return client().me()
			.toCompletable()
			.compose(RxUtil::flip);
	}

}
