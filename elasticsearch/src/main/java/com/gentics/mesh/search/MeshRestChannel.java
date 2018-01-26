package com.gentics.mesh.search;

/**
 * Wrapper for an elasticsearch rest channel.
 */
public class MeshRestChannel extends AbstractRestChannel implements RestChannel {

	/**
	 * Mocked request.
	 */
	public static RestRequest dummyRequest = new RestRequest(null, null,  null) {

		@Override
		public Method method() {
			return Method.POST;
		}

		@Override
		public String uri() {
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