import { NgModule, Provider } from '@angular/core';
import { AngularMeshClientDriver } from './angular-mesh-client-driver';
import { MeshRestClientService } from './mesh-rest-client.service';

/**
 * @deprecated Use the new {@link provideMeshRestClient} function instead of the Module.
 */
@NgModule({
    providers: [
        AngularMeshClientDriver,
        MeshRestClientService,
    ],
})
export class MeshRestClientModule {}

export function provideMeshRestClient(): Provider[] {
    return [
        AngularMeshClientDriver,
        MeshRestClientService,
    ];
}
