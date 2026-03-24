package com.gentics.mesh.test.openapi;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openapitools.client.ApiCallback;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Pair;
import org.openapitools.client.ProgressRequestBody;

import com.gentics.mesh.http.MeshHeaders;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.http.HttpMethod;

/**
 * API client with an access to the request builder
 */
public class UpgradedApiClient extends ApiClient {

	private boolean disableAnonymousAccess;

	public UpgradedApiClient() {
		super();
	}

	public UpgradedApiClient(OkHttpClient client) {
		super(client);
	}

	@SuppressWarnings("rawtypes")
	public Request.Builder buildRequestBuilder(String baseUrl, String path, String method, List<Pair> queryParams,
			List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams,
			Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback)
			throws ApiException {
		final String url = buildUrl(baseUrl, path, queryParams, collectionQueryParams);

        // prepare HTTP request body
        RequestBody reqBody;
        String contentType = headerParams.get("Content-Type");
        String contentTypePure = contentType;
        if (contentTypePure != null && contentTypePure.contains(";")) {
            contentTypePure = contentType.substring(0, contentType.indexOf(";"));
        }
        if (!HttpMethod.permitsRequestBody(method)) {
            reqBody = null;
        } else if ("application/x-www-form-urlencoded".equals(contentTypePure)) {
            reqBody = buildRequestBodyFormEncoding(formParams);
        } else if ("multipart/form-data".equals(contentTypePure)) {
            reqBody = buildRequestBodyMultipart(formParams);
        } else if (body == null) {
            if ("DELETE".equals(method)) {
                // allow calling DELETE without sending a request body
                reqBody = null;
            } else {
                // use an empty request body (for POST, PUT and PATCH)
                reqBody = RequestBody.create("", contentType == null ? null : MediaType.parse(contentType));
            }
        } else {
            reqBody = serialize(body, contentType);
        }

        List<Pair> updatedQueryParams = new ArrayList<>(queryParams);

        // update parameters with authentication settings
        updateParamsForAuth(authNames, updatedQueryParams, headerParams, cookieParams, requestBodyToString(reqBody), method, URI.create(url));

        final Request.Builder reqBuilder = new Request.Builder().url(buildUrl(baseUrl, path, updatedQueryParams, collectionQueryParams));
        processHeaderParams(headerParams, reqBuilder);
        processCookieParams(cookieParams, reqBuilder);

        // Associate callback with request (if not null) so interceptor can
        // access it when creating ProgressResponseBody
        reqBuilder.tag(callback);

        if (callback != null && reqBody != null) {
            ProgressRequestBody progressRequestBody = new ProgressRequestBody(reqBody, callback);
            reqBuilder.method(method, progressRequestBody);
        } else {
            reqBuilder.method(method, reqBody);
        }

        if (disableAnonymousAccess) {
			headerParams.put(MeshHeaders.ANONYMOUS_AUTHENTICATION, "disable");
		}
        return reqBuilder;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Request buildRequest(String baseUrl, String path, String method, List<Pair> queryParams,
			List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams,
			Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback)
			throws ApiException {
		return buildRequestBuilder(baseUrl, path, method, queryParams, collectionQueryParams, body, headerParams, cookieParams, formParams, authNames, callback).build();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public UpgradedCall buildCall(String baseUrl, String path, String method, List<Pair> queryParams,
			List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams,
			Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback)
			throws ApiException {
		Request.Builder request = buildRequestBuilder(baseUrl, path, method, queryParams, collectionQueryParams, body, headerParams, cookieParams, formParams, authNames, callback);

        return new UpgradedCall(request, httpClient);
	}

	public void disableAnonymousAccess(boolean b) {
		this.disableAnonymousAccess = b;
	}
}
