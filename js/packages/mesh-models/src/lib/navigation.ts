import { NodeResponse } from './nodes';

export interface NavigationElement {
    /** List of further child elements of the node. */
    children?: NavigationElement[];
    node?: NodeResponse;
    /** Uuid of the node within this navigation element. */
    uuid?: string;
}

export interface NavigationResponse {
    /** List of further child elements of the node. */
    children?: NavigationElement[];
    node?: NodeResponse;
    /** Uuid of the node within this navigation element. */
    uuid?: string;
}
