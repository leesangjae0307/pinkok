import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'pinkok.tokens';

export const tokenStore = {
  accessToken: null as string | null,
  refreshToken: null as string | null,

  /** 저장된 토큰을 메모리로 올린다. 로그인 유지 가능 여부를 반환한다. */
  async load(): Promise<boolean> {
    const raw = await AsyncStorage.getItem(KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as { accessToken: string | null; refreshToken: string | null };
      this.accessToken = parsed.accessToken;
      this.refreshToken = parsed.refreshToken;
    }
    return !!this.refreshToken;
  },

  async save(accessToken: string, refreshToken: string): Promise<void> {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    await AsyncStorage.setItem(KEY, JSON.stringify({ accessToken, refreshToken }));
  },

  async clear(): Promise<void> {
    this.accessToken = null;
    this.refreshToken = null;
    await AsyncStorage.removeItem(KEY);
  },
};
