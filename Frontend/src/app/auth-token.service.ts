import { Injectable } from '@angular/core';
import { AuthSessionResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  private readonly accessTokenKey = 'roomflow.accessToken';
  private readonly refreshTokenKey = 'roomflow.refreshToken';

  storeSession(session: AuthSessionResponse): void {
    const storage = this.storage();
    if (!storage) {
      return;
    }

    storage.setItem(this.accessTokenKey, session.accessToken);
    storage.setItem(this.refreshTokenKey, session.refreshToken);
  }

  clearSession(): void {
    const storage = this.storage();
    if (!storage) {
      return;
    }

    storage.removeItem(this.accessTokenKey);
    storage.removeItem(this.refreshTokenKey);
  }

  hasSession(): boolean {
    return this.getAccessToken() !== null || this.getRefreshToken() !== null;
  }

  getAccessToken(): string | null {
    return this.storage()?.getItem(this.accessTokenKey) ?? null;
  }

  getRefreshToken(): string | null {
    return this.storage()?.getItem(this.refreshTokenKey) ?? null;
  }

  isTokenExpired(token: string, bufferMs = 0): boolean {
    const expiration = this.getTokenExpiration(token);
    if (expiration === null) {
      return true;
    }

    return expiration <= Date.now() + bufferMs;
  }

  private getTokenExpiration(token: string): number | null {
    const payload = this.parseTokenPayload(token);
    const exp = payload?.['exp'];
    if (typeof exp !== 'number') {
      return null;
    }
    return exp * 1000;
  }

  private parseTokenPayload(token: string): Record<string, unknown> | null {
    const parts = token.split('.');
    if (parts.length !== 3 || !parts[1]) {
      return null;
    }

    try {
      const decoded = this.decodeBase64Url(parts[1]);
      return JSON.parse(decoded) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private decodeBase64Url(value: string): string {
    if (typeof globalThis.atob !== 'function') {
      throw new Error('Base64 decoder is unavailable.');
    }

    const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
    const padding = normalized.length % 4;
    const padded = normalized + '='.repeat((4 - padding) % 4);
    const binary = globalThis.atob(padded);
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
    return new TextDecoder().decode(bytes);
  }

  private storage(): Storage | null {
    try {
      return typeof globalThis.localStorage === 'undefined' ? null : globalThis.localStorage;
    } catch {
      return null;
    }
  }
}
