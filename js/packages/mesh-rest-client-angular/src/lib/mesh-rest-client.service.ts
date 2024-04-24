import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
    MeshAuthAPI,
    MeshBranchAPI,
    MeshClusterAPI,
    MeshCoordinatorAPI,
    MeshGraphQLAPI,
    MeshGroupAPI,
    MeshLanguageAPI,
    MeshMicroschemaAPI,
    MeshNodeAPI,
    MeshPermissionAPI,
    MeshPluginAPI,
    MeshProjectAPI,
    MeshRestClient,
    MeshRestClientConfig,
    MeshRestClientRequest,
    MeshRoleAPI,
    MeshSchemaAPI,
    MeshServerAPI,
    MeshTagFamiliesAPI,
    MeshTagsAPI,
    MeshUserAPI,
    RequestMethod,
} from '@gentics/mesh-rest-client';
import { AngularMeshClientDriver } from './angular-mesh-client-driver';

@Injectable()
export class MeshRestClientService {

    private driver: AngularMeshClientDriver;
    private client: MeshRestClient;

    constructor(
        http: HttpClient,
    ) {
        this.driver = new AngularMeshClientDriver(http);
        this.client = new MeshRestClient(this.driver);
    }

    init(config: MeshRestClientConfig, apiKey?: string): void {
        this.client.config = config;
        this.client.apiKey = apiKey;
    }

    configure(config: MeshRestClientConfig): void {
        this.client.config = config;
    }

    isInitialized(): boolean {
        return this.client != null;
    }

    getConfig(): MeshRestClientConfig {
        return this.client.config;
    }

    public prepareRequest(
        requestMethod: RequestMethod,
        path: string,
        queryParams: Record<string, any>,
        requestHeaders: Record<string, string>,
    ): MeshRestClientRequest {
        return this.client.prepareRequest(requestMethod, path, queryParams, requestHeaders);
    }

    get auth(): MeshAuthAPI {
        return this.client.auth;
    }

    get users(): MeshUserAPI {
        return this.client.users;
    }

    get roles(): MeshRoleAPI {
        return this.client.roles;
    }

    get groups(): MeshGroupAPI {
        return this.client.groups;
    }

    get permissions(): MeshPermissionAPI {
        return this.client.permissions;
    }

    get projects(): MeshProjectAPI {
        return this.client.projects;
    }

    get schemas(): MeshSchemaAPI {
        return this.client.schemas;
    }

    get microschemas(): MeshMicroschemaAPI {
        return this.client.microschemas;
    }

    get branches(): MeshBranchAPI {
        return this.client.branches;
    }

    get nodes(): MeshNodeAPI {
        return this.client.nodes;
    }

    get tagFamilies(): MeshTagFamiliesAPI {
        return this.client.tagFamilies;
    }

    get tags(): MeshTagsAPI {
        return this.client.tags;
    }

    get server(): MeshServerAPI {
        return this.client.server;
    }

    get coordinator(): MeshCoordinatorAPI {
        return this.client.coordinator;
    }

    get cluster(): MeshClusterAPI {
        return this.client.cluster;
    }

    get plugins(): MeshPluginAPI {
        return this.client.plugins;
    }

    get graphql(): MeshGraphQLAPI {
        return this.client.graphql;
    }

    get language(): MeshLanguageAPI {
        return this.client.language;
    }
}
