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
        request: MeshRestClientRequestData,
        body?: null | string,
    ): MeshRestClientResponse<Record<string, any>>;
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

export interface MeshRestClientRequestData {
    method: RequestMethod;
    url: string;
    params: Record<string, string>;
    headers: Record<string, string>;
}

export interface MeshRestClientResponse<T> {
    send: () => Promise<T>;
    cancel: () => void;
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
    login(body: LoginRequest): MeshRestClientResponse<LoginResponse>;
    me(): MeshRestClientResponse<UserResponse>;
    logout(): MeshRestClientResponse<GenericMessageResponse>;
}

export interface MeshUserAPI {
    list(params?: UserListOptions): MeshRestClientResponse<UserListResponse>;
    create(body: UserCreateRequest): MeshRestClientResponse<UserResponse>;
    get(uuid: string, params?: UserLoadOptions): MeshRestClientResponse<UserResponse>;
    update(uuid: string, body: UserUpdateRequest): MeshRestClientResponse<UserResponse>;
    delete(uuid: string): MeshRestClientResponse<GenericMessageResponse>;

    createAPIToken(uuid: string): MeshRestClientResponse<UserAPITokenResponse>;
}

export interface MeshRoleAPI {
    list(params?: RoleListOptions): MeshRestClientResponse<RoleListResponse>;
    create(body: RoleCreateRequest): MeshRestClientResponse<RoleResponse>;
    get(uuid: string, params?: RoleLoadOptions): MeshRestClientResponse<RoleResponse>;
    update(uuid: string, body: RoleUpdateRequest): MeshRestClientResponse<RoleResponse>;
    delete(uuid: string): MeshRestClientResponse<GenericMessageResponse>;
}

export interface MeshGroupAPI {
    list(params?: GroupListOptions): MeshRestClientResponse<GroupListResponse>;
    create(body: GroupCreateRequest): MeshRestClientResponse<GroupResponse>;
    get(uuid: string, params?: GroupLoadOptions): MeshRestClientResponse<GroupResponse>;
    update(uuid: string, body: GroupUpdateRequest): MeshRestClientResponse<GroupResponse>;
    delete(uuid: string): MeshRestClientResponse<GenericMessageResponse>;

    getRoles(uuid: string, params?: RoleListOptions): MeshRestClientResponse<RoleListResponse>;
    assignRole(uuid: string, roleUuid: string): MeshRestClientResponse<GroupResponse>;
    unassignRole(uuid: string, roleUuid: string): MeshRestClientResponse<void>;

    getUsers(uuid: string, params?: UserListOptions): MeshRestClientResponse<UserListResponse>;
    assignUser(uuid: string, userUuid: string): MeshRestClientResponse<GroupResponse>;
    unassignUser(uuid: string, userUuid: string): MeshRestClientResponse<void>;
}

export interface MeshPermissionAPI {
    get(roleUuid: string, path: string): MeshRestClientResponse<RolePermissionResponse>;
    set(roleUuid: string, path: string, body: RolePermissionRequest): MeshRestClientResponse<GenericMessageResponse>;

    check(userUuid: string, path: string): MeshRestClientResponse<RolePermissionResponse>;
}

export interface MeshProjectAPI {
    list(params?: ProjectListOptions): MeshRestClientResponse<ProjectListResponse>;
    create(body: ProjectCreateRequest): MeshRestClientResponse<ProjectResponse>;
    get(project: string, params?: ProjectLoadOptions): MeshRestClientResponse<ProjectResponse>;
    update(project: string, body: ProjectUpdateRequest): MeshRestClientResponse<ProjectResponse>;
    delete(project: string): MeshRestClientResponse<GenericMessageResponse>;

    listSchemas(project: string, params?: SchemaListOptions): MeshRestClientResponse<SchemaListResponse>;
    getSchema(project: string, uuid: string): MeshRestClientResponse<SchemaResponse>;
    assignSchema(project: string, uuid: string): MeshRestClientResponse<SchemaResponse>;
    unassignSchema(project: string, uuid: string): MeshRestClientResponse<void>;

    listMicroschemas(project: string, params?: MicroschemaListOptions): MeshRestClientResponse<MicroschemaListResponse>;
    getMicroschema(project: string, uuid: string): MeshRestClientResponse<MicroschemaResponse>;
    assignMicroschema(project: string, uuid: string): MeshRestClientResponse<MicroschemaResponse>;
    unassignMicroschema(project: string, uuid: string): MeshRestClientResponse<void>;
}

export interface MeshSchemaAPI {
    list(params?: SchemaListOptions): MeshRestClientResponse<SchemaListResponse>;
    create(body: SchemaCreateRequest): MeshRestClientResponse<SchemaResponse>;
    get(uuid: string, params?: SchemaLoadOptions): MeshRestClientResponse<SchemaResponse>;
    update(uuid: string, body: SchemaUpdateRequest): MeshRestClientResponse<SchemaResponse>;
    delete(uuid: string): MeshRestClientResponse<void>;

    diff(uuid: string, body: SchemaUpdateRequest): MeshRestClientResponse<SchemaChanges>;
    changes(uuid: string, body: SchemaChanges): MeshRestClientResponse<GenericMessageResponse>;
}

export interface MeshMicroschemaAPI {
    list(params?: MicroschemaListOptions): MeshRestClientResponse<MicroschemaListResponse>;
    create(body: MicroschemaCreateRequest): MeshRestClientResponse<MicroschemaResponse>;
    get(uuid: string, params?: MicroschemaLoadOptions): MeshRestClientResponse<MicroschemaResponse>;
    update(uuid: string, body: MicroschemaUpdateRequest): MeshRestClientResponse<MicroschemaResponse>;
    delete(uuid: string): MeshRestClientResponse<void>;

    diff(uuid: string, body: MicroschemaUpdateRequest): MeshRestClientResponse<SchemaChanges>;
    changes(uuid: string, body: SchemaChanges): MeshRestClientResponse<GenericMessageResponse>;
}

export interface MeshNodeAPI {
    list(project: string, params?: NodeListOptions): MeshRestClientResponse<NodeListResponse>;
    create(project: string, body: NodeCreateRequest): MeshRestClientResponse<NodeResponse>;
    get(project: string, uuid: string, prams?: NodeLoadOptions): MeshRestClientResponse<NodeResponse>;
    update(project: string, uuid: string, body: NodeUpdateRequest): MeshRestClientResponse<NodeResponse>;
    delete(project: string, uuid: string, params?: NodeDeleteOptions): MeshRestClientResponse<GenericMessageResponse>;
    deleteLanguage(project: string, uuid: string, language: string): MeshRestClientResponse<GenericMessageResponse>;
    children(project: string, uuid: string, params?: NodeListOptions): MeshRestClientResponse<NodeListResponse>;
    versions(project: string, uuid: string): MeshRestClientResponse<NodeVersionsResponse>;

    publishStatus(project: string, uuid: string, language?: string): MeshRestClientResponse<PublishStatusResponse>;
    publish(project: string, uuid: string, language?: string, params?: PublishOptions): MeshRestClientResponse<PublishStatusResponse>;
    unpublish(project: string, uuid: string, language?: string): MeshRestClientResponse<GenericMessageResponse>;

    listTags(project: string, uuid: string): MeshRestClientResponse<TagListResponse>;
    setTags(project: string, uuid: string, tags: TagListUpdateRequest): MeshRestClientResponse<TagListResponse>;
    assignTag(project: string, uuid: string, tag: string): MeshRestClientResponse<NodeResponse>;
    removeTag(project: string, uuid: string, tag: string): MeshRestClientResponse<GenericMessageResponse>;
}

export interface MeshBranchAPI {
    list(project: string, params?: BranchListOptions): MeshRestClientResponse<BranchListResponse>;
    create(project: string, body: BranchCreateRequest): MeshRestClientResponse<BranchResponse>;
    get(project: string, uuid: string): MeshRestClientResponse<BranchResponse>;
    update(project: string, uuid: string, body: BranchUpdateRequest): MeshRestClientResponse<BranchResponse>;
    asLatest(project: string, uuid: string): MeshRestClientResponse<BranchResponse>;
}

export interface MeshTagFamiliesAPI {
    list(project: string, params?: TagFamilyListOptions): MeshRestClientResponse<TagFamilyListResponse>;
    create(project: string, body: TagFamilyCreateRequest): MeshRestClientResponse<TagFamilyResponse>;
    get(project: string, uuid: string, params?: TagFamilyLoadOptions): MeshRestClientResponse<TagFamilyResponse>;
    update(project: string, uuid: string, body: TagFamilyUpdateRequest): MeshRestClientResponse<TagFamilyResponse>;
    delete(project: string, uuid: string): MeshRestClientResponse<void>;
}

export interface MeshTagsAPI {
    list(project: string, familyUuid: string, params?: TagListOptions): MeshRestClientResponse<TagListResponse>;
    create(project: string, familyUuid: string, body: TagCreateRequest): MeshRestClientResponse<TagResponse>;
    get(project: string, familyUuid: string, uuid: string, params?: TagLoadOptions): MeshRestClientResponse<TagResponse>;
    update(project: string, familyUuid: string, uuid: string, body: TagUpdateRequest): MeshRestClientResponse<TagResponse>;
    delete(project: string, familyUuid: string, uuid: string): MeshRestClientResponse<void>;

    nodes(project: string, familyUuid: string, uuid: string, params?: TagNodeListOptions): MeshRestClientResponse<NodeListResponse>;
}

export interface MeshServerAPI {
    info(): MeshRestClientResponse<ServerInfoModel>;
    config(): MeshRestClientResponse<LocalConfigModel>;
    status(): MeshRestClientResponse<StatusResponse>;
}

export interface MeshCoordinatorAPI {
    config(): MeshRestClientResponse<CoordinatorConfig>;
    master(): MeshRestClientResponse<CoordinatorMasterResponse>;
}

export interface MeshClusterAPI {
    config(): MeshRestClientResponse<ClusterConfigResponse>;
    status(): MeshRestClientResponse<ClusterStatusResponse>;
}

export interface MeshPluginAPI {
    list(): MeshRestClientResponse<PluginListResponse>;
}

export type MeshGraphQLAPI = (project: string, body: GraphQLRequest, params?: GraphQLOptions) => MeshRestClientResponse<GraphQLResponse>;

export interface MeshLanguageAPI {
    list(project: string): MeshRestClientResponse<ListResponse<Language>>;
}
