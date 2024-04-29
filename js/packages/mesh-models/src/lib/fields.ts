import { ElasticSearchSettings } from './common';

export enum FieldType {
    BINARY = 'binary',
    BOOLEAN = 'boolean',
    DATE = 'date',
    /**
     * @deprecated Use the `STRING` type instead
     */
    HTML = 'html',
    LIST = 'list',
    MICRONODE = 'micronode',
    NODE = 'node',
    NUMBER = 'number',
    STRING = 'string',
}


export interface SchemaField {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** Label of the field. */
    label?: string;
    /** Name of the field. */
    name: string;
    /** If the field is excluded from indexing. */
    noIndex?: boolean;
    /** If the field is required. */
    required?: boolean;
    /** Type of the field. */
    type: FieldType;
    /** Type of the list values. Only used when `type` is `FieldType.LIST`. */
    listType?: FieldType;
    /**
     * A whitelist to restrict value assignment.
     * When it's a (micro-)node field, it's the names of the (micro-)schemas which are allowed to be set.
     */
    allow?: string[];
    /**
     * Indicates whether binaries must be checked by an external service before being available for download.
     * When the property is set, new binaries will have a check status of `POSTPONED` until the check service
     * sets the status to either `ACCEPTED` or `DENIED`.
     */
    checkServiceUrl?: string;
    /** Controls how the to extract the metadata from uploaded binary. */
    extract?: FieldExtractOptions;
}

export interface FieldExtractOptions {
    /** Extracts and sends plain text content to Elasticsearch if available. */
    content?: boolean;
    /** Extracts and sends metadata to Elasticsearch. An example for metadata is exif data from JPGs. */
    metadata?: boolean;
}

export interface FieldMap {
    [key: string]: SchemaField;
}
