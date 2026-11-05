import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Alert, Pressable, Text, View } from 'react-native';

import { useAuth } from '@/auth/auth-context';
import {
  AuthColors,
  AuthHeading,
  AuthPage,
  FormMessage,
  PrimaryButton,
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

export default function HomeScreen() {
  const { isReady, session, saveSession, logout } = useAuth();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const deletionInProgress = useRef(false);

  useEffect(() => {
    if (isReady && !session && !deletionInProgress.current) {
      router.replace('/');
    }
  }, [isReady, session]);

  async function signOut() {
    setLoading(true);
    setMessage('');
    try {
      if (session) {
        await authApi.logout(session, async (tokens) => {
          await saveSession({ ...tokens, email: session.email });
        });
      }
      await logout();
      router.replace('/');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not log out securely.');
    } finally {
      setLoading(false);
    }
  }

  function confirmAccountDeletion() {
    Alert.alert(
      'Delete account permanently?',
      'This action is permanent and cannot be undone. Your account and its data will be deleted.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete Account',
          style: 'destructive',
          onPress: () => {
            void deleteAccount();
          },
        },
      ],
      { cancelable: true },
    );
  }

  async function deleteAccount() {
    if (!session) {
      return;
    }

    setLoading(true);
    setMessage('');
    try {
      await authApi.deleteAccount(session, async (tokens) => {
        await saveSession({ ...tokens, email: session.email });
      });
      deletionInProgress.current = true;
      await logout();
      router.replace('/login');
    } catch (error) {
      deletionInProgress.current = false;
      setMessage(error instanceof Error ? error.message : 'Could not delete the account.');
    } finally {
      setLoading(false);
    }
  }

  if (!isReady || !session) {
    return <View style={styles.loadingScreen} />;
  }

  return (
    <AuthPage welcome>
      <AuthHeading title="Welcome to WayMate" subtitle={`Signed in as ${session.email}`} />
      <Text style={authStyles.center}>
        Your next journey starts here.
      </Text>
      <FormMessage message={message} />
      <PrimaryButton title="Log Out" loading={loading} onPress={signOut} />
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ disabled: loading }}
        disabled={loading}
        onPress={confirmAccountDeletion}
        style={({ pressed }) => [styles.deleteButton, pressed && styles.pressed]}>
        <Text style={styles.deleteText}>Delete Account</Text>
      </Pressable>
    </AuthPage>
  );
}

const styles = {
  loadingScreen: {
    flex: 1,
    backgroundColor: AuthColors.cream,
  },
  deleteButton: {
    alignItems: 'center' as const,
    paddingVertical: 12,
  },
  deleteText: {
    color: AuthColors.error,
    fontSize: 15,
    fontWeight: '600' as const,
  },
  pressed: {
    opacity: 0.7,
  },
};
