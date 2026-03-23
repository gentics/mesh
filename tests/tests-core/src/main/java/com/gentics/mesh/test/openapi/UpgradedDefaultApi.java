package com.gentics.mesh.test.openapi;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openapitools.client.ApiCallback;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.Pair;
import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.NodeListResponse;
import org.openapitools.client.model.NodeResponse;

import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;

/**
 * An upgrade to the generated DefaultAPI, overriding its generation problems 
 */
@SuppressWarnings("rawtypes")
public class UpgradedDefaultApi extends DefaultApi {

	public UpgradedDefaultApi() {
		super();
	}

	public UpgradedDefaultApi(ApiClient apiClient) {
		super(apiClient);
	}

    /**
     * Build call for apiV2ProjectNodesNodeUuidPost
     * @param nodeUuid Uuid of the node (required)
     * @param project  (required)
     * @param body Json body
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> New or updated node. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> A conflict has been detected. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2ProjectNodesNodeUuidPostCall(@jakarta.annotation.Nonnull String nodeUuid, @jakarta.annotation.Nonnull String project, JsonObject body, final ApiCallback _callback) throws ApiException {
        String basePath = null;
        // Operation Servers
        String[] localBasePaths = new String[] {  };

        // Determine Base Path to Use
        if (getCustomBaseUrl() != null){
            basePath = getCustomBaseUrl();
        } else if ( localBasePaths.length > 0 ) {
            basePath = localBasePaths[getHostIndex()];
        } else {
            basePath = null;
        }

        Object localVarPostBody = Optional.ofNullable(body).map(json -> JsonParser.parseString(body.toString()).getAsJsonObject());

        // create path and map variables
        String localVarPath = "/api/v2/{project}/nodes/{nodeUuid}"
            .replace("{" + "nodeUuid" + "}", getApiClient().escapeString(nodeUuid.toString()))
            .replace("{" + "project" + "}", getApiClient().escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json",
            "*/*"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    private okhttp3.Call apiV2ProjectNodesNodeUuidPostValidateBeforeCall(@jakarta.annotation.Nonnull String nodeUuid, @jakarta.annotation.Nonnull String project, JsonObject body, final ApiCallback _callback) throws ApiException {
        // verify the required parameter 'nodeUuid' is set
        if (nodeUuid == null) {
            throw new ApiException("Missing the required parameter 'nodeUuid' when calling apiV2ProjectNodesNodeUuidPost(Async)");
        }

        // verify the required parameter 'project' is set
        if (project == null) {
            throw new ApiException("Missing the required parameter 'project' when calling apiV2ProjectNodesNodeUuidPost(Async)");
        }

        return apiV2ProjectNodesNodeUuidPostCall(nodeUuid, project, body, _callback);
    }

    /**
     * 
     * 
     * @param nodeUuid Uuid of the node (required)
     * @param project  (required)
     * @return NodeResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> New or updated node. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> A conflict has been detected. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public NodeResponse apiV2ProjectNodesNodeUuidPost(@jakarta.annotation.Nonnull String nodeUuid, @jakarta.annotation.Nonnull String project, JsonObject body) throws ApiException {
        ApiResponse<NodeResponse> localVarResp = apiV2ProjectNodesNodeUuidPostWithHttpInfo(nodeUuid, project, body);
        return localVarResp.getData();
    }

    /**
     * 
     * 
     * @param nodeUuid Uuid of the node (required)
     * @param project  (required)
     * @return ApiResponse&lt;NodeResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> New or updated node. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> A conflict has been detected. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<NodeResponse> apiV2ProjectNodesNodeUuidPostWithHttpInfo(@jakarta.annotation.Nonnull String nodeUuid, @jakarta.annotation.Nonnull String project, JsonObject body) throws ApiException {
        okhttp3.Call localVarCall = apiV2ProjectNodesNodeUuidPostValidateBeforeCall(nodeUuid, project, body, null);
        Type localVarReturnType = new TypeToken<NodeResponse>(){}.getType();
        return getApiClient().execute(localVarCall, localVarReturnType);
    }

    /**
     *  (asynchronously)
     * 
     * @param nodeUuid Uuid of the node (required)
     * @param project  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> New or updated node. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> A conflict has been detected. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2ProjectNodesNodeUuidPostAsync(@jakarta.annotation.Nonnull String nodeUuid, @jakarta.annotation.Nonnull String project, JsonObject body, final ApiCallback<NodeResponse> _callback) throws ApiException {
        okhttp3.Call localVarCall = apiV2ProjectNodesNodeUuidPostValidateBeforeCall(nodeUuid, project, body, _callback);
        Type localVarReturnType = new TypeToken<NodeResponse>(){}.getType();
        getApiClient().executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }

    // Missing JSON body fix
    /**
     * Build call for apiV2SearchNodesPost
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param branch Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted. (optional)
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result for nodes </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2SearchNodesPostCall(String jsonbody, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String branch, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
        String basePath = null;
        // Operation Servers
        String[] localBasePaths = new String[] {  };

        // Determine Base Path to Use
        if (getCustomBaseUrl() != null){
            basePath = getCustomBaseUrl();
        } else if ( localBasePaths.length > 0 ) {
            basePath = localBasePaths[getHostIndex()];
        } else {
            basePath = null;
        }

        Object localVarPostBody = JsonParser.parseString(jsonbody);

        // create path and map variables
        String localVarPath = "/api/v2/search/nodes";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("wait", wait));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("perPage", perPage));
        }

        if (sortBy != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("sortBy", sortBy));
        }

        if (etag != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("etag", etag));
        }

        if (page != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("page", page));
        }

        if (fields != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("fields", fields));
        }

        if (branch != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("branch", branch));
        }

        if (order != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call apiV2SearchNodesPostValidateBeforeCall(String jsonBody, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String branch, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
        return apiV2SearchNodesPostCall(jsonBody, wait, perPage, sortBy, etag, page, fields, branch, order, _callback);

    }

    /**
     * 
     * Invoke a search query for nodes and return a paged list response.
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param branch Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted. (optional)
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @return NodeListResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result for nodes </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public NodeListResponse apiV2SearchNodesPost(String jsonBody, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String branch, @jakarta.annotation.Nullable String order) throws ApiException {
        ApiResponse<NodeListResponse> localVarResp = apiV2SearchNodesPostWithHttpInfo(jsonBody, wait, perPage, sortBy, etag, page, fields, branch, order);
        return localVarResp.getData();
    }

    /**
     * 
     * Invoke a search query for nodes and return a paged list response.
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param branch Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted. (optional)
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @return ApiResponse&lt;NodeListResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result for nodes </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<NodeListResponse> apiV2SearchNodesPostWithHttpInfo(String jsonBody, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String branch, @jakarta.annotation.Nullable String order) throws ApiException {
        okhttp3.Call localVarCall = apiV2SearchNodesPostValidateBeforeCall(jsonBody, wait, perPage, sortBy, etag, page, fields, branch, order, null);
        Type localVarReturnType = new TypeToken<NodeListResponse>(){}.getType();
        return getApiClient().execute(localVarCall, localVarReturnType);
    }

    /**
     *  (asynchronously)
     * Invoke a search query for nodes and return a paged list response.
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param branch Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted. (optional)
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result for nodes </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2SearchNodesPostAsync(String jsonBody, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String branch, @jakarta.annotation.Nullable String order, final ApiCallback<NodeListResponse> _callback) throws ApiException {

        okhttp3.Call localVarCall = apiV2SearchNodesPostValidateBeforeCall(jsonBody, wait, perPage, sortBy, etag, page, fields, branch, order, _callback);
        Type localVarReturnType = new TypeToken<NodeListResponse>(){}.getType();
        getApiClient().executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }

    /**
     * Build call for apiV2RawSearchNodesPost
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2RawSearchNodesPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
        String basePath = null;
        // Operation Servers
        String[] localBasePaths = new String[] {  };

        // Determine Base Path to Use
        if (getCustomBaseUrl() != null){
            basePath = getCustomBaseUrl();
        } else if ( localBasePaths.length > 0 ) {
            basePath = localBasePaths[getHostIndex()];
        } else {
            basePath = null;
        }

        Object localVarPostBody = JsonParser.parseString(json);

        // create path and map variables
        String localVarPath = "/api/v2/rawSearch/nodes";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call apiV2RawSearchNodesPostValidateBeforeCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
        return apiV2RawSearchNodesPostCall(json, wait, _callback);
    }

    /**
     * 
     * Invoke a search query for nodes and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @return Object
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public Object apiV2RawSearchNodesPost(String json, @jakarta.annotation.Nullable Boolean wait) throws ApiException {
        ApiResponse<Object> localVarResp = apiV2RawSearchNodesPostWithHttpInfo(json, wait);
        return localVarResp.getData();
    }

    /**
     * 
     * Invoke a search query for nodes and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @return ApiResponse&lt;Object&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Object> apiV2RawSearchNodesPostWithHttpInfo(String json, @jakarta.annotation.Nullable Boolean wait) throws ApiException {
        okhttp3.Call localVarCall = apiV2RawSearchNodesPostValidateBeforeCall(json, wait, null);
        Type localVarReturnType = new TypeToken<Object>(){}.getType();
        return getApiClient().execute(localVarCall, localVarReturnType);
    }

    /**
     *  (asynchronously)
     * Invoke a search query for nodes and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2RawSearchNodesPostAsync(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback<Object> _callback) throws ApiException {

        okhttp3.Call localVarCall = apiV2RawSearchNodesPostValidateBeforeCall(json, wait, _callback);
        Type localVarReturnType = new TypeToken<Object>(){}.getType();
        getApiClient().executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }

    /**
     * Build call for apiV2ProjectRawSearchNodesPost
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2ProjectRawSearchNodesPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
        String basePath = null;
        // Operation Servers
        String[] localBasePaths = new String[] {  };

        // Determine Base Path to Use
        if (getCustomBaseUrl() != null){
            basePath = getCustomBaseUrl();
        } else if ( localBasePaths.length > 0 ) {
            basePath = localBasePaths[getHostIndex()];
        } else {
            basePath = null;
        }

        Object localVarPostBody = JsonParser.parseString(json);

        // create path and map variables
        String localVarPath = "/api/v2/{project}/rawSearch/nodes"
            .replace("{" + "project" + "}", getApiClient().escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call apiV2ProjectRawSearchNodesPostValidateBeforeCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
        // verify the required parameter 'project' is set
        if (project == null) {
            throw new ApiException("Missing the required parameter 'project' when calling apiV2ProjectRawSearchNodesPost(Async)");
        }

        return apiV2ProjectRawSearchNodesPostCall(json, project, wait, _callback);

    }

    /**
     * 
     * Invoke a search query for nodes and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @return Object
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public Object apiV2ProjectRawSearchNodesPost(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait) throws ApiException {
        ApiResponse<Object> localVarResp = apiV2ProjectRawSearchNodesPostWithHttpInfo(json, project, wait);
        return localVarResp.getData();
    }

    /**
     * 
     * Invoke a search query for nodes and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @return ApiResponse&lt;Object&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Object> apiV2ProjectRawSearchNodesPostWithHttpInfo(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait) throws ApiException {
        okhttp3.Call localVarCall = apiV2ProjectRawSearchNodesPostValidateBeforeCall(json, project, wait, null);
        Type localVarReturnType = new TypeToken<Object>(){}.getType();
        return getApiClient().execute(localVarCall, localVarReturnType);
    }

    /**
     *  (asynchronously)
     * Invoke a search query for nodes and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Raw search response. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2ProjectRawSearchNodesPostAsync(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, final ApiCallback<Object> _callback) throws ApiException {

        okhttp3.Call localVarCall = apiV2ProjectRawSearchNodesPostValidateBeforeCall(json, project, wait, _callback);
        Type localVarReturnType = new TypeToken<Object>(){}.getType();
        getApiClient().executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }

    /**
     * Build call for apiV2ProjectSearchNodesPost
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result list. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2ProjectSearchNodesPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
        String basePath = null;
        // Operation Servers
        String[] localBasePaths = new String[] {  };

        // Determine Base Path to Use
        if (getCustomBaseUrl() != null){
            basePath = getCustomBaseUrl();
        } else if ( localBasePaths.length > 0 ) {
            basePath = localBasePaths[getHostIndex()];
        } else {
            basePath = null;
        }

        Object localVarPostBody = JsonParser.parseString(json);

        // create path and map variables
        String localVarPath = "/api/v2/{project}/search/nodes"
            .replace("{" + "project" + "}", getApiClient().escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("wait", wait));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("perPage", perPage));
        }

        if (sortBy != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("sortBy", sortBy));
        }

        if (etag != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("etag", etag));
        }

        if (page != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("page", page));
        }

        if (fields != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("fields", fields));
        }

        if (order != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call apiV2ProjectSearchNodesPostValidateBeforeCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
        // verify the required parameter 'project' is set
        if (project == null) {
            throw new ApiException("Missing the required parameter 'project' when calling apiV2ProjectSearchNodesPost(Async)");
        }

        return apiV2ProjectSearchNodesPostCall(json, project, wait, perPage, sortBy, etag, page, fields, order, _callback);

    }

    /**
     * 
     * Invoke a search query for nodes and return a paged list response.
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @return NodeListResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result list. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public NodeListResponse apiV2ProjectSearchNodesPost(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String order) throws ApiException {
        ApiResponse<NodeListResponse> localVarResp = apiV2ProjectSearchNodesPostWithHttpInfo(json, project, wait, perPage, sortBy, etag, page, fields, order);
        return localVarResp.getData();
    }

    /**
     * 
     * Invoke a search query for nodes and return a paged list response.
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @return ApiResponse&lt;NodeListResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result list. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<NodeListResponse> apiV2ProjectSearchNodesPostWithHttpInfo(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String order) throws ApiException {
        okhttp3.Call localVarCall = apiV2ProjectSearchNodesPostValidateBeforeCall(json, project, wait, perPage, sortBy, etag, page, fields, order, null);
        Type localVarReturnType = new TypeToken<NodeListResponse>(){}.getType();
        return getApiClient().execute(localVarCall, localVarReturnType);
    }

    /**
     *  (asynchronously)
     * Invoke a search query for nodes and return a paged list response.
     * @param project  (required)
     * @param wait Specify whether search should wait for the search to be idle before responding. (optional)
     * @param perPage Number of elements per page. (optional)
     * @param sortBy Field name to sort the result by. (optional)
     * @param etag Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed. (optional, default to true)
     * @param page Number of page to be loaded. (optional)
     * @param fields Limit the output to certain fields. This is useful in order to reduce the response JSON overhead. (optional, default to )
     * @param order Field order (ASC/DESC) to sort the result by. (optional)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Paged search result list. </td><td>  -  </td></tr>
        <tr><td> 0 </td><td> application/json </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call apiV2ProjectSearchNodesPostAsync(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String order, final ApiCallback<NodeListResponse> _callback) throws ApiException {

        okhttp3.Call localVarCall = apiV2ProjectSearchNodesPostValidateBeforeCall(json, project, wait, perPage, sortBy, etag, page, fields, order, _callback);
        Type localVarReturnType = new TypeToken<NodeListResponse>(){}.getType();
        getApiClient().executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
}
