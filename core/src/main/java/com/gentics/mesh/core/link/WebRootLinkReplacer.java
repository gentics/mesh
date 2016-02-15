package com.gentics.mesh.core.link;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * This class will resolve mesh link placeholders.
 */
@Component
public class WebRootLinkReplacer {

	private static final String START_TAG = "{{mesh.link(";
	private static final String END_TAG = ")}}";

	private static WebRootLinkReplacer instance;

	private static final Logger log = LoggerFactory.getLogger(WebRootLinkReplacer.class);

	@PostConstruct
	public void setup() {
		WebRootLinkReplacer.instance = this;
	}

	public static WebRootLinkReplacer getInstance() {
		return instance;
	}

	/**
	 * Replace the links in the content.
	 * @param content content containing links to replace
	 * @param type replacing type
	 * @param projectName project name (used for 404 links)
	 * @param languageTags optional language tags
	 * @return content with links (probably) replaced
	 */
	public String replace(String content, Type type, String projectName, List<String> languageTags) {
		if (isEmpty(content) || type == Type.OFF || type == null) {
			return content;
		}

		List<Observable<String>> segments = new ArrayList<>();
		int pos = 0;
		int lastPos = 0;
		int length = content.length();

		// 1. Tokenize the content
		while (lastPos < length) {
			pos = content.indexOf(START_TAG, lastPos);
			if (pos == -1) {
				// add last string segment
				if (lastPos < length) {
					segments.add(Observable.just(content.substring(lastPos)));
				}
				break;
			}
			int endPos = content.indexOf(END_TAG, pos);
			if (endPos == -1) {
				// add last string segment
				if (lastPos < length) {
					segments.add(Observable.just(content.substring(lastPos)));
				}
				break;
			}

			// add intermediate string segment
			if (lastPos < pos) {
				segments.add(Observable.just(content.substring(lastPos, pos)));
			}

			// 2. Parse the link and invoke resolving
			String link = content.substring(pos + START_TAG.length(), endPos);
			// Strip away the quotes. We only care about the argument values
			// double quotes may be escaped
			link = link.replaceAll("\\\\\"", "");
			link = link.replaceAll("'", "");
			link = link.replaceAll("\"", "");
			String[] linkArguments = link.split(",");
			if (linkArguments.length == 2) {
				segments.add(resolve(linkArguments[0], type, projectName, linkArguments[1].trim()));
			} else if (languageTags != null) {
				segments.add(resolve(linkArguments[0], type, projectName, languageTags.toArray(new String[languageTags.size()])));
			} else {
				segments.add(resolve(linkArguments[0], type, projectName));
			}

			lastPos = endPos + END_TAG.length();
		}

		// 3.: Buildup the new content
		StringBuilder renderedContent = new StringBuilder(length);
		segments.stream().forEachOrdered(obs -> renderedContent.append(obs.toBlocking().first()));

		return renderedContent.toString();
	}

	/**
	 * Resolve the link to the node with uuid (in the given language) into an observable
	 * @param uuid target uuid
	 * @param type link type
	 * @param projectName project name (which is used for 404 links)
	 * @param languageTags optional language tags
	 * @return observable of the rendered link
	 */
	public Observable<String> resolve(String uuid, Type type, String projectName, String... languageTags) {
		// Get rid of additional whitespaces
		uuid = uuid.trim();
		Node node = MeshRoot.getInstance().getNodeRoot().findByUuid(uuid).toBlocking().single();

		// check for null
		if (node == null) {
			if (log.isDebugEnabled()) {
				log.debug("Could not resolve link to '" + uuid + "', target node could not be found");
			}
			switch (type) {
			case SHORT:
				return Observable.just("/error/404");
			case MEDIUM:
				return Observable.just("/" + projectName + "/error/404");
			case FULL:
				return Observable
						.just(RouterStorage.DEFAULT_API_MOUNTPOINT + "/" + projectName + "/webroot/error/404");
			default:
				return Observable.error(new Exception("Cannot render link with type " + type));
			}
		}
		return resolve(node, type, languageTags);
	}

	/**
	 * Resolve the link to the given node
	 * @param node target node
	 * @param type link type
	 * @param languageTag target language
	 * @return observable of the rendered link
	 */
	public Observable<String> resolve(Node node, Type type, String...languageTag) {
		if (languageTag == null || languageTag.length == 0) {
			languageTag = new String[] { Mesh.mesh().getOptions().getDefaultLanguage() };

			if (log.isDebugEnabled()) {
				log.debug("Fallback to default language " + languageTag);
			}
		}
		try {
			if (log.isDebugEnabled()) {
				log.debug("Resolving link to " + node.getUuid() + " in language " + languageTag + " with type " + type);
			}
			switch (type) {
			case SHORT:
				return node.getPath(languageTag).onErrorReturn(e -> "/error/404");
			case MEDIUM:
				return node.getPath(languageTag).onErrorReturn(e -> "/error/404")
						.map(path -> "/" + node.getProject().getName() + path);
			case FULL:
				return node.getPath(languageTag).onErrorReturn(e -> "/error/404")
						.map(path -> RouterStorage.DEFAULT_API_MOUNTPOINT + "/" + node.getProject().getName()
								+ "/webroot" + path);
			default:
				return Observable.error(new Exception("Cannot render link with type " + type));
			}
		} catch (UnsupportedEncodingException e) {
			return Observable.error(e);
		}
	}

	/**
	 * Link Replacing type
	 */
	public static enum Type {
		/**
		 * No link replacing
		 */
		OFF,

		/**
		 * Link replacing without the API prefix and without the project name
		 */
		SHORT,

		/**
		 * Link replacing without the API prefix, but with the project name
		 */
		MEDIUM,

		/**
		 * Link replacing with API prefix and project name
		 */
		FULL
	}
}
