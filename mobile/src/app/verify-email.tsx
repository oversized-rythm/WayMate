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
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function VerifyEmailScreen() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  async function sendOtp() {
    const normalizedEmail = email.trim().toLowerCase();
    if (!emailPattern.test(normalizedEmail)) {
      setMessage('Enter a valid email address to continue.');
      return;
    }

    setLoading(true);
    setMessage('');
    try {
      await authApi.requestOtp(normalizedEmail);
      router.push({ pathname: '/verify-otp', params: { email: normalizedEmail } });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not send the OTP.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPage>
      <BackButton onPress={() => router.back()} />
      <AuthHeading
        title="Verify your email"
        subtitle="We’ll send a 6-digit OTP to your email to continue."
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
          onSubmitEditing={sendOtp}
        />
        <FormMessage message={message} />
        <PrimaryButton title="Send OTP" loading={loading} onPress={sendOtp} />
      </View>
    </AuthPage>
  );
}
