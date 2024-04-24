import { GenericErrorResponse } from '@gentics/mesh-models';
import { RequestFailedError } from '../errors';
import { MeshClientDriver, MeshRestClientRequest } from '../models';

export class MeshFetchDriver implements MeshClientDriver {

    constructor() {}

    async performJsonRequest(
        request: MeshRestClientRequest,
        body?: null | string,
    ): Promise<Record<string, any>> {
        let fullUrl = request.url;
        if (request.params) {
            const params = new URLSearchParams(request.params).toString();
            if (params) {
                fullUrl += `?${params}`;
            }
        }

        const res = await fetch({
            method: request.method,
            url: fullUrl,
            headers: request.headers,
            body: body,
        } as any);

        if (res.ok) {
            return res.json();
        }

        let raw: string;
        let parsed: GenericErrorResponse;
        let bodyError: Error;

        try {
            raw = await res.text();
            try {
                parsed = JSON.parse(raw);
            } catch (err) {
                bodyError = err;
            }
        } catch (err) {
            bodyError = err;
        }

        throw new RequestFailedError(
            `Request "${request.method} ${request.url}" responded with error code ${res.status}: "${res.statusText}"`,
            request,
            res.status,
            raw,
            parsed,
            bodyError,
        );
    }

}
