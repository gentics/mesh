package com.gentics.mesh.example;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class MiscExamples extends AbstractExamples {

	public LoginRequest getLoginRequest() {
		LoginRequest request = new LoginRequest();
		request.setUsername("admin");
		request.setPassword("finger");
		return request;
	}

	public SearchStatusResponse searchStatusJson() {
		SearchStatusResponse status = new SearchStatusResponse();
		status.setAvailable(true);

		Map<String, Object> metrics = new HashMap<>();
		Stream.of(
			"tagfamily",
			"schema",
			"node",
			"role",
			"microschema",
			"project",
			"tag",
			"user",
			"group"
		).forEach(key -> metrics.put(key, exampleMetric()));
		status.setMetrics(metrics);

		return status;
	}

	private JsonObject exampleMetric() {
		JsonObject object = new JsonObject();
		Stream.of(
			"insert.total",
			"delete.pending",
			"insert.pending",
			"update.pending",
			"delete.total",
			"update.total"
		).forEach(key -> object.put(key, 0));
		return object;
	}

	public GenericMessageResponse createMessageResponse() {
		// TODO allow for custom messages
		GenericMessageResponse message = new GenericMessageResponse();
		message.setMessage("I18n message");
		return message;
	}

	public JSONObject getSearchQueryExample() {
		JSONObject node = new JSONObject();
		try {
			JSONObject query = new JSONObject();
			JSONObject queryString = new JSONObject();
			queryString.put("query", "some name");
			query.put("query_string", queryString);
			node.put("query", query);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return node;
	}

	public JSONObject getAuthTokenResponse() {
		JSONObject node = new JSONObject();
		try {
			node.put("token",
				"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyVXVpZCI6IlVVSURPRlVTRVIxIiwiZXhwIjoxNDY5MTE3MjQ3LCJpYXQiOjE0NjkxMTM2NDd9.i1u4RMs4K7zBkGhmcpp1P79Wpz2UQYJkZKJTVdFp_iU=");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return node;
	}

	public JsonObject createSearchResponse() {
		return new JsonObject();
	}

}
