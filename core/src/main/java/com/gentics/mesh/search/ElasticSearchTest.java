package com.gentics.mesh.search;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.test.SpringElasticSearchTestConfiguration;

@ContextConfiguration(classes = { SpringElasticSearchTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ElasticSearchTest {

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Test
	public void testElasticSearchJson() throws JSONException {

		QueryBuilder qb = QueryBuilders.queryStringQuery("Großraumflugzeug");
		System.out.println(qb.toString());

		SearchSourceBuilder b = new SearchSourceBuilder();
		QuerySourceBuilder qbs = new QuerySourceBuilder();
		//		SearchRequestBuilder builder = elasticSearchNode.client().prepareSearch();
		//		builder.setSize(Integer.MAX_VALUE);
		//		builder.setFrom(0);
		//		builder.setExtraSource(getDummyRequest());
		//		System.out.println(builder.toString());

		
//		System.out.println(queryStringObject);

	}

	public String getDummyRequest() {
		String json = "{";
		json += "				\"sort\" : {\n";
		json += "			      \"created\" : {\"order\" : \"asc\"}\n";
		json += "			    },\n";
		json += "			    \"query\":{\n";
		json += "			        \"bool\" : {\n";
		json += "			            \"must\" : {\n";
		json += "			                \"term\" : { \"schema.name\" : \"content\" }\n";
		json += "			            }\n";
		json += "			        }\n";
		json += "			    }\n";
		json += "			}\n";

		return json;
	}
}
