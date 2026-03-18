export interface LoginRequest {
  email: string;
  password: string;
}

export interface BaseResponse<T> {
  mensaje: string;
  status: number;
  data: T;
}

// ajustá el campo token según tu backend real:
export interface LoginResponseData {
  token: string;
  rol?: string;
  mustChangePassword?: boolean;
}
