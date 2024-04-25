import { GenericErrorResponse } from '@gentics/mesh-models';
import { MeshRestClientRequestError } from '../errors';
import { MeshClientDriver, MeshRestClientRequestData, MeshRestClientResponse } from '../models';

async function parseErrorFromAPI<T>(request: MeshRestClientRequestData, res: Response): Promise<T> {
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

    throw new MeshRestClientRequestError(
        `Request "${request.method} ${request.url}" responded with error code ${res.status}: "${res.statusText}"`,
        request,
        res.status,
        raw,
        parsed,
        bodyError,
    );
}

export class MeshFetchDriver implements MeshClientDriver {

    constructor() { }

    performJsonRequest(
        request: MeshRestClientRequestData,
        body?: null | string,
    ): MeshRestClientResponse<Record<string, any>> {
        return this.prepareRequest(request, (fullUrl) => {
            return {
                method: request.method,
                url: fullUrl,
                headers: request.headers,
                body: body,
            } as any;
        }, (res) => {
            if (res.ok) {
                return res.json();
            }
            return parseErrorFromAPI(request, res);
        });
    }

    private prepareRequest<T>(
        request: MeshRestClientRequestData,
        fn: (fullUrl: string) => RequestInfo,
        handler: (res: Response) => Promise<T>,
    ): MeshRestClientResponse<T> {
        let fullUrl = request.url;
        if (request.params) {
            const params = new URLSearchParams(request.params).toString();
            if (params) {
                fullUrl += `?${params}`;
            }
        }

        const abortController = new AbortController();

        function sendRequest() {
            const options: RequestInfo = {
                ...fn(fullUrl) as any,
                signal: abortController.signal,
            }
            return fetch(options)
                .then(res => handler(res));
        }

        return {
            cancel: () => abortController.abort(),
            send: () => sendRequest(),
        };
    }

}
