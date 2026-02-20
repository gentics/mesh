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
}
