# Uxis utility

Android-клиент для прокси (VLESS / VMess / Trojan / Shadowsocks, Reality, TLS, WS, gRPC, HTTPUpgrade, XHTTP) на базе Xray-core.
Стеклянный интерфейс на Jetpack Compose, SVG-иконки, анимации, 6 тем.

## Сборка на GitHub
1. Создайте репозиторий и загрузите содержимое этой папки.
2. Откройте вкладку **Actions** → **Build APK** → **Run workflow** (или просто сделайте push в `main`).
3. Скачайте артефакт `uxis-utility-apk`: `app-arm64-v8a-release.apk` для большинства телефонов, `app-universal-release.apk` подходит всем.

Во время сборки автоматически скачиваются:
- `libv2ray.aar` (Xray-core) из 2dust/AndroidLibXrayLite
- `geoip.dat` / `geosite.dat` из Loyalsoldier/v2ray-rules-dat

### Своя подпись (чтобы обновления ставились поверх)
По умолчанию APK подписывается debug-ключом, который каждый запуск новый. Добавьте секреты в **Settings → Secrets → Actions**:
- `KEYSTORE_BASE64` — `base64 -w0 uxis.jks`
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

Создать ключ: `keytool -genkey -v -keystore uxis.jks -alias uxis -keyalg RSA -keysize 2048 -validity 10000`

## Возможности
- Импорт ссылок из буфера, вставка вручную, подписки по URL (base64 / plain), обновление подписок
- TCP-пинг всех серверов
- Маршрутизация: глобально / LAN напрямую / RU и LAN напрямую, блокировка рекламы, IPv6
- Таймер подключения и скорость загрузки/отдачи
- Темы: Aurora, Ocean, Sunset, Emerald, Midnight, Frost (светлая)
- Логотип: `logo.svg`, иконка приложения — адаптивный vector drawable
