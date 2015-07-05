import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
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

		IndexResponse response = client
				.prepareIndex("twitter", "tweet", "1")
				.setSource(
						jsonBuilder().startObject().field("user", "kimchy").field("postDate", new Date())
								.field("message", "trying out Elasticsearch").endObject()).execute().actionGet();
		
		System.out.println(response.getIndex());
	}
	
}
