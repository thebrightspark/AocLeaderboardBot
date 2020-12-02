package brightspark.aocleaderboardbot

data class Leaderboard(
	val ownerId: Int,
	val event: String,
	val members: Map<Int, Member>
)

data class Member(
	val id: Int,
	val name: String?,
	val stars: Int,
	val lastStarTS: Long,
	val localScore: Int,
	val globalScore: Int,
	val completionDayLevel: Map<Int, Map<Int, Part>>
) {
	val displayName: String by lazy { name ?: "(anonymous user #$id)" }

	val completionsString: String by lazy {
		completionDayLevel.entries
			.sortedBy { it.key }
			.joinToString("\n") {
				"**${it.key})** ${StringBuilder().apply { repeat(it.value.size) { append(AocLeaderboardBot.emojiStar) } }}"
			}
	}
}

data class Part(
	val getStarTs: Long
)
