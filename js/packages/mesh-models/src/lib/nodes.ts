/* eslint-disable @typescript-eslint/naming-convention */
import {
    BasicListOptions,
    BranchedEntityOptions,
    ListResponse,
    MultiLangugeEntityOptions,
    PartialEntityLoadOptions,
    ResolvableLinksOptions,
    RolePermissionsOptions,
    VersionedEntity,
    VersionedEntityOptions,
} from './common';
import { FieldMap } from './fields';
import { ProjectReference } from './projects';
import { SchemaReference } from './schemas';
import { TagReference } from './tags';
import { UserReference } from './users';

export interface NodeChildrenInfo {
    /** Count of children which utilize the schema. */
    count: number;
    /** Reference to the schema of the node child */
    schemaUuid: string;
}

export interface EditableNodeProperties {
    /** Dynamic map with fields of the node language specific content. */
    fields: FieldMap;
    /** ISO 639-1 language tag of the node content. */
    language: string;
    /** List of tags that should be used to tag the node. */
    tags?: TagReference[];
}

export interface Node extends EditableNodeProperties, VersionedEntity {
    /** The project root node. All futher nodes are children of this node. */
    parentNode: NodeReference;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
}

export interface NodeCreateRequest extends EditableNodeProperties {
    /** The project root node. All futher nodes are children of this node. */
    parentNode: NodeReference;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
}

export interface NodeLoadOptions extends RolePermissionsOptions, MultiLangugeEntityOptions,
    PartialEntityLoadOptions<NodeResponse>, BranchedEntityOptions, VersionedEntityOptions, ResolvableLinksOptions { }

export interface NodeListOptions extends BasicListOptions, RolePermissionsOptions, MultiLangugeEntityOptions,
    PartialEntityLoadOptions<NodeResponse>, BranchedEntityOptions, VersionedEntityOptions, ResolvableLinksOptions  { }

export type NodeListResponse = ListResponse<NodeResponse>;

/** The project root node. All futher nodes are children of this node. */
export interface NodeReference {
    /**
     * Optional display name of the node. A display field must be set in the schema in
     * order to populate this property.
     */
    displayName?: string;
    /**
     * Webroot path of the node. The path property will only be provided if the
     * resolveLinks query parameter has been set.
     */
    path?: string;
    /** Name of the project to which the node belongs */
    projectName: string;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
    /** Uuid of the node */
    uuid: string;
}

export interface NodeResponse extends Node {
    /** Map of languages for which content is available and their publish status. */
    availableLanguages: {
        [languageCode: string]: PublishStatusModel;
    };
    /**
     * List of nodes which construct the breadcrumb. Note that the start node will not
     * be included in the list.
     */
    breadcrumb?: NodeReference[];
    /** Object which contains information about child elements. */
    childrenInfo?: {
        [schemaName: string]: NodeChildrenInfo;
    };
    /**
     * Flag which indicates whether the node is a container and can contain nested
     * elements.
     */
    container: boolean;
    /**
     * Display field name of the node. May not be retured if the node schema has no
     * display field.
     */
    displayField?: string;
    /**
     * Display field value of the node. May not be retured if the node schema has no
     * display field.
     */
    displayName?: string;
    /**
     * Map of webroot paths per language. This property will only be populated if the
     * resolveLinks query parameter has been set accordingly.
     */
    languagePaths?: {
        [languageCode: string]: string;
    };
    /**
     * Webroot path to the node content. Will only be provided if the resolveLinks query
     * parameter has been set accordingly.
     */
    path?: string;
    /** Reference to the project of the node. */
    project: ProjectReference;
    /** List of tags that were used to tag the node. */
    tags: TagReference[];
}

export interface NodeUpdateRequest extends EditableNodeProperties {
    /**
     * Version number which can be provided in order to handle and detect concurrent
     * changes to the node content.
     */
    version?: string;
}

export interface NodeDeleteOptions {
    /** Specifiy whether deletion should also be applied recursively. */
    recursive?: boolean;
}

export interface NodeVersionsResponse {
    versions?: {
        [languageCode: string]: VersionInfo[];
    };
}

export interface VersionInfo {
    /** Is the version used as a root version in another branch? */
    branchRoot: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** Is the content a draft version? */
    draft: boolean;
    /** Is the content published version? */
    published: boolean;
    /** Version of the content. */
    version: string;
}

export interface PublishOptions {
    /** Specifiy whether the invoked action should be applied recursively. */
    recursive: boolean;
}

export interface PublishStatusModel {
    /** ISO8601 formatted publish date string. */
    publishDate?: string;
    /** Flag which indicates whether the content is published. */
    published: boolean;
    /** User reference of the creator of the element. */
    publisher: UserReference;
    /** Version number. */
    version: string;
}

export interface PublishStatusResponse {
    /** Map of publish status entries per language */
    availableLanguages?: {
        [languageCode: string]: PublishStatusModel;
    };
}
