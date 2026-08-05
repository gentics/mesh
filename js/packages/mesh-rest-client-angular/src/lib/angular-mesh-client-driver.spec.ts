import { HttpResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { UserListResponse } from '@gentics/mesh-models';
import { of } from 'rxjs';
import { provideMeshRestClient } from './mesh-rest-client.module';
import { MeshRestClientService } from './mesh-rest-client.service';

it('should handle the response correctly', async () => {
    let requestCounter = 0;
    const RESPONSE: UserListResponse = {
        _metainfo: {
            currentPage: 1,
            pageCount: 1,
            perPage: 20,
            totalCount: 0,
        },
        data: [],
    };

    TestBed.configureTestingModule({
        providers: [
            provideMeshRestClient(),
            provideHttpClient(
                withInterceptors([
                    (req) => {
                        requestCounter++;

                        return of(new HttpResponse({
                            status: 200,
                            statusText: 'OK',
                            url: req.url,
                            body: RESPONSE,
                        }));
                    },
                ]),
            ),
        ],
    });

    const client = TestBed.inject(MeshRestClientService);
    client.init({
        connection: {
            absolute: true,
            host: 'localhost',
        },
    });
    const request = client.users.list();
    const res = await request.send();

    expect(requestCounter).toEqual(1);
    expect(res).toEqual(RESPONSE);
});
