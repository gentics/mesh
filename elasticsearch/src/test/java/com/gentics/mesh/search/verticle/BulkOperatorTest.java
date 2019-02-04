package com.gentics.mesh.search.verticle;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.core.Vertx;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class BulkOperatorTest {
	private ElasticSearchRequest nonBulkable;
	private Bulkable bulkable;
	private BulkOperator bulkOperator;
	private static final int bulkTime = 50;

	@Before
	public void setUp() throws Exception {
		nonBulkable = mock(ElasticSearchRequest.class);
		bulkable = mock(Bulkable.class);
		bulkOperator = new BulkOperator(Vertx.vertx(), Duration.ofMillis(bulkTime), 1000);
	}

	/**
	 * Creates a flowable that emits certain amounts of requests.
	 * The first number is the amount of non bulkable requests, the second the number of bulkable requests.
	 *
	 * @param amounts
	 * @return
	 */
	private Flowable<ElasticSearchRequest> createAlternatingRequests(int ...amounts) {
		return Flowable.range(0, amounts.length)
			.flatMap(i -> i % 2 == 0
				? Flowable.just(nonBulkable).repeat(amounts[i])
				: Flowable.just(bulkable).repeat(amounts[i])
			);
	}

	private Flowable<ElasticSearchRequest> createNotCompletedAlternatingRequests(int ...amounts) {
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
		TestSubscriber<ElasticSearchRequest> test = createNotCompletedAlternatingRequests(1, 3)
			.lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.await(bulkTime * 2, TimeUnit.MILLISECONDS);
		test.assertValueCount(2);
	}

	@Test
	public void testManualFlushing() {
		TestSubscriber<ElasticSearchRequest> test = createNotCompletedAlternatingRequests(1, 3)
			.lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.assertValueAt(0, this::isNonBulkRequest);
		bulkOperator.flush();
		test.assertValueCount(2);
		test.assertValueAt(1, this::isBulkRequest);
	}

	private boolean isBulkRequest(ElasticSearchRequest request) {
		return request instanceof BulkRequest;
	}

	private boolean isNonBulkRequest(ElasticSearchRequest request) {
		return request == nonBulkable;
	}
}
