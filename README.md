# java-explore-with-me
Feature: Comments (ветка feature_comments)
Кратко: пользователи могут оставлять комментарии к опубликованным событиям. Комментарии модерируются админами.
Статусы: PENDING → APPROVED | REJECTED.

Публичный API

GET /events/{eventId}/comments?from=0&size=10 — список только APPROVED к PUBLISHED событию.

Пользовательский API

POST /users/{userId}/events/{eventId}/comments → 201 — создать (стартует как PENDING).

PATCH /users/{userId}/comments/{commentId} → 200 — редактировать свой PENDING.

DELETE /users/{userId}/comments/{commentId} → 204 — удалить свой PENDING.

Админский API

GET /admin/comments/pending?from=0&size=20 — очередь модерации.

PATCH /admin/comments/{commentId}/approve → 200 — одобрить.

PATCH /admin/comments/{commentId}/reject[?reason=...] → 200 — отклонить.

DELETE /admin/comments/{commentId} → 204 — удалить.

Правила:

Комментировать можно только опубликованные события.

После APPROVED автор не может редактировать/удалять (даёт 409).

Публично видны только APPROVED.

Postman-коллекция: postman/feature.json
Проверяет: создание/редактирование/модерацию/удаление и коды ответов.

Ссылка на PR: https://github.com/VladimirPlot/explore-with-me/pull/3