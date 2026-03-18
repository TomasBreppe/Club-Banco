import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { parseJwt } from './jwt.util';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();

  if (!token) {
    router.navigateByUrl('/login');
    return false;
  }

  const payload = parseJwt(token);

  // ⚠️ Ajuste según tu JWT: a veces viene "role", "rol", "authorities", etc.
  const rol =
    payload?.rol ??
    payload?.role ??
    payload?.roles?.[0] ??
    payload?.authorities?.[0];

  // Tus roles suelen ser: "ROLE_ADMIN" o "ADMIN"
  const isAdmin = rol === 'ADMIN' || rol === 'ROLE_ADMIN';

  if (!isAdmin) {
    router.navigateByUrl('/me'); // después hacemos pantalla socio
    return false;
  }

  return true;
};
