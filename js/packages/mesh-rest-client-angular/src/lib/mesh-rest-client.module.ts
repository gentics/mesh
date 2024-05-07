import { NgModule } from '@angular/core';
import { MeshRestClientService } from './mesh-rest-client.service';

@NgModule({
    providers: [
        MeshRestClientService,
    ],
})
export class MeshRestClientModule {}
