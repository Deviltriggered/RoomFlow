export interface AuthResponse {
  userId: number;
  email: string;
  legalName: string;
  phone: string | null;
  role: string;
}

export interface AuthSessionResponse {
  user: AuthResponse;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
}
