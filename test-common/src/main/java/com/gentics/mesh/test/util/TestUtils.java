package com.gentics.mesh.test.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.rest.common.ListResponse;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class TestUtils {

	private TestUtils() {

	}

	private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

	public static void runAndWait(Runnable runnable) {

		Thread thread = run(runnable);
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Done waiting");
	}

	public static Thread run(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.start();
		return thread;
	}

	public static void waitFor(CyclicBarrier barrier) {
		try {
			barrier.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a random hash
	 * 
	 * @param len
	 * @return
	 */
	public static String getRandomHash(int len) {
		String hash = new String();

		while (hash.length() < len) {
			int e = (int) (Math.random() * 62 + 48);

			// Only use 0-9 and a-z characters
			if (e >= 58 && e <= 96) {
				continue;
			}
			hash += (char) e;
		}
		return hash;
	}

	public static String getHostname() throws UnknownHostException {
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		return localMachine.getHostName();
	}

	/**
	 * Iterates over the iterable and returns the size.
	 * 
	 * @param it
	 * @return
	 */
	public static long size(Iterable<?> it) {
		Iterator<?> iterator = it.iterator();
		long size = 0;
		while (iterator.hasNext()) {
			iterator.next();
			size++;
		}
		return size;
	}

	/**
	 * Return a free port random port by opening an socket and check whether it is currently used. Not the most elegant or efficient solution, but works.
	 * 
	 * @return
	 */
	public static int getRandomPort() {
		ServerSocket socket = null;

		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException ioe) {
			return -1;
		} finally {
			// if we did open it cause it's available, close it
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
					e.printStackTrace();
				}
			}
		}
	}

	public static String getResourceAsString(String path) {
		try {
			return IOUtils.toString(TestUtils.class.getResourceAsStream(path), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getJson(String name) throws IOException {
		return getJson(name, StandardCharsets.UTF_8);
	}

	/**
	 * Return the json data from classpath.
	 * 
	 * @param name
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public static String getJson(String name, Charset charset) throws IOException {
		return IOUtils.toString(TestUtils.class.getResourceAsStream("/json/" + name), charset);
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static <T> List<T> toList(Iterator<? extends T> it) {
		List<T> list = new ArrayList<>();
		while (it.hasNext()) {
			list.add(it.next());
		}
		return list;
	}

	public static <T> List<T> toList(Iterable<? extends T> it) {
		return toList(it.iterator());
	}

	public static long size(Iterator<?> it) {
		return toList(it).size();
	}

	public static <T> Observable<T> listObservable(Single<? extends ListResponse<T>> upstream) {
		return upstream.flatMapObservable(response -> Observable.fromIterable(response.getData()));
	}

	public static <T, R> List<T> difference(Iterable<T> minuend, Iterable<T> subtrahend, Function<T, R> compareBy) {
		Set<R> subSet = streamFromIterable(subtrahend)
			.map(compareBy)
			.collect(Collectors.toSet());

		return streamFromIterable(minuend)
			.filter(item -> !subSet.contains(compareBy.apply(item)))
			.collect(Collectors.toList());
	}

	public static <T> Stream<T> streamFromIterable(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

}
