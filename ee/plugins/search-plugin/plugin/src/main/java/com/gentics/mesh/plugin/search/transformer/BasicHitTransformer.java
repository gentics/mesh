package com.gentics.mesh.plugin.search.transformer;

import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.plugin.search.extension.HitTransformer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A very simple transformation for search hits.
 *
 * <p>
 *     All entries from the <code>_source</code> field of the Elasticsearch result, as well as all fields from
 *     the optional GraphQL query are added to the result, under the names <code>raw</code> and <code>additional</code>
 *     respectively.
 * </p>
 *
 * <p>
 *     When highlights are present in the Elasticsearch result, the first non empty highlight is also added under
 *     the name <code>excerpt</code>.
 * </p>
 */
public class BasicHitTransformer implements HitTransformer {

	public BasicHitTransformer() {
	}

	@Override
	public JsonObject transform(JsonObject hit, JsonObject node, String contextProject, BranchResponse branch) {
		JsonObject emptyObject = new JsonObject();
		JsonArray emptyArray = new JsonArray();
		JsonObject ret = new JsonObject();

		ret.put("raw", hit.getJsonObject("_source", emptyObject));

		Map<String, List<String>> highlights = hit.getJsonObject("highlight", emptyObject).stream()
			.filter(e -> e != null && e.getValue() instanceof JsonArray && !((JsonArray) e.getValue()).isEmpty())
			.map(e -> Pair.of(e.getKey(), transformHighlightFragments((JsonArray) e.getValue())))
			.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		ret.put("highlights", highlights)
			.put("excerpt", highlights.values().stream().flatMap(List::stream).collect(Collectors.joining(" ... ")));

		if (node != null) {
			ret.put("additional", node.getJsonObject("fields", emptyObject));
			// TODO: above line is to allow backward compatibility
			JsonObject tmp = node.copy();
			tmp.remove("fields");
			ret.put("additionalDetails", tmp);
		}

		return ret;
	}

	/**
	 * Creates a list of sanitized strings from the given JsonArray.
	 *
	 * <p>
	 *     This method performs the following transformations on each entry:
	 *     <ul>
	 *         <li>Replace {@code %hb%} with {@code &lt;strong&gt;}</li>
	 *         <li>Replace {@code %he%} with {@code &lt;/strong&gt;}</li>
	 *         <li>Replace all consecutive whitespace characters with a single space</li>
	 *         <lil>Use {@link Jsoup#parse(String)} and {@link Document#text()} to remove markup</lil>
	 *     </ul>
	 * </p>
	 * @param fragments The highlight fragments from the search result.
	 * @return The highlights as a list of sanitized strings.
	 */
	private List<String> transformHighlightFragments(JsonArray fragments) {
		return fragments.stream()
			.filter(Objects::nonNull)
			.map(Object::toString)
			.map(s -> Jsoup.parse(s).text())
			.map(s -> s.replaceAll("%hb%", "<strong>")
				.replaceAll("%he%", "</strong>")
				.replaceAll("\\s+", " "))
			.collect(Collectors.toList());
	}
}
