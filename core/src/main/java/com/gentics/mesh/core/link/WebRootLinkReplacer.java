package com.gentics.mesh.core.link;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;

import rx.Observable;

/**
 * This class will resolve mesh link placeholders.
 */
@Component
public class WebRootLinkReplacer {

	private static final String START_TAG = "{{mesh.link(";
	private static final String END_TAG = ")}}";

	/**
	 * Replace the links in the content.
	 */
	public String replace(String content) throws InterruptedException, ExecutionException {

		if (isEmpty(content)) {
			return content;
		}

		StringBuilder builder = new StringBuilder(content.length());
		final List<Observable<String>> renderedLinks = new ArrayList<>();
		int[][] segments = new int[5000][2];
		int s = 0;
		int nLink = 0;
		// 1. Tokenize the content
		while (s != -1) {
			s = content.indexOf(START_TAG, s + START_TAG.length());
			int e = content.indexOf(END_TAG, s);
			if (s == -1) {
				break;
			}
			segments[nLink][0] = s;
			segments[nLink][1] = e + END_TAG.length();

			// 2. Parse the link and invoke resolving
			String link = content.substring(s + START_TAG.length(), e);
			link = link.replaceAll("\"", "");
			String[] linkArguments = link.split(",");
			if (linkArguments.length == 2) {
				renderedLinks.add(resolve(linkArguments[0], linkArguments[1]));
			} else {
				renderedLinks.add(resolve(linkArguments[0], null));
			}
			nLink++;
		}
		int maxLinks = nLink;
		nLink = 0;
		int lastStart = 0;

		// 3.: Buildup the new content
		for (int i = 0; i < maxLinks; i++) {
			builder.append(content.substring(lastStart, segments[nLink][0]));
			builder.append(renderedLinks.get(nLink).toBlocking().first());
			lastStart = segments[nLink][1];
			nLink++;
		}
		builder.append(content.substring(lastStart, content.length()));
		return builder.toString();

	}

	private Observable<String> resolve(String uuid, String languageTag) {
		System.out.println("Link:" + uuid + " lang " + languageTag);
		return Observable.just("test");
	}
}
