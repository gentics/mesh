/* eslint-disable @typescript-eslint/naming-convention */
import { Entity, ListResponse, PagingOptions, PartialEntityLoadOptions, RolePermissionsOptions, SortingOptions } from './common';

export interface EditableTagProperties {
    /** Name of the tag. */
    name: string;
}

export interface EditableTagFamilyProperties {
    /** Name of the tag family which will be created. */
    name: string;
}

export interface Tag extends EditableTagProperties, Entity { }

export interface TagFamily extends EditableTagFamilyProperties, Entity { }

export interface TagFamilyCreateRequest extends EditableTagFamilyProperties { }

export interface TagFamilyLoadOptions { }

export interface TagFamilyListOptions extends PagingOptions, SortingOptions, RolePermissionsOptions { }

export type TagFamilyListResponse = ListResponse<TagFamilyResponse>;

/** Reference to the tag family to which the tag belongs. */
export interface TagFamilyReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface TagFamilyResponse extends TagFamily { }

export interface TagFamilyUpdateRequest extends Partial<EditableTagFamilyProperties> { }

export interface TagCreateRequest extends EditableTagProperties { }

export interface TagLoadOptions extends PartialEntityLoadOptions<TagResponse> { }

export interface TagListOptions extends PagingOptions, SortingOptions, RolePermissionsOptions, PartialEntityLoadOptions<TagResponse> { }

export interface TagNodeListOptions extends PagingOptions, SortingOptions { }

export type TagListResponse = ListResponse<TagResponse>;

export interface TagListUpdateRequest {
    /**
     * List of tags which should be assigned to the node. Tags which are not included
     * will be removed from the node.
     */
    tags: TagReference[];
}

export interface TagReference {
    /** Name of the referenced element */
    name?: string;
    tagFamily?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface TagResponse extends Tag {
    /** Reference to the tag family to which the tag belongs. */
    tagFamily: TagFamilyReference;
}

export interface TagUpdateRequest extends Partial<EditableTagProperties> { }
