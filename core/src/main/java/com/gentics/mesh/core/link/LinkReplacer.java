package com.gentics.mesh.core.link;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The link replacer can be used to replace links within the contents of a string.
 * 
 * @param <T>
 */
public class LinkReplacer<T extends AbstractLinkResolver> {

	private static final String START_TAG = "${";
	private static final String END_TAG = "}";

	private static final long DEFAULT_TIMEOUT_MS = 1000L;

	private long timeoutMs = DEFAULT_TIMEOUT_MS;

	private ExecutorService executor;

	private LinkResolverFactory<T> factory;

	/**
	 * Create a new link replacer. The factory will be used to retrieve link resolvers.
	 * 
	 * @param factory
	 */
	public LinkReplacer(LinkResolverFactory<T> factory) {
		this.executor = Executors.newFixedThreadPool(10);
		this.factory = factory;
	}

	/**
	 * Replace the links in the content.
	 */
	public String replace(String content) throws InterruptedException, ExecutionException {

		if (isEmpty(content) || isEmpty(content)) {
			return content;
		}

		final List<Future<String>> renderedLinks = new ArrayList<>(15);
		int[][] segments = new int[5000][2];
		int s = 0;
		int nLink = 0;
		// First step: Determine all placeholders
		while (s != -1) {
			s = content.indexOf(START_TAG, s + START_TAG.length());
			int e = content.indexOf(END_TAG, s);
			if (s == -1) {
				break;
			}
			segments[nLink][0] = s;
			segments[nLink][1] = e + END_TAG.length();
			String link = content.substring(s + START_TAG.length(), e);
			renderedLinks.add(executor.submit(factory.createLinkResolver(link)));
			nLink++;
		}
		executor.shutdown();
		executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
		StringBuilder builder = new StringBuilder(content.length());
		int maxLinks = nLink;
		nLink = 0;
		int lastStart = 0;

		// Second step: Buildup the new content
		for (int i = 0; i < maxLinks; i++) {
			builder.append(content.substring(lastStart, segments[nLink][0]));
			builder.append(renderedLinks.get(nLink).get());
			lastStart = segments[nLink][1];
			nLink++;
		}
		builder.append(content.substring(lastStart, content.length()));

		return builder.toString();

	}

}
