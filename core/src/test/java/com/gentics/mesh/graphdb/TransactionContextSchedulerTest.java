package com.gentics.mesh.graphdb;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.test.AbstractDBTest;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class TransactionContextSchedulerTest extends AbstractDBTest {

	@Test
	public void testRetry() throws Exception {
		Observable.merge(getObs("A"), getObs("B"), getObs("C")).toBlocking().forEach(next -> {
			System.out.println("Result: " + next);
		});

		Thread.sleep(1000);
	}

	private Observable<String> getObs(String prefix) {
		Operator operator = null;
		return Observable.just(prefix + ":one", prefix + ":two").lift(operator).map(element -> {
			//			System.out.println("Graph size: " + Database.getThreadLocalGraph().v().count());
			System.out.println(element);

			return element;
		}).doOnError(error -> {
			//System.out.println("ERROR:" + error.getMessage());
		});
	}
	
	private <T, R> Operator<T, R> testOperator() {
		return new Operator<T, R>() {
			@Override
			public Subscriber<? super R> call(Subscriber<? super T> t) {
				// TODO Auto-generated method stub
//				t.
				return null;
			}
			
		};
	}

	class TestWorker extends Scheduler {

		@Override
		public Worker createWorker() {
			// TODO Auto-generated method stub
			return new Worker() {

				@Override
				public void unsubscribe() {
					// TODO Auto-generated method stub

				}

				@Override
				public boolean isUnsubscribed() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public Subscription schedule(Action0 action) {
					System.out.println("Runnnnn");
					Trx tx = MeshSpringConfiguration.getInstance().database().trx();
					
					try {
						action.call();
						if (true) {
							//						action.call();
							throw new RuntimeException("blub");
						}

					} catch (Exception e){
						tx.close();
					}
					System.out.println("Donnnnn");
					return Subscriptions.empty();

				}

				@Override
				public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
					// TODO Auto-generated method stub
					return null;
				}

			};

		}

	}
}
