package com.gentics.mesh.plugin.search.transformer;

import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.plugin.search.extension.HitTransformer;
import com.gentics.mesh.plugin.search.util.MiscUtils;
import com.gentics.mesh.plugin.search.util.JsonUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

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

		Optional<JsonArray> highlight = hit.getJsonObject("highlight", emptyObject).stream()
			.map(o -> o instanceof JsonArray ? (JsonArray) o : emptyArray)
			.filter(o -> !o.isEmpty())
			.findFirst();

		if (highlight.isPresent()) {
			String excerpt = highlight.get().getString(0);

			if (!StringUtils.isBlank(excerpt)) {
				excerpt = excerpt.replaceAll("%hb%", "<strong>")
					.replaceAll("%he%", "</strong>")
					.replaceAll("\\s+", " ");

				ret.put("excerpt", Jsoup.parse(excerpt).text());
			}
		}

		if (node != null) {
			ret.put("additional", node.getJsonObject("fields", emptyObject));
		}

		return ret;
	}
}
