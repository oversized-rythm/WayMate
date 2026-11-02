import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text, View } from 'react-native';

import { useAuth } from '@/auth/auth-context';
import {
  AuthColors,
  AuthHeading,
  AuthPage,
  FormMessage,
  PrimaryButton,
  authStyles,
} from '@/components/auth-ui';

export default function HomeScreen() {
  const { isReady, session, logout } = useAuth();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (isReady && !session) {
      router.replace('/');
    }
  }, [isReady, session]);

  async function signOut() {
    setLoading(true);
    setMessage('');
    try {
      await logout();
      router.replace('/');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not log out securely.');
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
    </AuthPage>
  );
}

const styles = {
  loadingScreen: {
    flex: 1,
    backgroundColor: AuthColors.cream,
  },
};
