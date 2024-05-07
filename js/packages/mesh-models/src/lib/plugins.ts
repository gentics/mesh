/* eslint-disable @typescript-eslint/naming-convention */
import { PagingMetaInfo } from './common';

export interface PluginDeploymentRequest {
    /** Deployment path of the plugin which is relative to the plugin directory. */
    path: string;
}

export enum PluginStatus {
    LOADED = 'LOADED',
    VALIDATED = 'VALIDATED',
    STARTED = 'STARTED',
    PRE_REGISTERED = 'PRE_REGISTERED',
    INITIALIZED = 'INITIALIZED',
    REGISTERED = 'REGISTERED',
    FAILED = 'FAILED',
    FAILED_RETRY = 'FAILED_RETRY',
}

export interface PluginListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: PluginResponse[];
}

/** Manifest of the plugin */
export interface PluginManifest {
    /**
     * API name of the plugin. This will be used to construct the REST API path to the
     * plugin.
     */
    apiName: string;
    /** Author of the plugin. */
    author: string;
    /** Description of the plugin. */
    description: string;
    /** Unique id of the plugin was defined by the plugin developer. */
    id: string;
    /** Inception date of the plugin. */
    inception: string;
    /** License of the plugin. */
    license: string;
    /** Human readable name of the plugin. */
    name: string;
    /** Version of the plugin. */
    version: string;
}

export interface PluginResponse {
    /** Id of the plugin. */
    id: string;
    /** Manifest of the plugin */
    manifest: PluginManifest;
    /** Name of the plugin. */
    name: string;
    status: PluginStatus;
}
