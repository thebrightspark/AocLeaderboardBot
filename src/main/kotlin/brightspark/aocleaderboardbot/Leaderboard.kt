package brightspark.aocleaderboardbot

import java.time.*
import java.time.temporal.ChronoUnit

data class Leaderboard(
	val ownerId: Int,
	val event: String,
	val members: Map<Int, Member>
) {
	init {
		members.values.forEach { it.event = event }
	}
}

data class Member(
	val id: Int,
	val name: String?,
	val stars: Int,
	val lastStarTS: Long,
	val localScore: Int,
	val globalScore: Int,
	val completionDayLevel: Map<Int, Map<Int, Part>>
) {
	lateinit var event: String

	val eventStartDate: OffsetDateTime by lazy {
		OffsetDateTime.of(event.toInt(), Month.DECEMBER.value, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5))
	}

	val displayName: String by lazy { name ?: "(anonymous user #$id)" }

	val completionsString: String by lazy {
		completionDayLevel.entries
			.sortedBy { it.key }
			.joinToString("\n") { entry ->
				val sb = StringBuilder().append("**").append(entry.key).append(")** ")
				// Stars
				repeat(entry.value.size) { sb.append(AocLeaderboardBot.emojiStar) }
				// Completion time
				val dayStartDate = eventStartDate.plusDays(entry.key.toLong() - 1)
				entry.value.entries.maxByOrNull { it.key }?.let {
					val partCompletionDate =
						LocalDateTime.ofEpochSecond(it.value.getStarTs, 0, ZoneOffset.UTC).atOffset(ZoneOffset.UTC)
					val seconds = dayStartDate.until(partCompletionDate, ChronoUnit.SECONDS)
					sb.append(" ").append(formatSeconds(seconds))
				}
				return@joinToString sb.toString()
			}
	}

	private fun formatSeconds(secondsIn: Long): String {
		val list = ArrayList<Pair<Long, String>>(3).apply {
			(secondsIn / 86400).takeIf { it > 0 }?.let { add(it to "d") }
			((secondsIn % 86400) / 3600).takeIf { it > 0 }?.let { add(it to "h") }
			((secondsIn % 3600) / 60).takeIf { it > 0 }?.let { add(it to "m") }
			(secondsIn % 60).takeIf { it > 0 }?.let { add(it to "s") }
		}
		return when (list.size) {
			0 -> "0s"
			1 -> list[0].let { "${it.first}${it.second}" }
			else -> list.joinToString(" ") { it.let { "${it.first}${it.second}" } }
		}
	}
}

data class Part(val getStarTs: Long)
