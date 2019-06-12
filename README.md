# Пример Android приложения с Backend Driven UI

Пересылка UI от сервера на android клиент идет по web-socket протоколу (undertow/okhttp)

Сделан проброс `onClick` событий. Пересылка `onTextChanged` событий в планах.

#### Серверный компонент с версткой и логикой:

[TodoList.kt](server/src/main/kotlin/y2k/backendui/server/TodoList.kt)

#### Библиотеки для UI:

- [Virtual UI (y2k)](https://github.com/y2k/virtual-ui-research)
- [TEA архтектура для Kotlin (y2k)](https://github.com/y2k/tea)
