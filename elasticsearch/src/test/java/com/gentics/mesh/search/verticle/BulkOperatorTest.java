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

	private Flowable<ElasticSearchRequest> createAlternatingRequests(int ...amounts) {
		return Flowable.range(0, amounts.length)
			.flatMap(i -> i % 2 == 0
				? Flowable.just(nonBulkable).repeat(amounts[i])
				: Flowable.just(bulkable).repeat(amounts[i])
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
		TestSubscriber<ElasticSearchRequest> test = Flowable.merge(
			Flowable.never(),
			createAlternatingRequests(1, 3)
		).lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.await(bulkTime * 2, TimeUnit.MILLISECONDS);
		test.assertValueCount(2);
	}
}
