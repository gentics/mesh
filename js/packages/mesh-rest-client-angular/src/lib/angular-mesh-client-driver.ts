import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { GenericErrorResponse } from '@gentics/mesh-models';
import {
    MeshClientDriver,
    MeshRestClientRequestData,
    MeshRestClientResponse,
    MeshRestClientRequestError,
    MeshRestClientAbortError,
} from '@gentics/mesh-rest-client';
import { Subscription, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export class AngularMeshClientDriver implements MeshClientDriver {

    constructor(
        private http: HttpClient,
    ) { }

    performJsonRequest(
        request: MeshRestClientRequestData,
        body?: string,
    ): MeshRestClientResponse<Record<string, any>> {
        const obs = this.http.request(request.method, request.url, {
            body,
            headers: request.headers,
            params: request.params,
            observe: 'response',
            responseType: 'text',
        }).pipe(
            map((res) => {
                if (res.ok) {
                    return JSON.parse(res.body || '');
                }

                // eslint-disable-next-line @typescript-eslint/only-throw-error
                throw new HttpErrorResponse({
                    headers: new HttpHeaders(request.headers),
                    status: res.status,
                    statusText: res.statusText,
                    error: res.body,
                    url: request.url,
                });
            }),
            catchError((err) => {
                if (!(err instanceof HttpErrorResponse)) {
                    return throwError(() => err);
                }

                let raw = '';
                let parsed: GenericErrorResponse | undefined;
                let bodyError: unknown;

                try {
                    raw = err.error;
                    try {
                        parsed = JSON.parse(raw);
                    } catch (err) {
                        bodyError = err;
                    }
                } catch (err) {
                    bodyError = err;
                }

                return throwError(() => new MeshRestClientRequestError(
                    `Request "${request.method} ${request.url}" responded with error code ${err.status}: "${err.statusText}"`,
                    request,
                    err.status,
                    raw,
                    parsed,
                    bodyError as Error,
                ));
            }),
        );

        let promiseSub: Subscription | null = null;
        let canceled = false;

        return {
            // rx: () => obs,
            send: () => {
                if (canceled) {
                    return Promise.reject(new MeshRestClientAbortError(request));
                }

                return new Promise((resolve, reject) => {
                    let tmpValue: any;
                    let hasValue = false;
                    let isMultiValue = false;

                    promiseSub = obs.subscribe({
                        next: (value) => {
                            if (!hasValue) {
                                tmpValue = value;
                                hasValue = true;
                            } else if (!isMultiValue) {
                                tmpValue = [tmpValue, value];
                                isMultiValue = true;
                            } else {
                                (tmpValue as any[]).push(value);
                            }
                        },
                        complete: () => {
                            resolve(tmpValue);
                        },
                        error: (err) => {
                            // eslint-disable-next-line @typescript-eslint/prefer-promise-reject-errors
                            reject(err);
                        },
                    });
                });
            },
            cancel: () => {
                if (promiseSub) {
                    promiseSub.unsubscribe();
                    promiseSub = null;
                }
                canceled = true;
            },
        };
    }

}
