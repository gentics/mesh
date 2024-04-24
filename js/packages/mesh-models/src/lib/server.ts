export interface ServerInfoModel {
    /** Database structure revision hash. */
    databaseRevision?: string;
    /** Used database implementation vendor name. */
    databaseVendor?: string;
    /** Used database implementation version. */
    databaseVersion?: string;
    /** Node name of the Gentics Mesh instance. */
    meshNodeName?: string;
    /** Gentics Mesh Version string. */
    meshVersion?: string;
    /** Used search implementation vendor name. */
    searchVendor?: string;
    /** Used search implementation version. */
    searchVersion?: string;
    /** Used Vert.x version. */
    vertxVersion?: string;
}

export enum Status {
    STARTING = 'STARTING',
    WAITING_FOR_CLUSTER = 'WAITING_FOR_CLUSTER',
    READY = 'READY',
    SHUTTING_DOWN = 'SHUTTING_DOWN',
    BACKUP = 'BACKUP',
    RESTORE = 'RESTORE',
}

export interface StatusResponse {
    /** The current Gentics Mesh server status. */
    status: Status;
}

export interface ClusterInstanceInfo {
    address?: string;
    name?: string;
    role?: string;
    startDate?: string;
    status?: string;
}

export interface ClusterStatusResponse {
    instances?: ClusterInstanceInfo[];
}

export enum ClusterWriteQuorum {
    ALL = 'all',
    MAJORITY = 'majority',
}

export interface ClusterConfigResponse {
    servers?: ClusterInstanceInfo[];
    writeQuorum?: ClusterWriteQuorum;
    readQuorum?: number;
}

export interface LocalConfigModel {
    /** If true, mutating requests to this instance are not allowed. */
    readOnly?: boolean;
}

export enum CoordinatorMode {
    DISABLED = 'DISABLED',
    CUD = 'CUD',
    ALL = 'ALL',
}

/**
 * The currently active coordination config on this instance.
 */
export interface CoordinatorConfig {
    /** Coordinator mode which can be set to DISABLED to disable coordination, to CUD to handle only modifying requests or to ALL to handle all requests. */
    mode?: CoordinatorMode;
}

export interface CoordinatorMasterResponse {
    host?: string;
    name?: string;
    port?: number;
}
