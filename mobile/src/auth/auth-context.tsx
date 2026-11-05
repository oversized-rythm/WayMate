import * as SecureStore from 'expo-secure-store';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Platform } from 'react-native';

const SESSION_KEY = 'waymate.auth-session';

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  email: string;
};

type AuthContextValue = {
  session: AuthSession | null;
  isReady: boolean;
  storageError: string | null;
  saveSession: (session: AuthSession) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isReady, setIsReady] = useState(Platform.OS === 'web');
  const [storageError, setStorageError] = useState<string | null>(null);

  useEffect(() => {
    if (Platform.OS === 'web') {
      return;
    }

    SecureStore.getItemAsync(SESSION_KEY)
      .then((storedSession) => {
        if (storedSession) {
          const parsed: unknown = JSON.parse(storedSession);
          if (
            typeof parsed !== 'object' ||
            parsed === null ||
            !('accessToken' in parsed) ||
            typeof parsed.accessToken !== 'string' ||
            !('refreshToken' in parsed) ||
            typeof parsed.refreshToken !== 'string' ||
            !('tokenType' in parsed) ||
            typeof parsed.tokenType !== 'string' ||
            !('email' in parsed) ||
            typeof parsed.email !== 'string'
          ) {
            throw new Error('Stored sign-in data is invalid.');
          }
          setSession(parsed as AuthSession);
        }
      })
      .catch((error: unknown) => {
        setStorageError(
          error instanceof Error ? error.message : 'Could not read secure sign-in data.',
        );
      })
      .finally(() => setIsReady(true));
  }, []);

  const saveSession = useCallback(async (nextSession: AuthSession) => {
    if (Platform.OS === 'web') {
      throw new Error('Secure sign-in storage is available in the iOS and Android apps only.');
    }

    await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
    setStorageError(null);
  }, []);

  const logout = useCallback(async () => {
    if (Platform.OS !== 'web') {
      await SecureStore.deleteItemAsync(SESSION_KEY);
    }
    setSession(null);
    setStorageError(null);
  }, []);

  const value = useMemo(
    () => ({ session, isReady, storageError, saveSession, logout }),
    [session, isReady, storageError, saveSession, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider.');
  }
  return context;
}
