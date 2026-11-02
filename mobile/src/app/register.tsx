import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { View } from 'react-native';

import {
  AuthField,
  AuthHeading,
  AuthPage,
  BackButton,
  FormMessage,
  PrimaryButton,
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

export default function RegisterScreen() {
  const { email: emailParam } = useLocalSearchParams<{ email?: string }>();
  const email = typeof emailParam === 'string' ? emailParam : '';
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!email) {
      router.replace('/verify-email');
    }
  }, [email]);

  async function createAccount() {
    const normalizedName = fullName.trim();
    if (normalizedName.length < 2) {
      setMessage('Enter your full name (at least 2 characters).');
      return;
    }
    if (password.length < 8) {
      setMessage('Your password must be at least 8 characters.');
      return;
    }

    setLoading(true);
    setMessage('');
    try {
      await authApi.register(normalizedName, email, password);
      router.replace({ pathname: '/login', params: { email, registered: '1' } });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not create your account.');
    } finally {
      setLoading(false);
    }
  }

  if (!email) {
    return null;
  }

  return (
    <AuthPage>
      <BackButton onPress={() => router.back()} />
      <AuthHeading
        title="Create account"
        subtitle="Your email is verified. Complete your profile to get started."
      />
      <View style={authStyles.form}>
        <AuthField
          accessibilityLabel="Verified email address"
          value={email}
          editable={false}
          autoCapitalize="none"
        />
        <AuthField
          accessibilityLabel="Full name"
          placeholder="Full name"
          value={fullName}
          onChangeText={setFullName}
          autoCapitalize="words"
          autoComplete="name"
          textContentType="name"
          returnKeyType="next"
        />
        <AuthField
          accessibilityLabel="Password"
          placeholder="Password (at least 8 characters)"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          autoCapitalize="none"
          autoComplete="new-password"
          textContentType="newPassword"
          returnKeyType="done"
          onSubmitEditing={createAccount}
        />
        <FormMessage message={message} />
        <PrimaryButton title="Create Account" loading={loading} onPress={createAccount} />
      </View>
    </AuthPage>
  );
}
