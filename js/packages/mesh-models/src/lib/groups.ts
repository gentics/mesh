/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, Entity, ListResponse, PartialEntityLoadOptions, RolePermissionsOptions } from './common';
import { RoleReference } from './roles';

export interface EditableGroupProperties {
    /** Name of the group */
    name: string;
}

export interface GroupCreateRequest extends EditableGroupProperties {}

export interface GroupLoadOptions extends RolePermissionsOptions, PartialEntityLoadOptions<GroupResponse> {}

export interface GroupListOptions extends BasicListOptions, RolePermissionsOptions { }

export type GroupListResponse = ListResponse<GroupResponse>;

export interface GroupReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface Group extends EditableGroupProperties, Entity {}

export interface GroupResponse extends Group {
    /** List of role references */
    roles: RoleReference[];
}

export interface GroupUpdateRequest extends EditableGroupProperties { }
