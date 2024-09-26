/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, PagingMetaInfo, PermissionInfo } from './common';
import { TagReference } from './tags';
import { UserReference } from './users';

export interface BranchCreateRequest {
    /**
     * Optional reference to the base branch. If not set, the new branch will be based
     * on the current 'latest' branch.
     */
    baseBranch?: BranchReference;
    /**
     * The hostname of the branch which will be used to generate links across multiple
     * projects.
     */
    hostname?: string;
    /** Whether the new branch will be set as 'latest' branch. Defaults to 'true'. */
    latest?: boolean;
    /** Name of the branch. */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * SSL flag of the branch which will be used to generate links across multiple
     * projects.
     */
    ssl?: boolean;
}

/**
 * Optional reference to the base branch. If not set, the new branch will be based
 * on the current 'latest' branch.
 */
export interface BranchReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface BranchInfoMicroschemaList {
    /** List of microschema references. */
    microschemas?: BranchMicroschemaInfo[];
}

export interface BranchInfoSchemaList {
    /** List of schema references. */
    schemas?: BranchSchemaInfo[];
}

export interface BranchListOptions extends BasicListOptions { }

export interface BranchListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: BranchResponse[];
}

export interface BranchMicroschemaInfo {
    /** Uuid of the migration job. */
    jobUuid?: string;
    /**
     * Status of the migration which was triggered when the schema/microschema was added
     * to the branch.
     */
    migrationStatus?: string;
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
    /** The version of the microschema. */
    version: string;
    versionUuid?: string;
}

export interface BranchResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /**
     * The hostname of the branch which will be used to generate links across multiple
     * projects.
     */
    hostname?: string;
    /**
     * Flag which indicates whether this is the latest branch. Requests that do not
     * specify a specific branch will be performed in the scope of the latest branch.
     */
    latest: boolean;
    /**
     * Flag which indicates whether any active node migration for this branch is still
     * running or whether all nodes have been migrated to this branch.
     */
    migrated: boolean;
    /** Name of the branch. */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /**
     * SSL flag of the branch which will be used to generate links across multiple
     * projects.
     */
    ssl?: boolean;
    /** List of tags that were used to tag the branch. */
    tags: TagReference[];
    /** Uuid of the element */
    uuid: string;
}

export interface BranchSchemaInfo {
    /** Uuid of the migration job. */
    jobUuid?: string;
    /**
     * Status of the migration which was triggered when the schema/microschema was added
     * to the branch.
     */
    migrationStatus?: string;
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
    /** The version of the microschema. */
    version: string;
    versionUuid?: string;
}

export interface BranchUpdateRequest {
    /**
     * The hostname of the branch which will be used to generate links across multiple
     * projects.
     */
    hostname?: string;
    /** Name of the branch. */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * SSL flag of the branch which will be used to generate links across multiple
     * projects.
     */
    ssl?: boolean;
}
