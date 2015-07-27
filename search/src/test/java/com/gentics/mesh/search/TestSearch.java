package com.gentics.mesh.search;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.FilterBuilders.andFilter;
import static org.elasticsearch.index.query.FilterBuilders.prefixFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.Test;

public class TestSearch {

	@Test
	public void testSearch() throws ElasticsearchException, IOException {
		Node node = NodeBuilder.nodeBuilder().node();
		Client client = node.client();

		/*
		 * 
		 * SearchResponse response = client.prepareSearch("index1", "index2") .setTypes("type1", "type2") .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		 * .setQuery(QueryBuilders.termQuery("multi", "test")) // Query .setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18)) // Filter
		 * .setFrom(0).setSize(60).setExplain(true) .execute() .actionGet();
		 */

		Map<String, Object> json = new HashMap<String, Object>();
		json.put("user", "kimchy");
		json.put("postDate", new Date());
		json.put("message", "trying out Elasticsearch");

		// JsonUtil.getMapper().writeValueAsBytes(json)

		IndexResponse response2 = client
				.prepareIndex("twitter", "tweet", "1")
				.setSource(
						jsonBuilder().startObject().field("user", "kimchy").field("postDate", new Date())
								.field("message", "trying out Elasticsearch").endObject()).execute().actionGet();
		System.out.println(response2.getIndex());

		SearchResponse response = client.prepareSearch("twitter").setTypes("type1", "type2").setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setQuery(QueryBuilders.termQuery("multi", "test")) // Query
				.setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18)) // Filter
				.setFrom(0).setSize(60).setExplain(true).execute().actionGet();

		FilterBuilder filter = andFilter(rangeFilter("postDate").from("2010-03-01").to("2010-04-01"), prefixFilter("name.second", "ba"));

		System.out.println(response.getHits().getTotalHits());
	}

}
