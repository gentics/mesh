package com.gentics.mesh.contentrag.common.model;

/**
 * Publikationsstatus eines Mesh-Nodes. MVP indexiert standardmaessig nur {@link #PUBLISHED}.
 */
public enum PublishStatus {
    /** Veroeffentlichte Version (MVP-Default fuer die Indexierung). */
    PUBLISHED,
    /** Entwurfsversion (nicht veroeffentlicht). */
    DRAFT,
    /** Archivierte Version. */
    ARCHIVED
}
