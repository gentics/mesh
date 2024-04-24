/* eslint-disable @typescript-eslint/naming-convention */
import {
    BasicListOptions,
    BranchedEntityOptions,
    Entity,
    ExpandableNode,
    ListResponse,
    MultiLangugeEntityOptions,
    PartialEntityLoadOptions,
    PermissionInfo,
    ResolvableLinksOptions,
    RolePermissionsOptions,
    VersionedEntityOptions,
} from './common';
import { GroupReference } from './groups';

export interface UserAPITokenResponse {
    /** Date of the last time the API token was issued. */
    previousIssueDate: string;
    /** Issued client API token. */
    token: string;
}

export interface EditableUserProperties {
    /** Email address of the user */
    emailAddress?: string;
    /** Firstname of the user. */
    firstname?: string;
    /** Lastname of the user. */
    lastname?: string;
    /** If the user is a administrator. */
    admin?: boolean;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: ExpandableNode;
    /** Username of the user. */
    username: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange?: boolean;
}

export interface UserCreateRequest extends EditableUserProperties {
    /** Password of the new user. */
    password: string;
    /** Username of the user. */
    username: string;
}

export interface UserLoadOptions extends BranchedEntityOptions, RolePermissionsOptions,
    MultiLangugeEntityOptions, VersionedEntityOptions, ResolvableLinksOptions, PartialEntityLoadOptions<UserResponse> {}

export interface UserListOptions extends BasicListOptions, RolePermissionsOptions { }

export type UserListResponse = ListResponse<UserResponse>;

export interface UserPermissionResponse extends PermissionInfo {}

/** User reference of the creator of the element. */
export interface UserReference {
    /** Firstname of the user */
    firstname?: string;
    /** Lastname of the user */
    lastname?: string;
    /** Uuid of the user */
    uuid: string;
}

export interface UserResetTokenResponse {
    /** ISO8601 date of the creation date for the provided token */
    created: string;
    /** JSON Web Token which was issued by the API. */
    token: string;
}

export interface User extends EditableUserProperties, Entity { }

export interface UserResponse extends User {
    /**
     * Flag which indicates whether the user is enabled or disabled. Disabled users can
     * no longer log into Gentics Mesh. Deleting a user user will not remove it. Instead
     * the user will just be disabled.
     */
    enabled: boolean;
    /** List of group references to which the user belongs. */
    groups: GroupReference[];
    /** Hashsum of user roles which can be used for user permission caching. */
    rolesHash: string;
}

export interface UserUpdateRequest extends EditableUserProperties {
    /**
     * Optional group id for the user. If provided the user will automatically be
     * assigned to the identified group.
     */
    groupUuid?: string;
    /** New password of the user */
    password?: string;
}
