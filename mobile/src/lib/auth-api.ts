export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_URL?.trim() || '').replace(/\/+$/, '');

const REQUEST_TIMEOUT_MS = 30_000;

type ApiErrorPayload = Record<string, unknown> | string | null;
export type AuthTokens = Pick<LoginResponse, 'accessToken' | 'refreshToken' | 'tokenType'>;
type AuthenticatedSession = AuthTokens & { email: string };
type SaveRefreshedTokens = (tokens: LoginResponse) => Promise<void>;

class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiRequestError';
  }
}

async function request<TResponse>(
  path: string,
  method: 'POST' | 'DELETE',
  body?: object,
  authorization?: string,
): Promise<TResponse> {
  if (!API_BASE_URL) {
    throw new Error(
      'API URL is not configured. Set EXPO_PUBLIC_API_URL in mobile/.env and restart Expo.',
    );
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/${path}`, {
      method,
      headers: {
        Accept: 'application/json, text/plain',
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(authorization ? { Authorization: authorization } : {}),
      },
      ...(body ? { body: JSON.stringify(body) } : {}),
      signal: controller.signal,
    });
    const responseText = await response.text();
    const payload = parseResponse(responseText);

    if (!response.ok) {
      throw new ApiRequestError(
        getErrorMessage(payload) || `The server returned an error (${response.status}).`,
        response.status,
      );
    }

    return payload as TResponse;
  } catch (error) {
    if (controller.signal.aborted) {
      throw new Error(
        `The request to ${API_BASE_URL} timed out. Check that your phone and laptop are on the same Wi-Fi and that the backend is reachable on port 8080.`,
      );
    }

    if (error instanceof TypeError) {
      throw new Error(
        `Could not connect to ${API_BASE_URL}. Check that your phone and laptop are on the same Wi-Fi, that the backend is reachable on port 8080, and that your laptop firewall allows the connection.`,
      );
    }

    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

function post<TResponse>(path: string, body: object): Promise<TResponse> {
  return request<TResponse>(path, 'POST', body);
}

async function authenticatedRequest<TResponse>(
  path: string,
  method: 'POST' | 'DELETE',
  session: AuthenticatedSession,
  saveRefreshedTokens: SaveRefreshedTokens,
  body?: object,
): Promise<TResponse> {
  const authorization = `${session.tokenType || 'Bearer'} ${session.accessToken}`;
  try {
    return await request<TResponse>(path, method, body, authorization);
  } catch (error) {
    if (!(error instanceof ApiRequestError) || error.status !== 401) {
      throw error;
    }

    const refreshedTokens = await authApi.refresh(session.refreshToken);
    if (!refreshedTokens.accessToken || !refreshedTokens.refreshToken) {
      throw new Error('The server did not return valid refreshed tokens.');
    }
    await saveRefreshedTokens(refreshedTokens);
    const retryBody =
      body && 'refreshToken' in body
        ? { ...body, refreshToken: refreshedTokens.refreshToken }
        : body;
    return request<TResponse>(
      path,
      method,
      retryBody,
      `${refreshedTokens.tokenType || 'Bearer'} ${refreshedTokens.accessToken}`,
    );
  }
}

function parseResponse(responseText: string): ApiErrorPayload {
  if (!responseText) {
    return null;
  }
  try {
    return JSON.parse(responseText) as ApiErrorPayload;
  } catch {
    return responseText;
  }
}

function getErrorMessage(payload: ApiErrorPayload): string {
  if (typeof payload === 'string') {
    return payload;
  }
  if (!payload) {
    return '';
  }
  if (typeof payload.message === 'string') {
    return payload.message;
  }

  const validationMessages = Object.values(payload).filter(
    (value): value is string => typeof value === 'string',
  );
  return validationMessages.join('\n');
}

export type OtpRequestResponse = { message: string };
export type OtpVerificationResponse = {
  message: string;
  verified: boolean;
  email: string;
};
export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  refreshToken: string;
};

export const authApi = {
  requestOtp: (email: string) =>
    post<OtpRequestResponse>('request-otp', { email }),
  verifyOtp: (email: string, otp: string) =>
    post<OtpVerificationResponse>('verify-otp', { email, otp }),
  register: (fullName: string, email: string, password: string) =>
    post<string>('register', { fullName, email, password }),
  login: (email: string, password: string) =>
    post<LoginResponse>('login', { email, password }),
  refresh: (refreshToken: string) =>
    post<LoginResponse>('refresh', { refreshToken }),
  forgotPassword: (email: string) =>
    post<OtpRequestResponse>('forgot-password', { email }),
  verifyResetOtp: (email: string, otp: string) =>
    post<OtpVerificationResponse>('verify-reset-otp', { email, otp }),
  resetPassword: (email: string, newPassword: string) =>
    post<string>('reset-password', { email, newPassword }),
  deleteAccount: (session: AuthenticatedSession, saveRefreshedTokens: SaveRefreshedTokens) =>
    authenticatedRequest<string>(
      'delete-account',
      'DELETE',
      session,
      saveRefreshedTokens,
    ),
  logout: (session: AuthenticatedSession, saveRefreshedTokens: SaveRefreshedTokens) =>
    authenticatedRequest<string>(
      'logout',
      'POST',
      session,
      saveRefreshedTokens,
      { refreshToken: session.refreshToken },
    ),
};
