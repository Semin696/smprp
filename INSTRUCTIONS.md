# SDS Plugin — ResourcePack + OraxenModule

## 📁 Структура

```
sds-merged/
├── Oraxen/                          # Твои конфиги (базовые)
│   ├── items/                       # Предметы (*.yml)
│   ├── glyphs/                      # Глифы (*.yml)
│   ├── pack/                        # Папка ресурспака
│   │   ├── pack.zip                 # Сгенерированный пак
│   │   ├── textures/
│   │   ├── models/
│   │   ├── font/
│   │   └── ...
│   ├── sounds.yml
│   ├── font.yml
│   └── settings.yml
└── src/
```

## 🔧 Команды

| Команда | Что делает |
|---------|-----------|
| `/resourcepack build` | Собирает пак из твоих конфигов (items/, glyphs/, sounds.yml), считает SHA-1, загружает на GitHub, отправляет всем игрокам |
| `/resourcepack send` | Отправляет текущий пак всем онлайн игрокам |
| `/resourcepack info` | Показывает URL и SHA-1 |

## ⚙️ Принцип работы

Без Oraxen плагина на сервере:
1. Плагин читает твои конфиги из папки `Oraxen/`
2. Генерирует модели предметов (`assets/minecraft/models/item/`)
3. Генерирует шрифты из glyphs/*.yml и font.yml
4. Генерирует sounds.json из sounds.yml
5. Копирует все файлы из `Oraxen/pack/` (текстуры, модели, звуки)
6. Упаковывает всё в `pack.zip`
7. Загружает на GitHub Release
8. Рассылает всем игрокам

## 🚀 Использование

1. Положи папку `Oraxen` с конфигами в папку плагина `plugins/mainplug/Oraxen/`
2. Настрой GitHub в `plugins/mainplug/resourcepack.yml`:
```yaml
Pack:
  upload:
    type: github
    github:
      repo: "твой-логин/твой-репозиторий"
      token: "github_personal_access_token"
```
3. Выполни `/resourcepack build`
4. Пак соберётся, загрузится на GitHub и отправится всем игрокам

При заходе новых игроков пак отправляется автоматически (настройка `Pack.dispatch.send_on_join: true`).
