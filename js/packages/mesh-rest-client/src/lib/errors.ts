import { GenericErrorResponse } from '@gentics/mesh-models';
import { MeshRestClientRequestData } from './models';

export const MESH_ERROR_INSTANCE = Symbol('gcms-rest-client-error');
export const MESH_ABORT_INSTANCE = Symbol('gcms-rest-client-abort');

/**
 * Error class which is thrown whenever a Request to the REST-API failed.
 * These should only be thrown however, when an actual response (status >= 400),
 * or lack thereof (i.E. status >= 500) is received.
 * Other errors should not be wrapped by this Error.
 * This is so the user of the Client can handle Response errors correctly.
 *
 * To determine if this is the correct object/error, please use the javascript `instanceof`
 * feature, as there have been fixes to make this consistent even in tests.
 *
 * ```ts
 * async function myStuff() {
 *      const client: MeshRestClient = getClient();
 *      try {
 *          await client.auth.me().send();
 *      } catch (err) {
 *          if (err instanceof MeshRestClientRequestError) {
 *              // Handle request error accordingly.
 *          }
 *      }
 * }
 * ```
 */
export class MeshRestClientRequestError extends Error {

    constructor(
        message: string,
        public request: MeshRestClientRequestData,
        public responseCode: number,
        public rawBody?: string,
        public data?: GenericErrorResponse,
        public bodyError?: Error,
    ) {
        super (message);
        Object.defineProperty(this, MESH_ERROR_INSTANCE, {
            value: true,
            writable: false,
            enumerable: false,
            configurable: false,
        });
    }

    /**
     * Hacky workaround to allow custom error objects to still be identified as correct errors.
     * Mainly used in testing.
     * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/instanceof#instanceof_and_symbol.hasinstance
     * @returns If the provided object is an instance of this class.
     */
    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    static override [Symbol.hasInstance](obj: any): boolean {
        return obj != null && MESH_ERROR_INSTANCE in obj;
    }
}

/**
 * Defines a specific error for when a Request is being canceled.
 * The Promise of the Response will be rejected with this error,
 * to properly indicate that the request has been properly canceled.
 *
 * ```ts
 * async function myFunc() {
 *      const client = getClient();
 *      try {
 *          const req = client.node.list();
 *          const data = req.send();
 *          req.cancel();
 *          await data;
 *      } catch (err) {
 *          if (err instanceof MeshRestClientRequestError) {
 *              // The server responded with an error
 *          }
 *          if (err instanceof MeshRestClientAbortError) {
 *              // The request has been successfully aborted
 *          }
 *          // Some other actual error
 *      }
 * }
 * ```
 */
export class MeshRestClientAbortError extends Error {

    constructor(
        public request: MeshRestClientRequestData,
    ) {
        super(`The request "${request.method} ${request.url}" has been canceled`);
        Object.defineProperty(this, MESH_ABORT_INSTANCE, {
            value: true,
            writable: false,
            enumerable: false,
            configurable: false,
        });
    }

    /**
     * Hacky workaround to allow custom error objects to still be identified as correct errors.
     * Mainly used in testing.
     * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/instanceof#instanceof_and_symbol.hasinstance
     * @returns If the provided object is an instance of this class.
     */
    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    static override [Symbol.hasInstance](obj: any): boolean {
        return obj != null && MESH_ABORT_INSTANCE in obj;
    }
}
