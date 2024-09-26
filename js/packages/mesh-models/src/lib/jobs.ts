/* eslint-disable @typescript-eslint/naming-convention */
import { PagingMetaInfo } from './common';
import { UserReference } from './users';

export interface JobListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: JobResponse[];
}

export interface JobResponse {
    /**
     * The completion count of the job. This indicates how many items the job has
     * processed.
     */
    completionCount: number;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** The detailed error information of the job. */
    errorDetail?: string;
    /** The error message of the job. */
    errorMessage?: string;
    /** Name of the Gentics Mesh instance on which the job was executed. */
    nodeName?: string;
    /** Properties of the job. */
    properties: { [key: string]: string };
    /** The start date of the job. */
    startDate: string;
    /** Migration status. */
    status: string;
    /** The stop date of the job. */
    stopDate: string;
    /** The type of the job. */
    type: string;
    /** Uuid of the element */
    uuid: string;
    /** List of warnings which were encoutered while executing the job. */
    warnings?: JobWarning[];
}

export interface JobWarning {
    message?: string;
    properties?: { [key: string]: string };
    type?: string;
}
