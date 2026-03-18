import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { BaseResponse, LoginRequest, LoginResponseData } from './auth.models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  login(body: LoginRequest): Observable<BaseResponse<LoginResponseData>> {
    return this.http.post<BaseResponse<LoginResponseData>>(`${this.base}/api/auth/login`, body);
  }
}
