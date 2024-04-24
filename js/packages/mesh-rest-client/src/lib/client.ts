import { CONTENT_TYPE_JSON, DELETE, GET, HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_CONTENT_TYPE, POST } from './internal';
import {
    MeshAPIVersion,
    MeshAuthAPI,
    MeshBranchAPI,
    MeshClientDriver,
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
    MeshRestClientConfig,
    MeshRestClientInterceptorData,
    MeshRestClientRequest,
    MeshRoleAPI,
    MeshSchemaAPI,
    MeshServerAPI,
    MeshTagFamiliesAPI,
    MeshTagsAPI,
    MeshUserAPI,
    RequestMethod,
} from './models';
import { toRelativePath, trimTrailingSlash } from './utils';

export class MeshRestClient {

    constructor(
        public driver: MeshClientDriver,
        public config?: MeshRestClientConfig,
        public apiKey?: string,
    ) { }

    public prepareRequest(
        requestMethod: RequestMethod,
        path: string,
        queryParams: Record<string, any>,
        requestHeaders: Record<string, string>,
    ): MeshRestClientRequest {
        let buildPath = '';

        if (this.config.connection.basePath) {
            buildPath += trimTrailingSlash(toRelativePath(this.config.connection.basePath));
        } else {
            const version = this.config.connection.version ?? MeshAPIVersion.V2;
            buildPath += `/api/${version}`;
        }
        buildPath += toRelativePath(path);

        const data: MeshRestClientInterceptorData = this.config.connection.absolute ? {
            method: requestMethod,
            protocol: typeof this.config.connection.ssl === 'boolean'
                ? (this.config.connection.ssl ? 'https' : 'http')
                : null,
            host: this.config.connection.host,
            port: this.config.connection.port,
            path: buildPath,
            params: queryParams,
            headers: requestHeaders,
        } : {
            method: requestMethod,
            protocol: null,
            host: null,
            port: null,
            path: buildPath,
            params: queryParams,
            headers: requestHeaders,
        };

        const { method, protocol, host, port, path: finalPath, params, headers } = this.handleInterceptors(data);

        let url: string;

        if (this.config.connection.absolute) {
            url = `${protocol ?? ''}://${host}`;
            if (port) {
                url += `:${port}`;
            }
            url += finalPath;
        } else {
            url = finalPath;
        }

        return {
            method,
            url,
            params,
            headers,
        };
    }

    protected handleInterceptors(data: MeshRestClientInterceptorData): MeshRestClientInterceptorData {
        const interceptors = this.config.interceptors || [];
        for (const handler of interceptors) {
            data = handler(data);
        }
        return data;
    }

    protected executeJsonRequest<T>(
        method: RequestMethod,
        path: string,
        body?: null | any,
        queryParams?: Record<string, any>,
    ): Promise<T> {
        const headers: Record<string, string> = {
            [HTTP_HEADER_CONTENT_TYPE]: CONTENT_TYPE_JSON,
        };

        if (this.apiKey) {
            headers[HTTP_HEADER_AUTHORIZATION] = `Bearer ${this.apiKey}`;
        }

        const req = this.prepareRequest(method, path, queryParams, headers);

        return this.driver.performJsonRequest(req, body) as any;
    }

    public auth: MeshAuthAPI = {
        login: (body) => this.executeJsonRequest(POST, '/auth/login', body),
        me: () => this.executeJsonRequest(GET, '/auth/me'),
        logout: () => this.executeJsonRequest(GET, '/auth/logout'),
    } as const;

    public users: MeshUserAPI = {
        list: (params?) => this.executeJsonRequest(GET, '/users', null, params),
        create: (body) => this.executeJsonRequest(POST, '/users', body),
        get: (uuid, params?) => this.executeJsonRequest(GET, `/users/${uuid}`, null, params),
        update: (uuid, body) => this.executeJsonRequest(POST, `/users/${uuid}`, body),
        delete: (uuid) => this.executeJsonRequest(DELETE, `/users/${uuid}`),

        createAPIToken: (uuid) => this.executeJsonRequest(POST, `/users/${uuid}/token`),
    } as const;

    public roles: MeshRoleAPI = {
        list: (params?) => this.executeJsonRequest(GET, '/roles', null, params),
        create: (body) => this.executeJsonRequest(POST, '/roles', body),
        get: (uuid, params?) => this.executeJsonRequest(GET, `/roles/${uuid}`, null, params),
        update: (uuid, body) => this.executeJsonRequest(POST, `/roles/${uuid}`, body),
        delete: (uuid) => this.executeJsonRequest(DELETE, `/roles/${uuid}`),
    } as const;

    public groups: MeshGroupAPI = {
        list: (params?) => this.executeJsonRequest(GET, '/groups', null, params),
        create: (body) => this.executeJsonRequest(POST, '/groups', body),
        get: (uuid, params?) => this.executeJsonRequest(GET, `/groups/${uuid}`, null, params),
        update: (uuid, body) => this.executeJsonRequest(POST, `/groups/${uuid}`, body),
        delete: (uuid) => this.executeJsonRequest(DELETE, `/groups/${uuid}`),

        getRoles: (uuid, params?) => this.executeJsonRequest(GET, `/groups/${uuid}`, null, params),
        assignRole: (uuid, roleUuid) => this.executeJsonRequest(POST, `/groups/${uuid}/roles/${roleUuid}`),
        unassignRole: (uuid, roleUuid) => this.executeJsonRequest(DELETE, `/groups/${uuid}/roles/${roleUuid}`),

        getUsers: (uuid, params?) => this.executeJsonRequest(GET, `/groups/${uuid}/users`, null, params),
        assignUser: (uuid, userUuid) => this.executeJsonRequest(POST, `/groups/${uuid}/users/${userUuid}`),
        unassignUser: (uuid, userUuid) => this.executeJsonRequest(DELETE, `/groups/${uuid}/users/${userUuid}`),
    } as const;

    public permissions: MeshPermissionAPI = {
        get: (roleUuid, path) => this.executeJsonRequest(GET, `/roles/${roleUuid}/permissions/${path}`),
        set: (roleUuid, path, body) => this.executeJsonRequest(POST, `/roles/${roleUuid}/permissions/${path}`, body),

        check: (userUuid, path) => this.executeJsonRequest(GET, `/user/${userUuid}/permissions/${path}`),
    } as const;

    public projects: MeshProjectAPI = {
        list: (params?) => this.executeJsonRequest(GET, '/projects', null, params),
        create: (body) => this.executeJsonRequest(POST, '/projects', body),
        get: (project, params?) => this.executeJsonRequest(GET, `/projects/${project}`, null, params),
        update: (project, body) => this.executeJsonRequest(POST, `/projects/${project}`, body),
        delete: (project) => this.executeJsonRequest(DELETE, `/projects/${project}`),

        listSchemas: (project) => this.executeJsonRequest(GET, `/${project}/schemas`),
        getSchema: (project, uuid) => this.executeJsonRequest(GET, `/${project}/schemas/${uuid}`),
        assignSchema: (project, uuid) => this.executeJsonRequest(POST, `/${project}/schemas/${uuid}`),
        unassignSchema: (project, uuid) => this.executeJsonRequest(DELETE, `/${project}/schemas/${uuid}`),

        listMicroschemas: (project) => this.executeJsonRequest(GET, `/${project}/microschemas`),
        getMicroschema: (project, uuid) => this.executeJsonRequest(GET, `/${project}/microschemas/${uuid}`),
        assignMicroschema: (project, uuid) => this.executeJsonRequest(POST, `/${project}/microschemas/${uuid}`),
        unassignMicroschema: (project, uuid) => this.executeJsonRequest(DELETE, `/${project}/microschemas/${uuid}`),
    } as const;

    public schemas: MeshSchemaAPI = {
        list: (params?) => this.executeJsonRequest(GET, '/schemas', null, params),
        create: (body) => this.executeJsonRequest(POST, '/schemas', body),
        get: (uuid, params) => this.executeJsonRequest(GET, `/schemas/${uuid}`, null, params),
        update: (uuid, body) => this.executeJsonRequest(POST, `/schemas/${uuid}`, body),
        delete: (uuid) => this.executeJsonRequest(DELETE, `/schemas/${uuid}`),
        diff: (uuid, body) => this.executeJsonRequest(POST, `/schemas/${uuid}/diff`, body),
        changes: (uuid, body) => this.executeJsonRequest(POST, `/schemas/${uuid}/changes`, body),
    } as const;

    public microschemas: MeshMicroschemaAPI = {
        list: (params?) => this.executeJsonRequest(GET, '/microschemas', null, params),
        create: (body) => this.executeJsonRequest(POST, '/microschemas', body),
        get: (uuid, params?) => this.executeJsonRequest(GET, `/microschemas/${uuid}`, null, params),
        update: (uuid, body) => this.executeJsonRequest(POST, `/microschemas/${uuid}`, body),
        delete: (uuid) => this.executeJsonRequest(DELETE, `/microschemas/${uuid}`),
        diff: (uuid, body) => this.executeJsonRequest(POST, `/microschemas/${uuid}/diff`, body),
        changes: (uuid, body) => this.executeJsonRequest(POST, `/microschemas/${uuid}/changes`, body),
    } as const;

    public nodes: MeshNodeAPI = {
        list: (project, params?) => this.executeJsonRequest(GET, `/${project}/nodes`, null, params),
        create: (project, body) => this.executeJsonRequest(POST, `/${project}/nodes`, body),
        get: (project, uuid, params?) => this.executeJsonRequest(GET, `/${project}/nodes/${uuid}`, null, params),
        update: (project, uuid, body) => this.executeJsonRequest(POST, `/${project}/nodes/${uuid}`, body),
        delete: (project, uuid, params?) => this.executeJsonRequest(DELETE, `/${project}/nodes/${uuid}`, null, params),

        deleteLanguage: (project, uuid, language) => this.executeJsonRequest(DELETE, `/${project}/nodes/${uuid}/languages/${language}`),
        children: (project, uuid, params?) => this.executeJsonRequest(GET, `/${project}/nodes/${uuid}/children`, null, params),
        versions: (project, uuid) => this.executeJsonRequest(GET, `/${project}/nodes/${uuid}/versions`),

        publishStatus: (project, uuid, language?) => {
            const path = language
                ? `/${project}/nodes/${uuid}/languages/${language}/published`
                : `/${project}/nodes/${uuid}/published`;
            return this.executeJsonRequest(GET, path);
        },
        publish: (project, uuid, language?, params?) => {
            const path = language
                ? `/${project}/nodes/${uuid}/languages/${language}/published`
                : `/${project}/nodes/${uuid}/published`;
            return this.executeJsonRequest(POST, path, null, params);
        },
        unpublish: (project, uuid, language?) => {
            const path = language
                ? `/${project}/nodes/${uuid}/languages/${language}/published`
                : `/${project}/nodes/${uuid}/published`;
            return this.executeJsonRequest(DELETE, path);
        },

        listTags: (project, uuid) => this.executeJsonRequest(GET, `/${project}/nodes/${uuid}/tags`),
        setTags: (project, uuid, body) => this.executeJsonRequest(POST, `/${project}/nodes/${uuid}/tags`, body),
        assignTag: (project, uuid, tag) => this.executeJsonRequest(POST, `/${project}/nodes/${uuid}/tags/${tag}`),
        removeTag: (project, uuid, tag) => this.executeJsonRequest(DELETE, `/${project}/nodes/${uuid}/tags/${tag}`),
    } as const;

    public branches: MeshBranchAPI = {
        list: (project) => this.executeJsonRequest(GET, `/${project}/branches`),
        create: (project, body) => this.executeJsonRequest(POST, `/${project}/branches`, body),
        get: (project, uuid) => this.executeJsonRequest(GET, `/${project}/branches/${uuid}`),
        update: (project, uuid, body) => this.executeJsonRequest(POST, `/${project}/branches/${uuid}`, body),
        asLatest: (project, uuid) => this.executeJsonRequest(POST, `/${project}/branches/${uuid}/latest`),
    } as const;

    public tagFamilies: MeshTagFamiliesAPI = {
        list: (project, params?) => this.executeJsonRequest(GET, `/${project}/tagFamilies`, null, params),
        create: (project, body) => this.executeJsonRequest(POST, `/${project}/tagFamilies`, body),
        get: (project, uuid, params?) => this.executeJsonRequest(GET, `/${project}/tagFamilies/${uuid}`, null, params),
        update: (project, uuid, body) => this.executeJsonRequest(POST, `/${project}/tagFamilies/${uuid}`, body),
        delete: (project, uuid) => this.executeJsonRequest(DELETE, `/${project}/tagFamilies/${uuid}`),
    } as const;

    public tags: MeshTagsAPI = {
        list: (project, familyUuid, params?) => this.executeJsonRequest(GET, `/${project}/tagFamilies/${familyUuid}/tags`, null, params),
        create: (project, familyUuid, body) => this.executeJsonRequest(POST, `/${project}/tagFamilies/${familyUuid}/tags`, body),
        get: (project, familyUuid, uuid, params?) => this.executeJsonRequest(GET, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}`, null, params),
        update: (project, familyUuid, uuid, body) => this.executeJsonRequest(POST, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}`, body),
        delete: (project, familyUuid, uuid) => this.executeJsonRequest(DELETE, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}`),

        nodes: (project, familyUuid, uuid, params?) => this.executeJsonRequest(GET, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}/nodes`, null, params),
    } as const;

    public server: MeshServerAPI = {
        info: () => this.executeJsonRequest(GET, '/'),
        config: () => this.executeJsonRequest(GET, '/admin/config'),
        status: () => this.executeJsonRequest(GET, '/admin/status'),
    } as const;

    public coordinator: MeshCoordinatorAPI = {
        config: () => this.executeJsonRequest(GET, '/admin/coordinator/config'),
        master: () => this.executeJsonRequest(GET, '/admin/coordinator/master'),
    } as const;

    public cluster: MeshClusterAPI = {
        config: () => this.executeJsonRequest(GET, '/admin/cluster/config'),
        status: () => this.executeJsonRequest(GET, '/admin/cluster/status'),
    } as const;

    public plugins: MeshPluginAPI = {
        list: () => this.executeJsonRequest(GET, '/admin/plugins'),
    } as const;

    public graphql: MeshGraphQLAPI = (project, body, params) => this.executeJsonRequest(POST, `/${project}/graphql`, body, params);

    public language: MeshLanguageAPI = {
        list: (project) => this.executeJsonRequest(GET, `${project}/languages`),
    } as const;

}
