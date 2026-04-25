import { environment } from '../environments/environment';

export function resolveApiBase(): string {
  return environment.apiBaseUrl;
}
