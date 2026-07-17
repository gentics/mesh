package com.gentics.mesh.test.openapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openapitools.client.ApiCallback;
import org.openapitools.client.ApiException;
import org.openapitools.client.Pair;
import org.openapitools.client.api.DefaultApi;

import com.google.gson.JsonParser;

import io.vertx.core.json.JsonObject;

/**
 * An upgrade to the generated DefaultAPI, overriding its generation problems 
 */
@SuppressWarnings("rawtypes")
public class UpgradedDefaultApi extends DefaultApi {

	private UpgradedApiClient localVarApiClient;

	public UpgradedDefaultApi(UpgradedApiClient apiClient) {
		super(apiClient);
		this.localVarApiClient = apiClient;
	}

	@Override
	public UpgradedApiClient getApiClient() {
		return localVarApiClient;
	}

	// Fix for no generated response

	public UpgradedCall apiV2RamlGetCall(final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/raml";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/x-yaml"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

	public UpgradedCall apiV2ProjectWebrootfieldFieldNamePathGetCall(@jakarta.annotation.Nonnull String path, @jakarta.annotation.Nonnull String fieldName, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Double fpz, @jakarta.annotation.Nullable String rect, @jakarta.annotation.Nullable Double w, @jakarta.annotation.Nullable Double h, @jakarta.annotation.Nullable String resize, @jakarta.annotation.Nullable String version, @jakarta.annotation.Nullable Double fpy, @jakarta.annotation.Nullable String crop, @jakarta.annotation.Nullable Double fpx, final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/{project}/webrootfield/{fieldName}/{path}"
            .replace("{" + "path" + "}", getApiClient().escapeString(path.toString()))
            .replace("{" + "fieldName" + "}", getApiClient().escapeString(fieldName.toString()))
            .replace("{" + "project" + "}", getApiClient().escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (fpz != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("fpz", fpz));
        }

        if (rect != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("rect", rect));
        }

        if (w != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("w", w));
        }

        if (h != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("h", h));
        }

        if (resize != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("resize", resize));
        }

        if (version != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("version", version));
        }

        if (fpy != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("fpy", fpy));
        }

        if (crop != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("crop", crop));
        }

        if (fpx != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("fpx", fpx));
        }

        final String[] localVarAccepts = {
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

	public UpgradedCall apiV2OpenapiYamlGetCall(final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/openapi.yaml";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/x-yaml"
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return getApiClient().buildCall(basePath, localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    // Fix for no generated body parameter

	public UpgradedCall apiV2UtilitiesLinkResolverPostCall(String body, String lang, String resolveLinks, ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/api/v2/utilities/linkResolver";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (lang != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("lang", lang));
        }

        if (resolveLinks != null) {
            localVarQueryParams.addAll(getApiClient().parameterToPair("resolveLinks", resolveLinks));
        }

        final String[] localVarAccepts = {
        };
        final String localVarAccept = getApiClient().selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "text/plain"
        };
        final String localVarContentType = getApiClient().selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        
        return getApiClient().buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

	public UpgradedCall apiV2ProjectNodesNodeUuidPostCall(@jakarta.annotation.Nonnull String nodeUuid, @jakarta.annotation.Nonnull String project, JsonObject body, final ApiCallback _callback) throws ApiException {
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

    // Missing JSON body fix

    public UpgradedCall apiV2SearchNodesPostCall(String jsonbody, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String branch, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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

    public UpgradedCall apiV2RawSearchNodesPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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

    public UpgradedCall apiV2ProjectRawSearchNodesPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
    public UpgradedCall apiV2RawSearchGroupsPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/rawSearch/groups";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchRolesPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/rawSearch/roles";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchProjectsPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/rawSearch/projects";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchTagsPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/rawSearch/tags";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2SearchSchemasPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/search/schemas";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2SearchMicroschemasPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/search/microschemas";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }
    public UpgradedCall apiV2ProjectSearchTagFamiliesPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/{project}/search/tagFamilies"
            .replace("{" + "project" + "}", localVarApiClient.escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }
    public UpgradedCall apiV2ProjectSearchTagsPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/{project}/search/tags"
            .replace("{" + "project" + "}", localVarApiClient.escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }
    public UpgradedCall apiV2SearchTagsPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/search/tags";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }
    public UpgradedCall apiV2SearchProjectsPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/search/projects";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }
    public UpgradedCall apiV2SearchRolesPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/search/roles";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }
    public UpgradedCall apiV2SearchGroupsPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/search/groups";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2ProjectRawSearchTagsPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/{project}/rawSearch/tags"
            .replace("{" + "project" + "}", localVarApiClient.escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2ProjectRawSearchTagFamiliesPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/{project}/rawSearch/tagFamilies"
            .replace("{" + "project" + "}", localVarApiClient.escapeString(project.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchTagFamiliesPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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

        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/v2/rawSearch/tagFamilies";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchSchemasPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/rawSearch/schemas";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchMicroschemasPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/rawSearch/microschemas";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2RawSearchUsersPostCall(String json, @jakarta.annotation.Nullable Boolean wait, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/rawSearch/users";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2SearchUsersPostCall(String json, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
        String localVarPath = "/api/v2/search/users";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        if (sortBy != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("sortBy", sortBy));
        }

        if (wait != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("wait", wait));
        }

        if (page != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("page", page));
        }

        if (perPage != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("perPage", perPage));
        }

        if (order != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("order", order));
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        if (localVarContentType != null) {
            localVarHeaderParams.put("Content-Type", localVarContentType);
        }

        String[] localVarAuthNames = new String[] { "bearerAuth" };
        return localVarApiClient.buildCall(basePath, localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    public UpgradedCall apiV2ProjectSearchNodesPostCall(String json, @jakarta.annotation.Nonnull String project, @jakarta.annotation.Nullable Boolean wait, @jakarta.annotation.Nullable Double perPage, @jakarta.annotation.Nullable String sortBy, @jakarta.annotation.Nullable Boolean etag, @jakarta.annotation.Nullable Double page, @jakarta.annotation.Nullable String fields, @jakarta.annotation.Nullable String order, final ApiCallback _callback) throws ApiException {
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
}
