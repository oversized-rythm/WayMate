import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { useAuth } from '@/auth/auth-context';
import {
  AuthColors,
  AuthField,
  AuthHeading,
  AuthPage,
  BackButton,
  FormMessage,
  PrimaryButton,
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

export default function LoginScreen() {
  const { email: emailParam, registered } = useLocalSearchParams<{
    email?: string;
    registered?: string;
  }>();
  const [email, setEmail] = useState(typeof emailParam === 'string' ? emailParam : '');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(
    registered === '1' ? 'Account created. Log in to continue.' : '',
  );
  const [success, setSuccess] = useState(registered === '1');
  const { saveSession } = useAuth();

  async function login() {
    const normalizedEmail = email.trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setMessage('Enter a valid email address.');
      setSuccess(false);
      return;
    }
    if (!password) {
      setMessage('Enter your password.');
      setSuccess(false);
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const tokens = await authApi.login(normalizedEmail, password);
      if (!tokens.accessToken || !tokens.refreshToken) {
        throw new Error('The server did not return valid sign-in tokens.');
      }
      await saveSession({ ...tokens, email: normalizedEmail });
      router.replace('/home');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not log in.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPage>
      <BackButton onPress={() => router.back()} />
      <AuthHeading title="Welcome back" subtitle="Log in to continue your journey." />
      <View style={authStyles.form}>
        <AuthField
          accessibilityLabel="Email address"
          placeholder="Email"
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
          textContentType="emailAddress"
          value={email}
          onChangeText={setEmail}
          returnKeyType="next"
        />
        <AuthField
          accessibilityLabel="Password"
          placeholder="Password"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          autoCapitalize="none"
          autoComplete="current-password"
          textContentType="password"
          returnKeyType="done"
          onSubmitEditing={login}
        />
        <Pressable
          accessibilityRole="button"
          onPress={() => {
            setMessage('Password recovery is not available yet.');
            setSuccess(false);
          }}
          style={{ alignSelf: 'flex-end', paddingVertical: 4 }}>
          <Text style={{ color: AuthColors.green, fontSize: 14 }}>Forgot Password?</Text>
        </Pressable>
        <FormMessage message={message} success={success} />
        <PrimaryButton title="Log In" loading={loading} onPress={login} />
      </View>
      <Text style={authStyles.linkText}>
        New to WayMate?{' '}
        <Text
          accessibilityRole="link"
          onPress={() => router.push('/verify-email')}
          style={{ color: AuthColors.green, fontWeight: '700' }}>
          Create Account
        </Text>
      </Text>
    </AuthPage>
  );
}
