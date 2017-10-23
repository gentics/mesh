package com.gentics.mesh.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.rest.AbstractRestChannel;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;

/**
 * Wrapper for an elasticsearch rest channel.
 */
public class MeshRestChannel extends AbstractRestChannel implements RestChannel {

	/**
	 * Mocked request.
	 */
	public static RestRequest dummyRequest = new RestRequest() {

		@Override
		public String param(String key, String defaultValue) {
			return null;
		}

		@Override
		public Method method() {
			return Method.POST;
		}

		@Override
		public String uri() {
			return null;
		}

		@Override
		public String rawPath() {
			return null;
		}

		@Override
		public boolean hasContent() {
			return false;
		}

		@Override
		public BytesReference content() {
			return null;
		}

		@Override
		public String header(String name) {
			return null;
		}

		@Override
		public Iterable<Entry<String, String>> headers() {
			List<Entry<String, String>> entry = Collections.emptyList();
			return entry;
		}

		@Override
		public boolean hasParam(String key) {
			return false;
		}

		@Override
		public String param(String key) {
			return null;
		}

		@Override
		public Map<String, String> params() {
			return Collections.emptyMap();
		}

	};

	public MeshRestChannel(RestRequest request, boolean detailedErrorsEnabled) {
		super(dummyRequest, detailedErrorsEnabled);
	}

	@Override
	public RestRequest request() {
		return dummyRequest;
	}

	@Override
	public void sendResponse(RestResponse response) {
		// NA - We fetch the content manually and return it with our own methods
	}

}
