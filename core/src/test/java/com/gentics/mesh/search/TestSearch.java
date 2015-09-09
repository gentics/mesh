package com.gentics.mesh.search;

import static org.elasticsearch.client.Requests.refreshRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.UUIDUtil;

public class TestSearch {

	private Client client;

	@Before
	public void setup() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
		Node node = NodeBuilder.nodeBuilder().node();
		client = node.client();

	}

	@Test
	public void testSearch() throws ElasticsearchException, IOException, InterruptedException {
		String uuidForIndex = UUIDUtil.randomUUID();

		//		setupIndex();

		storeDocument("de", uuidForIndex, "Deutsch");
		storeDocument("en", uuidForIndex, "English");

		refreshIndex();
		Thread.sleep(1000);
		getDocument(uuidForIndex, "de");
		search("English");
		search("Deutsch");
		search("dagdsgasdg");

		client.close();
	}

	//	private void setupIndex() {
	//		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate("node");
	//		createIndexRequestBuilder.execute().actionGet();
	//	}

	private void getDocument(String uuid, String language) {
		GetResponse getResponse = client.prepareGet("node", "node-" + language, uuid).setFields("uuid", "name", "fields.name", "language").execute()
				.actionGet();
		System.out.println("\nLoading object with uuid {" + uuid + "}");
		System.out.println("------------------------------");
		for (String field : getResponse.getFields().keySet()) {
			System.out.println(field + "=" + getResponse.getField(field).getValue());
		}
		System.out.println("------------------------------\n");
	}

	private void search(String name) throws JsonParseException, JsonMappingException, IOException {
		SearchRequestBuilder builder = client.prepareSearch();
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

		//SearchResponse response = builder.setQuery(QueryBuilders.termQuery("language", "en")).execute().actionGet();
		//.setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18)) // Filter
		//.setFrom(0).setSize(60).setExplain(true).execute().actionGet();
		//FilterBuilder filter = andFilter(rangeFilter("postDate").from("2010-03-01").to("2010-04-01"), prefixFilter("name.second", "ba"));
		//SearchResponse response = builder.setQuery(QueryBuilders.termQuery("name", "Deutsch")).setExplain(true).execute().actionGet();

		QueryBuilder qb = QueryBuilders.queryStringQuery(name);
		SearchResponse response = client.prepareSearch().setQuery(qb).setSize(1000).execute().actionGet();

		Iterator<SearchHit> hit_it = response.getHits().iterator();
		while (hit_it.hasNext()) {
			SearchHit hit = hit_it.next();
			ObjectMapper mapper = new ObjectMapper();
			Object json = mapper.readValue(hit.getSourceAsString(), Object.class);
			String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

			System.out.println(indented);
		}
		System.out.println("Search Result: " + response.getHits().totalHits());

	}

	private void refreshIndex() {
		client.admin().indices().refresh(refreshRequest()).actionGet();

	}

	private void storeDocument(String language, String uuidForIndex, String name) {
		Map<String, Object> json = new HashMap<>();
		json.put("uuid", UUIDUtil.randomUUID());
		json.put("created", new Date());
		json.put("edited", new Date());

		Map<String, String> editorMap = new HashMap<>();
		editorMap.put("firstname", "Joe");
		editorMap.put("lastname", "Doe");
		editorMap.put("emailadress", "j.doe@spam.gentics.com");
		editorMap.put("username", "joe1");
		json.put("creator", editorMap);
		json.put("editor", editorMap);

		json.put("language", language);
		json.put("name", name);

		List<String> tagNames = new ArrayList<>();
		tagNames.add("green");
		tagNames.add("blue");

		List<String> tagUuids = new ArrayList<>();
		tagUuids.add(UUIDUtil.randomUUID());
		tagUuids.add(UUIDUtil.randomUUID());

		Map<String, Object> tags = new HashMap<>();
		tags.put("name", tagNames);
		tags.put("uuid", tagUuids);
		json.put("tags", tags);

		Map<String, Object> fields = new HashMap<>();
		fields.put("name", "f_" + name);
		fields.put("number", 1);
		json.put("fields", fields);
		System.err.println(JsonUtil.toJson(json));
		IndexResponse indexResponse = client.prepareIndex("node", "node-" + language, uuidForIndex).setSource(json).execute().actionGet();
		System.out.println("Created document {" + uuidForIndex + "} with lang {" + language + "} name = " + indexResponse.isCreated());
	}

}
