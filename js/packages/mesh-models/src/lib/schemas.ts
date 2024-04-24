/* eslint-disable @typescript-eslint/naming-convention */
import {
    BasicListOptions,
    BranchedEntityOptions,
    ElasticSearchSettings,
    GenericMessageResponse,
    ListResponse,
    PartialEntityLoadOptions,
    RolePermissionsOptions,
    VersionedEntity,
    VersionedEntityOptions,
} from './common';
import { SchemaField } from './fields';

export interface SchemaChange {
    /** Type of operation for this change */
    operation?: string;
    properties?: { [key: string]: any };
    /** Uuid of the change entry */
    uuid?: string;
}

export interface SchemaChanges {
    changes?: SchemaChange[];
}

export interface EditableSchemaProperties {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should be
     * automatically purged on update.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether the nodes of this version should be excluded from the indexing.
     */
    noIndex?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container?: boolean;
    /** Description of the schema. */
    description?: string;
    /** Name of the display field. */
    displayField?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of schema fields */
    fields: SchemaField[];
    /** Name of the schema. */
    name: string;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
}

export interface SchemaCreateRequest extends EditableSchemaProperties {}

export interface SchemaLoadOptions extends BranchedEntityOptions, VersionedEntityOptions,
    PartialEntityLoadOptions<SchemaResponse> {}

export interface SchemaListOptions extends BasicListOptions, RolePermissionsOptions { }

export type SchemaListResponse = ListResponse<SchemaResponse>;

/**
 * Reference to the schema of the root node. Creating a project will also
 * automatically create the base node of the project and link the schema to the
 * initial branch of the project.
 */
export interface SchemaReference {
    name?: string;
    set?: boolean;
    uuid?: string;
    version?: string;
    versionUuid?: string;
}

export interface Schema extends VersionedEntity, EditableSchemaProperties {}

export interface SchemaResponse extends Schema {}

export interface SchemaUpdateRequest extends EditableSchemaProperties {
    /** Version of the schema. */
    version?: string;
}

export interface SchemaValidationResponse {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    message?: GenericMessageResponse;
    /** Status of the validation. */
    status: string;
}
