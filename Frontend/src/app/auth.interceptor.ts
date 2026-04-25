import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthTokenService } from './auth-token.service';

const HAS_RETRIED = new HttpContextToken<boolean>(() => false);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authApi = inject(AuthApiService);
  const authTokens = inject(AuthTokenService);
  const path = resolvePath(request.url);

  if (shouldSkipInterceptor(path)) {
    return next(request);
  }

  if (!requiresAuthentication(path)) {
    const accessToken = authTokens.getAccessToken();
    return next(withAuthorizationHeader(request, accessToken && !authTokens.isTokenExpired(accessToken) ? accessToken : null));
  }

  return authApi.ensureFreshAccessToken().pipe(
    switchMap((token) => next(withAuthorizationHeader(request, token))),
    catchError((error) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || request.context.get(HAS_RETRIED)) {
        return throwError(() => error);
      }

      return authApi.ensureFreshAccessToken(true).pipe(
        switchMap((token) => {
          if (!token) {
            return throwError(() => error);
          }

          return next(withAuthorizationHeader(
            request.clone({ context: request.context.set(HAS_RETRIED, true) }),
            token
          ));
        })
      );
    })
  );
};

function withAuthorizationHeader(request: Parameters<HttpInterceptorFn>[0], token: string | null) {
  if (!token) {
    return request;
  }

  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

function shouldSkipInterceptor(path: string): boolean {
  return !path.startsWith('/api/')
    || path === '/api/auth/login'
    || path === '/api/auth/register'
    || path === '/api/auth/refresh';
}

function requiresAuthentication(path: string): boolean {
  if (!path.startsWith('/api/')) {
    return false;
  }

  return path !== '/api/auth/logout'
    && path !== '/api/health'
    && !path.startsWith('/api/locations')
    && !path.startsWith('/api/tariffs');
}

function resolvePath(url: string): string {
  try {
    return new URL(url, globalThis.location?.origin ?? 'http://localhost').pathname;
  } catch {
    return url;
  }
}
