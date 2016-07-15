package com.gentics.mesh.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

public final class RxUtil {

	private RxUtil() {
	}

	/**
	 * Concat the given list of observables and return a single observable that emits the listed elements.
	 * 
	 * @param list
	 * @return
	 * @deprecated Use {@link Observable#concat(Observable)} in combination with {@link Observable#from(Iterable)}
	 */
	@Deprecated
	public static <T> Observable<T> concatList(List<Observable<T>> list) {
		Observable<T> merged = Observable.empty();
		for (Observable<T> element : list) {
			merged = merged.concatWith(element);
		}
		return merged;
	}

	public static <T> Completable andThenCompletable(Single<T> source, Func1<T, Completable> mappingFunction) {
		return Observable.merge(source.toObservable().map(v -> mappingFunction.call(v).toObservable())).toCompletable();
	}

	//	/**
	//	 * Concat the given list of observables and return a single observable that emits the listed elements. Subscribes to the given observables eagerly.
	//	 *
	//	 * @param list
	//	 * @return
	//	 */
	//	public static <T> Observable<T> concatList(List<Observable<T>> list) {
	//		Objects.requireNonNull(list, "Provided list of Observables is null!");
	//
	//		// shortcut for empty list:
	//		if (list.size() == 0) {
	//			return Observable.empty();
	//		}
	//
	//		// shortcut for list with one observable:
	//		if (list.size() == 1) {
	//			Observable<T> obs = list.get(0);
	//			Objects.requireNonNull(obs);
	//			return obs;
	//		}
	//
	//		return Observable.create(sub -> {
	//			AtomicInteger nextToEmit = new AtomicInteger(0);
	//			AtomicBoolean done = new AtomicBoolean(false);
	//			final int size = list.size();
	//			int subscriptionIndex = 0;
	//			Subscription[] subscriptions = new Subscription[size];
	//			List<T>[] results = new List[size];
	//			Object[] listLocks = new Object[size];
	//			boolean[] completed = new boolean[size];
	//
	//			// Stop all actions when subscriber unsubs
	//			sub.add(Subscriptions.create(() -> {
	//				done.set(true);
	//				synchronized (subscriptions) {
	//					// unsub could happen when subbing to obs in list
	//					for (Subscription s : subscriptions) {
	//						if (s != null) {
	//							s.unsubscribe();
	//						}
	//					}
	//				}
	//			}));
	//
	//			// Fill listLocks. We need to synchronize on these because it can happen that
	//			// an observable emits an item while we are fast forwarding (simultaneous access to same list)
	//			for (int i = 0; i < size; i++) {
	//				listLocks[i] = new Object();
	//			}
	//
	//			// Subscribe to all observables
	//			for (Observable<T> obs : list) {
	//				// we need to capture the current index in lambdas
	//				final int currentIndex = subscriptionIndex;
	//				synchronized (subscriptions) {
	//					// unsub could happen when subbing to obs in list
	//					subscriptions[subscriptionIndex] = obs.subscribe(element -> {
	//						// do nothing when done
	//						if (done.get()) {
	//							return;
	//						}
	//
	//						if (nextToEmit.get() == currentIndex) {
	//							// It's this observable's turn. Just emit the element
	//							sub.onNext(element);
	//						} else {
	//							// It's not this observable's turn. Save result for later
	//							synchronized (listLocks[currentIndex]) {
	//								// we to synchronize because this can conflict with fast forwarding
	//								List<T> resultList = results[currentIndex];
	//								if (resultList == null) {
	//									resultList = new ArrayList<>();
	//									results[currentIndex] = resultList;
	//								}
	//								resultList.add(element);
	//							}
	//						}
	//					}, error -> {
	//						// When an error occurs, nothing else should happen. Also unsubscribe to all observables.
	//						done.set(true);
	//						synchronized (subscriptions) {
	//							for (Subscription s : subscriptions) {
	//								if (s != null) {
	//									s.unsubscribe();
	//								}
	//							}
	//						}
	//						// TODO maybe wrap it in another error with more information (index, happened in concat)?
	//						sub.onError(error);
	//					}, () -> {
	//						// do nothing when done
	//						if (done.get()) {
	//							return;
	//						}
	//
	//						nextToEmit.getAndUpdate(i -> {
	//							if (i + 1 == size) {
	//								// last observable completed
	//								sub.onCompleted();
	//								return i;
	//							} else if (currentIndex == i) {
	//								// current observable completed, we have to fast forward
	//								// fast forwarding: emit all items saved until we reach an observable that has not
	//								// yet completed
	//								boolean ffComplete = false;
	//								while (!ffComplete) {
	//									i++;
	//									if (i >= size) {
	//										// all observables done
	//										sub.onCompleted();
	//										ffComplete = true;
	//									} else {
	//										ffComplete = !completed[i];
	//										synchronized (listLocks[i]) {
	//											// we to synchronize because this can conflict with saving elements for later
	//											List<T> l = results[i];
	//											if (l != null) {
	//												l.forEach(sub::onNext);
	//											}
	//											// for garbage collection:
	//											if (completed[i]) {
	//												results[i] = null;
	//												listLocks[i] = null;
	//											}
	//										}
	//									}
	//								}
	//								return i;
	//							} else {
	//								// An observable that is not on its turn completed.
	//								// Sets the flag for fast forwarding.
	//								completed[currentIndex] = true;
	//								return i;
	//							}
	//						});
	//					});
	//					subscriptionIndex++;
	//				}
	//			}
	//		});
	//	}

	public static <T> void noopAction(T nix) {

	}

	public final static <T1, T2, R extends Observable<R2>, R2> Observable<R> flatZip(Observable<? extends T1> o1, Observable<? extends T2> o2,
			final Func2<? super T1, ? super T2, Observable<R>> zipFunction) {
		return Observable.zip(o1, o2, zipFunction).flatMap(x -> x);
	}

	/**
	 * Wait for the given observable to complete before emitting any items from the source observable.
	 *
	 * @param o1
	 * @return
	 */
	public static <T> Transformer<T, T> delay(Observable<?> o1) {
		return source -> {
			return source.delaySubscription(() -> o1.ignoreElements());
		};
	}

	public static <T, U> Transformer<T, U> then(Func0<Observable<U>> o1) {
		return source -> {
			return Observable.defer(o1).delaySubscription(() -> source.ignoreElements());
		};
	}

	public static <T> Observable<T> concatListNotEager(List<Observable<T>> input) {
		//TODO handle empty list
		return Observable.create(sub -> {
			AtomicInteger index = new AtomicInteger();
			Subscriber<T> subscriber = new Subscriber<T>() {
				@Override
				public void onCompleted() {
					int current = index.incrementAndGet();
					if (current == input.size()) {
						sub.onCompleted();
					} else {
						input.get(current).subscribe(this);
					}
				}

				@Override
				public void onError(Throwable e) {
					sub.onError(e);
				}

				@Override
				public void onNext(T o) {
					sub.onNext(o);
				}
			};
			input.get(0).subscribe(subscriber);
		});
	}

}
