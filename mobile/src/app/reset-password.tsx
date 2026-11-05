import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

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

export default function ResetPasswordScreen() {
  const { token: tokenParam } = useLocalSearchParams<{ token?: string }>();
  const [token, setToken] = useState(typeof tokenParam === 'string' ? tokenParam : '');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (typeof tokenParam === 'string') {
      setToken(tokenParam);
      setMessage('');
      setSuccess(false);
    }
  }, [tokenParam]);

  async function resetPassword() {
    const normalizedToken = token.trim();
    if (!normalizedToken) {
      setMessage('This reset link is missing its token. Request a new reset link.');
      setSuccess(false);
      return;
    }
    if (newPassword.length < 8) {
      setMessage('Your new password must be at least 8 characters.');
      setSuccess(false);
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const response = await authApi.resetPassword(normalizedToken, newPassword);
      setMessage(typeof response === 'string' ? response : 'Your password has been reset.');
      setSuccess(true);
      setNewPassword('');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not reset your password.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPage>
      <BackButton onPress={() => router.back()} />
      <AuthHeading
        title="Reset password"
        subtitle="Enter your reset token and new password."
      />
      <View style={authStyles.form}>
        <AuthField
          accessibilityLabel="Password reset token"
          placeholder="Enter reset token"
          autoCapitalize="none"
          autoComplete="off"
          value={token}
          onChangeText={setToken}
        />
        <AuthField
          accessibilityLabel="New password"
          placeholder="Enter new password"
          secureTextEntry
          autoCapitalize="none"
          autoComplete="new-password"
          textContentType="newPassword"
          value={newPassword}
          onChangeText={setNewPassword}
          onSubmitEditing={resetPassword}
        />
        <FormMessage message={message} success={success} />
        {success ? (
          <Pressable
            accessibilityRole="button"
            onPress={() => router.replace('/login')}
            style={styles.loginLink}>
            <Text style={styles.loginLinkText}>Return to Log In</Text>
          </Pressable>
        ) : (
          <PrimaryButton title="Reset Password" loading={loading} onPress={resetPassword} />
        )}
      </View>
    </AuthPage>
  );
}

const styles = {
  loginLink: {
    alignItems: 'center' as const,
    paddingVertical: 12,
  },
  loginLinkText: {
    color: AuthColors.green,
    fontSize: 15,
    fontWeight: '600' as const,
  },
};
