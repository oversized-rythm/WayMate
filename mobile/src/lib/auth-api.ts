export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_URL?.trim() || '').replace(/\/+$/, '');

const REQUEST_TIMEOUT_MS = 30_000;

type ApiErrorPayload = Record<string, unknown> | string | null;

async function post<TResponse>(path: string, body: object): Promise<TResponse> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/${path}`, {
      method: 'POST',
      headers: { Accept: 'application/json, text/plain', 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    const responseText = await response.text();
    const payload = parseResponse(responseText);

    if (!response.ok) {
      throw new Error(
        getErrorMessage(payload) || `The server returned an error (${response.status}).`,
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
};
