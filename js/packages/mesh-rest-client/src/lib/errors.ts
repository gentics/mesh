import { GenericErrorResponse } from '@gentics/mesh-models';
import { MeshRestClientRequest } from './models';

export class RequestFailedError extends Error {

    constructor(
        message: string,
        public request: MeshRestClientRequest,
        public responseCode: number,
        public rawBody?: string,
        public data?: GenericErrorResponse,
        public bodyError?: Error,
    ) {
        super (message);
    }
}
