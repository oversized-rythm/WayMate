import { router } from 'expo-router';
import { useEffect } from 'react';
import { Text, View } from 'react-native';

import { useAuth } from '@/auth/auth-context';
import {
  AuthColors,
  AuthPage,
  FormMessage,
  PrimaryButton,
  SecondaryButton,
  authStyles,
} from '@/components/auth-ui';

export default function WelcomeScreen() {
  const { isReady, session, storageError } = useAuth();

  useEffect(() => {
    if (isReady && session) {
      router.replace('/home');
    }
  }, [isReady, session]);

  if (!isReady || session) {
    return (
      <View style={styles.loadingScreen}>
        <Text style={styles.loadingText}>WayMate</Text>
      </View>
    );
  }

  return (
    <AuthPage welcome>
      <View style={authStyles.center}>
        <Text style={styles.tagline}>
          Map your journeys, memories and the places that matter.
        </Text>
      </View>
      <FormMessage message={storageError ?? ''} />
      <PrimaryButton title="Create Account" onPress={() => router.push('/verify-email')} />
      <SecondaryButton title="Log In" onPress={() => router.push('/login')} />
    </AuthPage>
  );
}

const styles = {
  loadingScreen: {
    flex: 1,
    alignItems: 'center' as const,
    justifyContent: 'center' as const,
    backgroundColor: AuthColors.cream,
  },
  loadingText: {
    color: AuthColors.green,
    fontSize: 20,
    fontWeight: '600' as const,
  },
  tagline: {
    color: AuthColors.muted,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center' as const,
  },
};
