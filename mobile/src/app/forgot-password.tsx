import { router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import {
  AuthField,
  AuthHeading,
  AuthPage,
  BackButton,
  FormMessage,
  PrimaryButton,
  SecondaryButton,
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ForgotPasswordScreen() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);

  async function requestReset() {
    const normalizedEmail = email.trim().toLowerCase();
    if (!emailPattern.test(normalizedEmail)) {
      setMessage('Enter a valid email address.');
      setSuccess(false);
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const response = await authApi.forgotPassword(normalizedEmail);
      const confirmation =
        typeof response === 'string' && response.trim()
          ? response.trim()
          : 'Password-reset instructions requested.';
      setMessage(`${confirmation} Check your registered email for the reset token.`);
      setEmail(normalizedEmail);
      setSuccess(true);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not request a password reset.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPage>
      <BackButton onPress={() => router.back()} />
      <AuthHeading
        title="Forgot password?"
        subtitle="Enter your registered email and we’ll send you a password-reset link."
      />
      <View style={authStyles.form}>
        <AuthField
          accessibilityLabel="Email address"
          placeholder="Email"
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
          textContentType="emailAddress"
          returnKeyType="send"
          value={email}
          onChangeText={setEmail}
          onSubmitEditing={requestReset}
        />
        <FormMessage message={message} success={success} />
        <PrimaryButton title="Send Reset Link" loading={loading} onPress={requestReset} />
        {success && (
          <SecondaryButton
            title="I have a reset token"
            onPress={() => router.push({ pathname: '/reset-password', params: { email } })}
          />
        )}
      </View>
    </AuthPage>
  );
}
