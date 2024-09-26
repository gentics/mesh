export interface GraphQLError {
    /** Mesh element id which is related to the error. */
    elementId?: string;
    /** Mesh element type which is related to the error. */
    elementType?: string;
    /** List of locations which are related to the error. */
    locations?: ErrorLocation[];
    /** The error message. */
    message: string;
    /** Type of the error. */
    type: string;
}

export interface GraphQLOptions {
    /** Specify whether search should wait for the search to be idle before responding. */
    wait?: boolean;
    /** Specify a branch to only have node versions for that particular branch in the response  */
    branch?: string;
    /** Specify the version to only have node versions for that particular version in the response  */
    version?: string;
}

export interface GraphQLRequest {
    /** GraphQL operation name. */
    operationName?: string;
    /** The actual GraphQL query. */
    query: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    variables?: { [key: string]: any };
}

export interface GraphQLResponse {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    data?: any;
    /** Array of errors which were encoutered when handling the query. */
    errors?: GraphQLError[];
}

export interface ErrorLocation {
    /** Error column number. */
    column: number;
    /** Error line number. */
    line: number;
}
