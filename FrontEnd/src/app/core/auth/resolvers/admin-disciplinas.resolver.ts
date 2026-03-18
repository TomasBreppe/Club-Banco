import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { DisciplinasService } from '../../../service/disciplinas.service';

export const adminDisciplinasResolver: ResolveFn<any> = async () => {
  const api = inject(DisciplinasService);
  return firstValueFrom(api.listar());
};
