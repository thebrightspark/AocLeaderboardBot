# Advent Of Code Discord Bot

This is a Discord bot written in Kotlin using the following core libraries:
- Spring Boot
- Ktor
- Jackson
- JDA

The currently configured leaderboard can be retrieved using `!aoc`.

To get more details for a specific member of the leaderboard, use `!aoc <name>`.

Leaderboards will be cached on retrieval, and will only be lazily refreshed after 15 minutes.

The following properties can be set to configure the bot:
- token
- prefix
- event
- leaderboardId
- sessionCookie

The `event`, `leaderboardId` and `sessionCookie` can all be changed at runtime using the respective commands:
- `!event <value>`
- `!leaderboard <value>`
- `!session <value>`

