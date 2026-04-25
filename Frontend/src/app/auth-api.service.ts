import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap } from 'rxjs/operators';
import { resolveApiBase } from './api-base';
import { AuthSessionResponse } from './auth.models';
import { AuthTokenService } from './auth-token.service';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly authTokens = inject(AuthTokenService);
  private readonly apiBase = resolveApiBase();
  private refreshRequest$?: Observable<string | null>;

  ensureFreshAccessToken(forceRefresh = false): Observable<string | null> {
    const accessToken = this.authTokens.getAccessToken();
    const hasUsableAccessToken = !!accessToken && !this.authTokens.isTokenExpired(accessToken, 5 * 60 * 1000);
    if (!forceRefresh && hasUsableAccessToken) {
      return of(accessToken);
    }

    const refreshToken = this.authTokens.getRefreshToken();
    if (!refreshToken || this.authTokens.isTokenExpired(refreshToken)) {
      if (!accessToken || this.authTokens.isTokenExpired(accessToken)) {
        this.authTokens.clearSession();
        return of(null);
      }
      return of(accessToken);
    }

    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.refreshRequest$ = this.http.post<AuthSessionResponse>(`${this.apiBase}/auth/refresh`, { refreshToken }).pipe(
      tap((session) => this.authTokens.storeSession(session)),
      map((session) => session.accessToken),
      catchError(() => {
        this.authTokens.clearSession();
        return of(null);
      }),
      finalize(() => {
        this.refreshRequest$ = undefined;
      }),
      shareReplay(1)
    );

    return this.refreshRequest$;
  }
}
