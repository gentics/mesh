import { inject, Injectable } from '@angular/core';
import {
    type MeshAuthAPI,
    type MeshBranchAPI,
    type MeshClusterAPI,
    type MeshCoordinatorAPI,
    type MeshGraphQLAPI,
    type MeshGroupAPI,
    type MeshLanguageAPI,
    type MeshMicroschemaAPI,
    type MeshNodeAPI,
    type MeshPermissionAPI,
    type MeshPluginAPI,
    type MeshProjectAPI,
    MeshRestClient,
    type MeshRestClientConfig,
    type MeshRestClientRequestData,
    type MeshRoleAPI,
    type MeshSchemaAPI,
    type MeshServerAPI,
    type MeshTagFamiliesAPI,
    type MeshTagsAPI,
    type MeshUserAPI,
    type RequestMethod,
} from '@gentics/mesh-rest-client';
import { AngularMeshClientDriver } from './angular-mesh-client-driver';

@Injectable()
export class MeshRestClientService {

    private driver = inject(AngularMeshClientDriver);
    private client: MeshRestClient | null = null;

    init(config: MeshRestClientConfig, apiKey?: string): void {
        this.client = new MeshRestClient(this.driver, config, apiKey);
    }

    configure(config: MeshRestClientConfig): void {
        if (this.client != null) {
            this.client.config = config;
        }
    }

    isInitialized(): boolean {
        return this.client != null;
    }

    getConfig(): MeshRestClientConfig | null {
        return this.client?.config ?? null;
    }

    public prepareRequest(
        requestMethod: RequestMethod,
        path: string,
        queryParams: Record<string, any>,
        requestHeaders: Record<string, string>,
    ): MeshRestClientRequestData {
        return this.client!.prepareRequest(requestMethod, path, queryParams, requestHeaders);
    }

    get auth(): MeshAuthAPI {
        return this.client!.auth;
    }

    get users(): MeshUserAPI {
        return this.client!.users;
    }

    get roles(): MeshRoleAPI {
        return this.client!.roles;
    }

    get groups(): MeshGroupAPI {
        return this.client!.groups;
    }

    get permissions(): MeshPermissionAPI {
        return this.client!.permissions;
    }

    get projects(): MeshProjectAPI {
        return this.client!.projects;
    }

    get schemas(): MeshSchemaAPI {
        return this.client!.schemas;
    }

    get microschemas(): MeshMicroschemaAPI {
        return this.client!.microschemas;
    }

    get branches(): MeshBranchAPI {
        return this.client!.branches;
    }

    get nodes(): MeshNodeAPI {
        return this.client!.nodes;
    }

    get tagFamilies(): MeshTagFamiliesAPI {
        return this.client!.tagFamilies;
    }

    get tags(): MeshTagsAPI {
        return this.client!.tags;
    }

    get server(): MeshServerAPI {
        return this.client!.server;
    }

    get coordinator(): MeshCoordinatorAPI {
        return this.client!.coordinator;
    }

    get cluster(): MeshClusterAPI {
        return this.client!.cluster;
    }

    get plugins(): MeshPluginAPI {
        return this.client!.plugins;
    }

    get graphql(): MeshGraphQLAPI {
        return this.client!.graphql;
    }

    get language(): MeshLanguageAPI {
        return this.client!.language;
    }
}
