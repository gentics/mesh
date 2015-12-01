package com.gentics.mesh.core.link;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;

import rx.Observable;

/**
 * This class will resolve mesh link placeholders.
 */
@Component
public class WebRootLinkReplacer {

	private static final String START_TAG = "{{mesh.link(";
	private static final String END_TAG = ")}}";

	private static WebRootLinkReplacer instance;

	@PostConstruct
	public void setup() {
		WebRootLinkReplacer.instance = this;
	}

	public static WebRootLinkReplacer getInstance() {
		return instance;
	}

	/**
	 * Replace the links in the content.
	 */
	public String replace(String content) {

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
			int r = 0;
			if (s != 0) {
				r = s + START_TAG.length();
			}
			s = content.indexOf(START_TAG, r);
			if (s == -1) {
				break;
			}
			int e = content.indexOf(END_TAG, s);
			segments[nLink][0] = s;
			segments[nLink][1] = e + END_TAG.length();

			// 2. Parse the link and invoke resolving
			String link = content.substring(s + START_TAG.length(), e);
			// Strip away the quotes. We only care about the argument values
			link = link.replaceAll("'", "");
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
		if (languageTag == null) {
			languageTag = Mesh.mesh().getOptions().getDefaultLanguage();
		}
		// Get rid of additional whitespaces
		uuid = uuid.trim();
		languageTag = languageTag.trim();
		Node node = MeshRoot.getInstance().getNodeRoot().findByUuidBlocking(uuid);
		Language language = MeshRoot.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
		return Observable.just("/webroot" + node.getPath(language));
	}
}
