import { GenericMessageResponse } from '@gentics/mesh-models';
import { RequestMethod } from '../models';
import { MeshFetchDriver } from './fetch-driver';
import { MeshRestClientAbortError } from '../errors';

/* Safe and restore the original fetch implementation in the runs */

let originalFetch: typeof global.fetch;

beforeEach(() => {
    originalFetch = global.fetch;
});

afterEach(() => {
    global.fetch = originalFetch;
});

it('should execute a created request only once, and return the same value', async () => {
    const driver = new MeshFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/api/v2/nowhere';
    const STATUS_MSG = 'Ok';
    const STATUS_CODE = 200;
    const RESPONSE_DATA: GenericMessageResponse = {
        internalMessage: 'foo bar',
        message: 'foo bar',
    };

    let execCounter = 0;

    global.fetch = vitest.fn(() => {
        execCounter++;

        return Promise.resolve<Partial<Response>>({
            status: STATUS_CODE,
            statusText: STATUS_MSG,
            ok: STATUS_CODE < 400,
            headers: new Headers(),
            text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
            json: () => Promise.resolve(RESPONSE_DATA),
        });
    }) as any;

    const req = driver.performJsonRequest({
        headers: {},
        method: RequestMethod.GET,
        url: REQUEST_URL,
        params: {},
    });

    const [res1, res2, res3] = await Promise.all([
        req.send(),
        req.send(),
        req.send(),
    ]);

    expect(execCounter).toEqual(1);
    expect(res1).toBe(res2);
    expect(res1).toBe(res3);
    expect(res2).toBe(res3);
});

it('should return a proper error on an error response', async () => {
    const driver = new MeshFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/api/v2/nowhere';
    const STATUS_MSG = 'Invalid';
    const STATUS_CODE = 400;
    const RESPONSE_DATA: GenericMessageResponse = {
        internalMessage: 'foo bar',
        message: 'foo bar',
    };

    global.fetch = vitest.fn(() => {
        return Promise.resolve<Partial<Response>>({
            status: STATUS_CODE,
            statusText: STATUS_MSG,
            ok: STATUS_CODE < 400,
            headers: new Headers(),
            text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
            json: () => Promise.resolve(RESPONSE_DATA),
        });
    }) as any;

    await expect(function () {
        return driver.performJsonRequest({
            headers: {},
            method: RequestMethod.GET,
            url: REQUEST_URL,
            params: {},
        }).send();
    }).rejects.toThrowErrorMatchingInlineSnapshot(`[Error: Request "GET http://localhost:8080/api/v2/nowhere" responded with error code 400: "Invalid"]`);
});

it('should cancel the request when told to do so', async () => {
    const driver = new MeshFetchDriver();
    const REQUEST_URL = 'http://localhost:8080/api/v2/nowhere';
    const STATUS_MSG = 'Ok';
    const STATUS_CODE = 200;
    const RESPONSE_DATA: GenericMessageResponse = {
        internalMessage: 'foo bar',
        message: 'foo bar',
    };

    global.fetch = vitest.fn((args) => {
        if (typeof args === 'string') {
            args = { url: args };
        } else if (args instanceof URL) {
            args = { url: args.toString() };
        }
        const signal = (args as RequestInit).signal!;

        return new Promise<Partial<Response>>((resolve, reject) => {
            let aborted = false;

            setTimeout(() => {
                if (aborted) {
                    return;
                }

                resolve({
                    status: STATUS_CODE,
                    statusText: STATUS_MSG,
                    ok: STATUS_CODE < 400,
                    headers: new Headers(),
                    text: () => Promise.resolve(JSON.stringify(RESPONSE_DATA)),
                    json: () => Promise.resolve(RESPONSE_DATA),
                });
            }, 1_000);

            signal.addEventListener('abort', () => {
                aborted = true; // 🤘
                reject(signal.reason);
            });
        });
    }) as any;

    const req = driver.performJsonRequest({
        headers: {},
        method: RequestMethod.GET,
        url: REQUEST_URL,
        params: {},
    });

    const res = req.send();

    req.cancel();

    try {
        await res;
        expect.fail('Should not resolve!');
    } catch (err) {
        // Don't use `toBeInstanceOf`, doesn't work!
        expect(err instanceof MeshRestClientAbortError).toEqual(true);
    }
});
