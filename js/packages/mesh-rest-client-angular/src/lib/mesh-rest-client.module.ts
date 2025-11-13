import { NgModule } from '@angular/core';
import { MeshRestClientService } from './mesh-rest-client.service';

@NgModule({
    providers: [
        MeshRestClientService,
    ],
})
// eslint-disable-next-line @typescript-eslint/no-extraneous-class
export class MeshRestClientModule {}
