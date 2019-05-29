package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.vertx.core.Vertx;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class BulkOperatorTest {
	private SearchRequest nonBulkable;
	private Bulkable bulkable;
	private BulkOperator bulkOperator;
	private static final int bulkTime = 50;

	@Before
	public void setUp() throws Exception {
		nonBulkable = mock(SearchRequest.class);
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
	private Observable<SearchRequest> createAlternatingRequests(int ...amounts) {
		return Observable.range(0, amounts.length)
			.flatMap(i -> i % 2 == 0
				? Observable.just(nonBulkable).repeat(amounts[i])
				: Observable.just(bulkable).repeat(amounts[i])
			);
	}

	private Observable<SearchRequest> createNotCompletedAlternatingRequests(int ...amounts) {
		return Observable.merge(
			Observable.never(),
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
		TestObserver<SearchRequest> test = createNotCompletedAlternatingRequests(1, 3)
			.lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.await(bulkTime * 2, TimeUnit.MILLISECONDS);
		test.assertValueCount(2);
	}

	@Test
	public void testManualFlushing() {
		TestObserver<SearchRequest> test = createNotCompletedAlternatingRequests(1, 3)
			.lift(bulkOperator)
			.test();

		test.assertValueCount(1);
		test.assertValueAt(0, this::isNonBulkRequest);
		bulkOperator.flush();
		test.assertValueCount(2);
		test.assertValueAt(1, this::isBulkRequest);
	}

	private boolean isBulkRequest(SearchRequest request) {
		return request instanceof BulkRequest;
	}

	private boolean isNonBulkRequest(SearchRequest request) {
		return request == nonBulkable;
	}
}
