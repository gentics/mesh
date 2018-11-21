package com.gentics.mesh.search.impl;

import com.gentics.elasticsearch.client.okhttp.ElasticsearchOkClient;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SearchClient extends ElasticsearchOkClient<JsonObject> {

	public SearchClient(String scheme, String hostname, int port) {
		super(scheme, hostname, port);
		setConverterFunction(JsonObject::new);
	}

	/**
	 * Check if ingest processors exist on the elasticsearch nodes.
	 *
	 * @param processorName one or multiple processor names
	 * @return true if all processors where found in the list of ingest processors
	 */
	public Single<Boolean> hasIngestProcessor(String... processorName) {
		Set<String> processorNames = new HashSet<>(Arrays.asList(processorName));
		return this.nodesInfo().async()
			.filter(response -> response.containsKey("nodes"))
			.map(response -> response.getJsonObject("nodes"))
			// we need to find one node which contains
			.map(nodes ->
				nodes.stream()
					.map(entry -> nodes.getJsonObject(entry.getKey()))
					.filter(node -> node.containsKey("ingest"))
					.map(node -> node.getJsonObject("ingest"))
					.filter(ingest -> ingest.containsKey("processors"))
					.flatMap(ingest -> {
						JsonArray processors = ingest.getJsonArray("processors");
						return IntStream.range(0, processors.size())
							.mapToObj(processors::getJsonObject)
							.filter(typeObj -> typeObj.containsKey("type"))
							.map(typeObj -> typeObj.getString("type"));
					})
					.collect(Collectors.toSet())
					.containsAll(processorNames)
			).toSingle();
	}

	/**
	 * Invoke a scroll the request.
	 * 
	 * @param scrollId
	 * @param scrollTimeout
	 * @return
	 */
	public RequestBuilder<JsonObject> scroll(String scrollId, String scrollTimeout) {
		JsonObject request = new JsonObject();
		request.put("scroll", scrollTimeout);
		request.put("scroll_id", scrollId);
		return postBuilder("_search/scroll", request);
	}

}
