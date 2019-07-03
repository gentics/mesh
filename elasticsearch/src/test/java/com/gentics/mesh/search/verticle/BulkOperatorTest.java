package com.gentics.mesh.search.verticle;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.search.verticle.bulk.BulkOperator;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.core.Vertx;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class BulkOperatorTest {

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	private SearchRequest nonBulkable;
	private Bulkable bulkable;
	private BulkOperator bulkOperator;

	private static final int bulkTime = 50;


	@Before
	public void setUp() throws Exception {
		nonBulkable = mock(SearchRequest.class);
		bulkable = mock(Bulkable.class);
		when(bulkable.bulkLength()).thenReturn(1000L);
		bulkOperator = new BulkOperator(Vertx.vertx(), Duration.ofMillis(bulkTime), 100, 100000000);
	}

	/**
	 * Creates a flowable that emits certain amounts of requests.
	 * The first number is the amount of non bulkable requests, the second the number of bulkable requests.
	 *
	 * @param amounts
	 * @return
	 */
	private Flowable<SearchRequest> createAlternatingRequests(int ...amounts) {
		return Flowable.range(0, amounts.length)
			.concatMap(i -> i % 2 == 0
				? Flowable.just(nonBulkable).repeat(amounts[i])
				: Flowable.just(bulkable).repeat(amounts[i])
			);
	}

	private Flowable<SearchRequest> createNotCompletedAlternatingRequests(int ...amounts) {
		return Flowable.merge(
			Flowable.never(),
			createAlternatingRequests(amounts)
		);
	}

	@Test
	public void testBulking() throws InterruptedException {
		createAlternatingRequests(2, 3)
			.lift(bulkOperator)
			.count()
			.test()
			.await()
			.assertValue(3L);

		createAlternatingRequests(1, 3, 1)
			.lift(bulkOperator)
			.count()
			.test()
			.await()
			.assertValue(3L);

		createAlternatingRequests(1, 3, 1, 2)
			.lift(bulkOperator)
			.count()
			.test()
			.await()
			.assertValue(4L);
	}

	@Test
	public void testTimeBasedFlushing() throws InterruptedException {
		TestSubscriber<SearchRequest> test = createNotCompletedAlternatingRequests(1, 3)
			.lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.await(bulkTime * 2, TimeUnit.MILLISECONDS);
		test.assertValueCount(2);
	}

	@Test
	public void testManualFlushing() {
		TestSubscriber<SearchRequest> test = createNotCompletedAlternatingRequests(1, 3)
			.lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.assertValueAt(0, this::isNonBulkRequest);
		bulkOperator.flush();
		test.assertValueCount(2);
		test.assertValueAt(1, this::isBulkRequest);
	}

	@Test
	public void testBackpressure() {
		Flowable<SearchRequest> upstream = createAlternatingRequests(2, 3, 1, 5, 1);
		AtomicLong emittedItems = new AtomicLong();

		TestSubscriber<SearchRequest> subscriber = upstream
			.doOnNext(ignore -> emittedItems.incrementAndGet())
			.lift(bulkOperator)
			.test(0);

		subscriber.request(1);
		subscriber.assertValueCount(1);
		assertThat(emittedItems.get()).isEqualTo(1);

		subscriber.request(1);
		subscriber.assertValueCount(2);
		assertThat(emittedItems.get()).isEqualTo(2);

		subscriber.request(1);
		subscriber.assertValueCount(3);
		assertThat(emittedItems.get()).isEqualTo(6);

		subscriber.request(1);
		subscriber.assertValueCount(4);
		assertThat(emittedItems.get()).isEqualTo(6);

		subscriber.request(1);
		subscriber.assertValueCount(5);
		assertThat(emittedItems.get()).isEqualTo(12);

		subscriber.request(1);
		subscriber.assertValueCount(6);
		assertThat(emittedItems.get()).isEqualTo(12);

		subscriber.assertComplete();
	}

	@Test
	public void testSizeLimit() {
		BulkOperator operator = new BulkOperator(Vertx.vertx(), Duration.ofMinutes(1), 100, 100000000);
		createAlternatingRequests(1, 500)
			.lift(operator)
			.test()
			.assertValueCount(6)
			.assertComplete();
	}

	@Test
	public void testLengthLimit() throws InterruptedException {
		BulkOperator operator = new BulkOperator(Vertx.vertx(), Duration.ofMillis(bulkTime), 100, 5000);
		TestSubscriber<SearchRequest> test = createNotCompletedAlternatingRequests(1, 12)
			.lift(operator)
			.test()
			.assertValueCount(3)
			.assertNotComplete();

		test.await(bulkTime * 2, TimeUnit.MILLISECONDS);

		test.assertValueCount(4)
			.assertNotComplete();
	}

	private boolean isBulkRequest(SearchRequest request) {
		return request instanceof BulkRequest;
	}

	private boolean isNonBulkRequest(SearchRequest request) {
		return request == nonBulkable;
	}
}
