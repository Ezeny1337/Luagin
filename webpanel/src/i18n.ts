import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zh from './locales/zh.json';
import en from './locales/en.json';

function getLangFromCookie() {
  const match = document.cookie.match(/(?:^|; )lang=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}
function setLangToCookie(lang: string) {
  document.cookie = `lang=${encodeURIComponent(lang)}; path=/; max-age=31536000`;
}
const lang = getLangFromCookie() || 'zh';
i18n
  .use(initReactI18next)
  .init({
    resources: {
      zh: { translation: zh },
      en: { translation: en }
    },
    lng: lang,
    fallbackLng: 'zh',
    interpolation: { escapeValue: false }
  });
i18n.on('languageChanged', setLangToCookie);

export default i18n;