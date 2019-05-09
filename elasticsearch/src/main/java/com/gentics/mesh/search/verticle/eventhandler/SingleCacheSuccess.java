/** Copyright 2019 Sergey Chelombitko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gentics.mesh.search.verticle.eventhandler;

import com.google.common.base.Preconditions;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores the success value from the source Single and replays it to observers.
 */
public final class SingleCacheSuccess<T> extends Single<T> implements SingleObserver<T> {
	@SuppressWarnings("rawtypes")
	private static final CacheDisposable[] EMPTY = new CacheDisposable[0];
	@SuppressWarnings("rawtypes")
	private static final CacheDisposable[] TERMINATED = new CacheDisposable[0];

	private final Single<T> source;
	private final AtomicBoolean wip = new AtomicBoolean();
	private final AtomicReference<CacheDisposable<T>[]> observers;
	private T value;

	public static <T> Single<T> create(Single<T> source) {
		Preconditions.checkNotNull(source, "source is null");
		return RxJavaPlugins.onAssembly(new SingleCacheSuccess<>(source));
	}

	@SuppressWarnings("unchecked")
	private SingleCacheSuccess(Single<T> source) {
		this.source = source;
		observers = new AtomicReference<CacheDisposable<T>[]>(EMPTY);
	}

	@Override
	protected void subscribeActual(SingleObserver<? super T> observer) {
		final CacheDisposable<T> d = new CacheDisposable<>(observer, this);
		observer.onSubscribe(d);

		if (add(d)) {
			if (d.isDisposed()) {
				remove(d);
			}
		} else {
			observer.onSuccess(value);
			return;
		}

		if (!wip.getAndSet(true)) {
			source.subscribe(this);
		}
	}

	@Override
	public void onSubscribe(Disposable d) {
		// not supported by this operator
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onSuccess(T value) {
		this.value = value;

		for (CacheDisposable<T> observer : observers.getAndSet(TERMINATED)) {
			if (!observer.isDisposed()) {
				observer.actual.onSuccess(value);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onError(Throwable e) {
		wip.set(false);

		for (CacheDisposable<T> observer : observers.getAndSet(EMPTY)) {
			if (!observer.isDisposed()) {
				observer.actual.onError(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean add(CacheDisposable<T> observer) {
		for (; ; ) {
			final CacheDisposable<T>[] a = observers.get();
			if (a == TERMINATED) {
				return false;
			}
			final int n = a.length;
			final CacheDisposable<T>[] b = new CacheDisposable[n + 1];
			System.arraycopy(a, 0, b, 0, n);
			b[n] = observer;
			if (observers.compareAndSet(a, b)) {
				return true;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void remove(CacheDisposable<T> observer) {
		for (; ; ) {
			final CacheDisposable<T>[] a = observers.get();
			final int n = a.length;
			if (n == 0) {
				return;
			}

			int j = -1;
			for (int i = 0; i < n; i++) {
				if (a[i] == observer) {
					j = i;
					break;
				}
			}

			if (j < 0) {
				return;
			}

			final CacheDisposable<T>[] b;
			if (n == 1) {
				b = EMPTY;
			} else {
				b = new CacheDisposable[n - 1];
				System.arraycopy(a, 0, b, 0, j);
				System.arraycopy(a, j + 1, b, j, n - j - 1);
			}
			if (observers.compareAndSet(a, b)) {
				return;
			}
		}
	}

	private static final class CacheDisposable<T> extends AtomicBoolean implements Disposable {
		private static final long serialVersionUID = 4746876330948546833L;

		final SingleObserver<? super T> actual;
		private final SingleCacheSuccess<T> parent;

		CacheDisposable(SingleObserver<? super T> actual, SingleCacheSuccess<T> parent) {
			this.actual = actual;
			this.parent = parent;
		}

		@Override
		public boolean isDisposed() {
			return get();
		}

		@Override
		public void dispose() {
			if (compareAndSet(false, true)) {
				parent.remove(this);
			}
		}
	}
}
