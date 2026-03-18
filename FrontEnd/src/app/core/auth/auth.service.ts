import { Injectable } from '@angular/core';

const TOKEN_KEY = 'cb_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  clear() {
    localStorage.removeItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
