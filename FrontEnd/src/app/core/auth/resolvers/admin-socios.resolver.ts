import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { forkJoin, firstValueFrom } from 'rxjs';
import { SociosService } from '../../../service/socio.service';
import { DisciplinasService } from '../../../service/disciplinas.service';

export const adminSociosResolver: ResolveFn<any> = async () => {
  const sociosApi = inject(SociosService);
  const disciplinasApi = inject(DisciplinasService);

  return firstValueFrom(
    forkJoin({
      disciplinas: disciplinasApi.listar(),
      socios: sociosApi.listar(),
    })
  );
};
