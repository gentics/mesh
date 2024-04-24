/* eslint-disable @typescript-eslint/naming-convention */
import { Entity, ListResponse, PagingOptions, RolePermissionsOptions, SortingOptions } from './common';
import { NodeReference } from './nodes';
import { SchemaReference } from './schemas';

export interface EditableProjectProperties {
    /** Name of the project */
    name: string;
}

export interface Project extends EditableProjectProperties, Entity { }

export interface ProjectCreateRequest extends EditableProjectProperties {
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * The hostname of the project can be used to generate links across multiple
     * projects. The hostname will be stored along the initial branch of the project.
     */
    hostname?: string;
    /**
     * SSL flag of the project which will be used to generate links across multiple
     * projects. The flag will be stored along the intial branch of the project.
     */
    ssl?: boolean;
}

export interface ProjectLoadOptions extends RolePermissionsOptions { }

export interface ProjectListOptions extends PagingOptions, SortingOptions, RolePermissionsOptions { }

export type ProjectListResponse = ListResponse<ProjectResponse>;

/** Reference to the project of the node. */
export interface ProjectReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface ProjectResponse extends Project {
    /** The project root node. All futher nodes are children of this node. */
    rootNode: NodeReference;
}

export interface ProjectUpdateRequest extends EditableProjectProperties {}
