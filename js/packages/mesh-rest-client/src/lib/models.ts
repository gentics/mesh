import {
    BranchCreateRequest,
    BranchListOptions,
    BranchListResponse,
    BranchResponse,
    BranchUpdateRequest,
    ClusterConfigResponse,
    ClusterStatusResponse,
    CoordinatorConfig,
    CoordinatorMasterResponse,
    GenericMessageResponse,
    GraphQLOptions,
    GraphQLRequest,
    GraphQLResponse,
    GroupCreateRequest,
    GroupListOptions,
    GroupListResponse,
    GroupLoadOptions,
    GroupResponse,
    GroupUpdateRequest,
    LocalConfigModel,
    LoginResponse,
    MicroschemaLoadOptions,
    MicroschemaCreateRequest,
    MicroschemaListOptions,
    MicroschemaListResponse,
    MicroschemaResponse,
    MicroschemaUpdateRequest,
    NodeCreateRequest,
    NodeDeleteOptions,
    NodeListOptions,
    NodeListResponse,
    NodeResponse,
    NodeUpdateRequest,
    NodeVersionsResponse,
    PluginListResponse,
    ProjectCreateRequest,
    ProjectListOptions,
    ProjectListResponse,
    ProjectResponse,
    ProjectUpdateRequest,
    PublishOptions,
    PublishStatusResponse,
    RoleCreateRequest,
    RoleListOptions,
    RoleListResponse,
    RoleLoadOptions,
    RolePermissionRequest,
    RolePermissionResponse,
    RoleResponse,
    RoleUpdateRequest,
    SchemaChanges,
    SchemaCreateRequest,
    SchemaListOptions,
    SchemaListResponse,
    SchemaLoadOptions,
    SchemaResponse,
    SchemaUpdateRequest,
    ServerInfoModel,
    StatusResponse,
    TagListResponse,
    TagListUpdateRequest,
    UserAPITokenResponse,
    UserCreateRequest,
    UserListOptions,
    UserListResponse,
    UserLoadOptions,
    UserResponse,
    UserUpdateRequest,
    ProjectLoadOptions,
    TagFamilyListResponse,
    TagFamilyListOptions,
    TagFamilyLoadOptions,
    TagFamilyResponse,
    TagFamilyCreateRequest,
    TagListOptions,
    TagCreateRequest,
    TagResponse,
    TagFamilyUpdateRequest,
    TagLoadOptions,
    TagUpdateRequest,
    TagNodeListOptions,
    NodeLoadOptions,
    LoginRequest,
    Language,
    ListResponse,
} from '@gentics/mesh-models';

export interface MeshClientDriver {
    performJsonRequest(
        request: MeshRestClientRequest,
        body?: null | string,
    ): Promise<Record<string, any>>;
}

export interface MeshRestClientConfig {
    interceptors?: MeshRestClientInterceptor[];
    connection: MeshClientConnection;
}

export interface MeshRestClientInterceptorData {
    method: RequestMethod;
    protocol?: 'http' | 'https';
    host: string;
    port?: number;
    path: string;
    params: Record<string, string>;
    headers: Record<string, string>;
}

export interface MeshRestClientRequest {
    method: RequestMethod;
    url: string;
    params: Record<string, string>;
    headers: Record<string, string>;
}

export type MeshRestClientInterceptor = (data: MeshRestClientInterceptorData) => MeshRestClientInterceptorData;

export interface AbsoluteMeshClientConnection {
    absolute: true;
    ssl?: boolean;
    host: string;
    port?: number;
    basePath?: string;
    version?: MeshAPIVersion;
}

export interface RelativeMeshClientConnection {
    absolute: false;
    basePath: string;
    version?: MeshAPIVersion;
}

export type MeshClientConnection = AbsoluteMeshClientConnection | RelativeMeshClientConnection;

export enum RequestMethod {
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT',
    DELETE = 'DELETE',
}

export enum MeshAPIVersion {
    V1 = 'v1',
    V2 = 'v2',
}

export interface MeshAuthAPI {
    login(body: LoginRequest): Promise<LoginResponse>;
    me(): Promise<UserResponse>;
    logout(): Promise<GenericMessageResponse>;
}

export interface MeshUserAPI {
    list(params?: UserListOptions): Promise<UserListResponse>;
    create(body: UserCreateRequest): Promise<UserResponse>;
    get(uuid: string, params?: UserLoadOptions): Promise<UserResponse>;
    update(uuid: string, body: UserUpdateRequest): Promise<UserResponse>;
    delete(uuid: string): Promise<GenericMessageResponse>;

    createAPIToken(uuid: string): Promise<UserAPITokenResponse>;
}

export interface MeshRoleAPI {
    list(params?: RoleListOptions): Promise<RoleListResponse>;
    create(body: RoleCreateRequest): Promise<RoleResponse>;
    get(uuid: string, params?: RoleLoadOptions): Promise<RoleResponse>;
    update(uuid: string, body: RoleUpdateRequest): Promise<RoleResponse>;
    delete(uuid: string): Promise<GenericMessageResponse>;
}

export interface MeshGroupAPI {
    list(params?: GroupListOptions): Promise<GroupListResponse>;
    create(body: GroupCreateRequest): Promise<GroupResponse>;
    get(uuid: string, params?: GroupLoadOptions): Promise<GroupResponse>;
    update(uuid: string, body: GroupUpdateRequest): Promise<GroupResponse>;
    delete(uuid: string): Promise<GenericMessageResponse>;

    getRoles(uuid: string, params?: RoleListOptions): Promise<RoleListResponse>;
    assignRole(uuid: string, roleUuid: string): Promise<GroupResponse>;
    unassignRole(uuid: string, roleUuid: string): Promise<void>;

    getUsers(uuid: string, params?: UserListOptions): Promise<UserListResponse>;
    assignUser(uuid: string, userUuid: string): Promise<GroupResponse>;
    unassignUser(uuid: string, userUuid: string): Promise<void>;
}

export interface MeshPermissionAPI {
    get(roleUuid: string, path: string): Promise<RolePermissionResponse>;
    set(roleUuid: string, path: string, body: RolePermissionRequest): Promise<GenericMessageResponse>;

    check(userUuid: string, path: string): Promise<RolePermissionResponse>;
}

export interface MeshProjectAPI {
    list(params?: ProjectListOptions): Promise<ProjectListResponse>;
    create(body: ProjectCreateRequest): Promise<ProjectResponse>;
    get(project: string, params?: ProjectLoadOptions): Promise<ProjectResponse>;
    update(project: string, body: ProjectUpdateRequest): Promise<ProjectResponse>;
    delete(project: string): Promise<GenericMessageResponse>;

    listSchemas(project: string, params?: SchemaListOptions): Promise<SchemaListResponse>;
    getSchema(project: string, uuid: string): Promise<SchemaResponse>;
    assignSchema(project: string, uuid: string): Promise<SchemaResponse>;
    unassignSchema(project: string, uuid: string): Promise<void>;

    listMicroschemas(project: string, params?: MicroschemaListOptions): Promise<MicroschemaListResponse>;
    getMicroschema(project: string, uuid: string): Promise<MicroschemaResponse>;
    assignMicroschema(project: string, uuid: string): Promise<MicroschemaResponse>;
    unassignMicroschema(project: string, uuid: string): Promise<void>;
}

export interface MeshSchemaAPI {
    list(params?: SchemaListOptions): Promise<SchemaListResponse>;
    create(body: SchemaCreateRequest): Promise<SchemaResponse>;
    get(uuid: string, params?: SchemaLoadOptions): Promise<SchemaResponse>;
    update(uuid: string, body: SchemaUpdateRequest): Promise<SchemaResponse>;
    delete(uuid: string): Promise<void>;

    diff(uuid: string, body: SchemaUpdateRequest): Promise<SchemaChanges>;
    changes(uuid: string, body: SchemaChanges): Promise<GenericMessageResponse>;
}

export interface MeshMicroschemaAPI {
    list(params?: MicroschemaListOptions): Promise<MicroschemaListResponse>;
    create(body: MicroschemaCreateRequest): Promise<MicroschemaResponse>;
    get(uuid: string, params?: MicroschemaLoadOptions): Promise<MicroschemaResponse>;
    update(uuid: string, body: MicroschemaUpdateRequest): Promise<MicroschemaResponse>;
    delete(uuid: string): Promise<void>;

    diff(uuid: string, body: MicroschemaUpdateRequest): Promise<SchemaChanges>;
    changes(uuid: string, body: SchemaChanges): Promise<GenericMessageResponse>;
}

export interface MeshNodeAPI {
    list(project: string, params?: NodeListOptions): Promise<NodeListResponse>;
    create(project: string, body: NodeCreateRequest): Promise<NodeResponse>;
    get(project: string, uuid: string, prams?: NodeLoadOptions): Promise<NodeResponse>;
    update(project: string, uuid: string, body: NodeUpdateRequest): Promise<NodeResponse>;
    delete(project: string, uuid: string, params?: NodeDeleteOptions): Promise<GenericMessageResponse>;
    deleteLanguage(project: string, uuid: string, language: string): Promise<GenericMessageResponse>;
    children(project: string, uuid: string, params?: NodeListOptions): Promise<NodeListResponse>;
    versions(project: string, uuid: string): Promise<NodeVersionsResponse>;

    publishStatus(project: string, uuid: string, language?: string): Promise<PublishStatusResponse>;
    publish(project: string, uuid: string, language?: string, params?: PublishOptions): Promise<PublishStatusResponse>;
    unpublish(project: string, uuid: string, language?: string): Promise<GenericMessageResponse>;

    listTags(project: string, uuid: string): Promise<TagListResponse>;
    setTags(project: string, uuid: string, tags: TagListUpdateRequest): Promise<TagListResponse>;
    assignTag(project: string, uuid: string, tag: string): Promise<NodeResponse>;
    removeTag(project: string, uuid: string, tag: string): Promise<GenericMessageResponse>;
}

export interface MeshBranchAPI {
    list(project: string, params?: BranchListOptions): Promise<BranchListResponse>;
    create(project: string, body: BranchCreateRequest): Promise<BranchResponse>;
    get(project: string, uuid: string): Promise<BranchResponse>;
    update(project: string, uuid: string, body: BranchUpdateRequest): Promise<BranchResponse>;
    asLatest(project: string, uuid: string): Promise<BranchResponse>;
}

export interface MeshTagFamiliesAPI {
    list(project: string, params?: TagFamilyListOptions): Promise<TagFamilyListResponse>;
    create(project: string, body: TagFamilyCreateRequest): Promise<TagFamilyResponse>;
    get(project: string, uuid: string, params?: TagFamilyLoadOptions): Promise<TagFamilyResponse>;
    update(project: string, uuid: string, body: TagFamilyUpdateRequest): Promise<TagFamilyResponse>;
    delete(project: string, uuid: string): Promise<void>;
}

export interface MeshTagsAPI {
    list(project: string, familyUuid: string, params?: TagListOptions): Promise<TagListResponse>;
    create(project: string, familyUuid: string, body: TagCreateRequest): Promise<TagResponse>;
    get(project: string, familyUuid: string, uuid: string, params?: TagLoadOptions): Promise<TagResponse>;
    update(project: string, familyUuid: string, uuid: string, body: TagUpdateRequest): Promise<TagResponse>;
    delete(project: string, familyUuid: string, uuid: string): Promise<void>;

    nodes(project: string, familyUuid: string, uuid: string, params?: TagNodeListOptions): Promise<NodeListResponse>;
}

export interface MeshServerAPI {
    info(): Promise<ServerInfoModel>;
    config(): Promise<LocalConfigModel>;
    status(): Promise<StatusResponse>;
}

export interface MeshCoordinatorAPI {
    config(): Promise<CoordinatorConfig>;
    master(): Promise<CoordinatorMasterResponse>;
}

export interface MeshClusterAPI {
    config(): Promise<ClusterConfigResponse>;
    status(): Promise<ClusterStatusResponse>;
}

export interface MeshPluginAPI {
    list(): Promise<PluginListResponse>;
}

export type MeshGraphQLAPI = (project: string, body: GraphQLRequest, params?: GraphQLOptions) => Promise<GraphQLResponse>;

export interface MeshLanguageAPI {
    list(project: string): Promise<ListResponse<Language>>;
}