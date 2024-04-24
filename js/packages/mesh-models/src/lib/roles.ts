/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, Entity, ListResponse, PartialEntityLoadOptions, PermissionInfo, RolePermissionsOptions } from './common';
import { GroupReference } from './groups';

export interface EditableRoleProperties {
    /** Name of the role. */
    name: string;
}

export interface RoleCreateRequest extends EditableRoleProperties {}

export interface RoleLoadOptions extends PartialEntityLoadOptions<RoleResponse> {}

export interface RoleListOptions extends BasicListOptions, RolePermissionsOptions { }

export type RoleListResponse = ListResponse<RoleResponse>

export interface RolePermissionRequest {
    permissions: Partial<PermissionInfo>;
    /** Flag which indicates whether the permission update should be applied recursively. */
    recursive?: boolean;
}

export interface RolePermissionResponse extends PermissionInfo {}

export interface RoleReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface Role extends EditableRoleProperties, Entity { }

export interface RoleResponse extends Role {
    /** List of groups which are assigned to the role. */
    groups: GroupReference[];
}

export interface RoleUpdateRequest extends Role { }
