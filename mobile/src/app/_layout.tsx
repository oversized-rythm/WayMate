import { Stack } from 'expo-router';

import { AuthProvider } from '@/auth/auth-context';
import { AuthColors } from '@/components/auth-ui';

export default function RootLayout() {
  return (
    <AuthProvider>
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: AuthColors.cream },
          animation: 'slide_from_right',
        }}>
        <Stack.Screen name="index" />
        <Stack.Screen name="verify-email" />
        <Stack.Screen name="verify-otp" />
        <Stack.Screen name="register" />
        <Stack.Screen name="login" />
        <Stack.Screen name="forgot-password" />
        <Stack.Screen name="reset-password" />
        <Stack.Screen name="home" />
      </Stack>
    </AuthProvider>
  );
}
