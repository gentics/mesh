package com.gentics.mesh.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;

import io.reactivex.Completable;

/**
 * Abstract base class for uniqueness tests when creating entities in parallel threads
 */
public abstract class AbstractNameUniquenessTest extends AbstractMeshTest {
	/**
	 * Number of parallel threads
	 */
	public final static int NUM_THREADS = 500;

	/**
	 * Test that when trying to create entities with the same names in parallel, only one request (per parent entity, if applicable) succeeds.
	 * All other requests should either get a conflict (if mesh detects the name conflict) or an error (if the error is detected by the database)
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Test
	public void testCreateEntitiesInThreads() throws InterruptedException, ExecutionException, TimeoutException {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger conflictCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();

		Optional<List<String>> optParents = parentEntities();

		ExecutorService service = Executors.newFixedThreadPool(NUM_THREADS);
		Set<Future<?>> futures = new HashSet<>();

		AtomicInteger parentIndex = new AtomicInteger();

		for (int i = 0; i < NUM_THREADS; i++) {
			Optional<String> optParent = optParents
					.map(parents -> {
						String parent = parents.get(parentIndex.get());
						if (parentIndex.incrementAndGet() >= parents.size()) {
							parentIndex.set(0);
						}
						return parent;
					});
			futures.add(service.submit(() -> {
				createEntity(optParent).doOnComplete(() -> {
					successCount.incrementAndGet();
				}).onErrorResumeNext(t -> {
					if (HibernateTestUtils.isConflict(t)) {
						conflictCount.incrementAndGet();
					} else {
						t.printStackTrace();
						errorCount.incrementAndGet();
					}
					return Completable.complete();
				}).blockingAwait();
			}));
		}

		for (Future<?> future : futures) {
			future.get(1, TimeUnit.HOURS);
		}

		int expectedSuccess = optParents.map(List::size).orElse(1);
		assertThat(successCount.get()).as("Successful requests").isEqualTo(expectedSuccess);
	}

	/**
	 * Create the entity for the optional parent
	 * @param optParent optional parent (if applicable)
	 * @return completable for creating the entity
	 */
	protected abstract Completable createEntity(Optional<String> optParent);

	/**
	 * Get an optional list of parent references (if applicable). May return an empty optional
	 * @return optional of parent references
	 */
	protected abstract Optional<List<String>> parentEntities();
}
