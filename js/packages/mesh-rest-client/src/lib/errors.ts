import { GenericErrorResponse } from '@gentics/mesh-models';
import { MeshRestClientRequestData } from './models';

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
    }
}
