import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import {
  AuthColors,
  AuthHeading,
  AuthPage,
  BackButton,
  FormMessage,
  PrimaryButton,
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

export default function VerifyOtpScreen() {
  const { email: emailParam } = useLocalSearchParams<{ email?: string }>();
  const email = typeof emailParam === 'string' ? emailParam : '';
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!email) {
      router.replace('/verify-email');
    }
  }, [email]);

  async function verifyOtp() {
    if (!/^\d{6}$/.test(otp)) {
      setMessage('Enter the 6-digit code from your email.');
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const result = await authApi.verifyOtp(email, otp);
      if (!result.verified) {
        throw new Error(result.message || 'That code could not be verified.');
      }
      router.replace({
        pathname: '/register',
        params: { email: result.email || email },
      });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not verify the OTP.');
    } finally {
      setLoading(false);
    }
  }

  async function resendOtp() {
    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const result = await authApi.requestOtp(email);
      setMessage(result.message || 'A new code has been sent.');
      setSuccess(true);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not resend the OTP.');
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
      <AuthHeading title="Enter OTP" subtitle={`We sent a 6-digit OTP to ${email}.`} />
      <View style={authStyles.form}>
        <View style={styles.otpRow}>
          {Array.from({ length: 6 }, (_, index) => (
            <View key={index} style={styles.otpCell}>
              <Text style={styles.otpDigit}>{otp[index] ?? ''}</Text>
            </View>
          ))}
          <TextInput
            accessibilityLabel="6-digit OTP"
            autoFocus
            keyboardType="number-pad"
            maxLength={6}
            onChangeText={(value) => setOtp(value.replace(/\D/g, '').slice(0, 6))}
            style={styles.otpInput}
            textContentType="oneTimeCode"
            value={otp}
          />
        </View>
        <FormMessage message={message} success={success} />
        <PrimaryButton title="Verify" loading={loading} onPress={verifyOtp} />
        <Pressable
          accessibilityRole="button"
          disabled={loading}
          onPress={resendOtp}
          style={({ pressed }) => [styles.resendButton, pressed && styles.pressed]}>
          <Text style={styles.resendText}>Resend OTP</Text>
        </Pressable>
      </View>
    </AuthPage>
  );
}

const styles = StyleSheet.create({
  otpRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 7,
    position: 'relative',
  },
  otpCell: {
    flex: 1,
    minWidth: 0,
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: AuthColors.paper,
    borderWidth: 1,
    borderColor: AuthColors.line,
    borderRadius: 13,
  },
  otpDigit: {
    color: AuthColors.green,
    fontSize: 20,
    fontWeight: '600',
  },
  otpInput: {
    ...StyleSheet.absoluteFill,
    opacity: 0.01,
  },
  resendButton: {
    alignSelf: 'center',
    padding: 8,
  },
  resendText: {
    color: AuthColors.green,
    fontSize: 14,
    fontWeight: '600',
  },
  pressed: {
    opacity: 0.7,
  },
});
