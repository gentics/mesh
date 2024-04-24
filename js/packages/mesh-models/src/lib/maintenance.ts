export interface ConsistencyCheckResponse {
    /** List of found inconsistencies. */
    inconsistencies: InconsistencyInfo[];
    /**
     * Flag which indicates whether the output was truncated because more than 250 have
     * been found.
     */
    outputTruncated: boolean;
    /** Counter for repair operations */
    repairCount: { [key: string]: number };
    /** Result of the consistency check. */
    result: string;
}

export interface InconsistencyInfo {
    /** Description of the inconsistency. */
    description: string;
    /** Uuid of the element which is related to the inconsistency. */
    elementUuid: string;
    /**
     * Repair action which will attept to fix the inconsistency. The action will only be
     * invoked when using invoking the rapair endpoint.
     */
    repairAction: string;
    /**
     * Status of the inconsistency. This will indicate whether the inconsistency could
     * be resolved via the repair action.
     */
    repaired: boolean;
    /** Level of severity of the inconsistency. */
    severity: string;
}


export interface MeshSearchStatusResponse {
    /**
     * Flag which indicates whether Elasticsearch is available and search queries can be
     * executed.
     */
    available: boolean;
    /** Map which contains various metric values. */
    metrics?: { [key: string]: EntityMetrics };
}

export interface EntityMetrics {
    delete?: TypeMetrics;
    insert?: TypeMetrics;
    update?: TypeMetrics;
}

export interface TypeMetrics {
    pending?: number;
    synced?: number;
}
