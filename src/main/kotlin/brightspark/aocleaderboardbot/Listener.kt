package brightspark.aocleaderboardbot

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.awt.Color
import java.util.concurrent.TimeUnit

@Component
class Listener(
	private val httpClient: HttpClient,
	private val sessionCookieStorage: SessionCookieStorage,

	@Value("\${prefix:}") private val prefix: String,
	@Value("\${event:}") private var event: String,
	@Value("\${leaderboardId:0}") private var leaderboardId: Int,
	@Value("\${sessionCookie:}") sessionCookie: String
) {
	companion object {
		private val logger = LoggerFactory.getLogger(Listener::class.java)
		private val requestCooldown = TimeUnit.MINUTES.toMillis(15)
		private val mentionRegex = Regex("<@!?(\\d+)>")
	}

	private lateinit var url: String
	private lateinit var webUrl: String
	private var lastRequestTimestamp = 0L
	private lateinit var lastLeaderboard: Leaderboard
	private val lastLeaderboardAge: String
		get() = "${TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastRequestTimestamp)} minutes old"

	init {
		setUrl(event, leaderboardId)
		sessionCookieStorage.set(sessionCookie)
	}

	private fun setUrl(event: String = this.event, leaderboardId: Int = this.leaderboardId) {
		this.event = event
		this.leaderboardId = leaderboardId
		url = "https://adventofcode.com/$event/leaderboard/private/view/$leaderboardId.json"
		webUrl = "https://adventofcode.com/$event/leaderboard/private/view/$leaderboardId"
	}

	private fun getLeaderboard(event: MessageReceivedEvent): Leaderboard? {
		val timeNow = System.currentTimeMillis()
		if (timeNow > lastRequestTimestamp + requestCooldown) {
			try {
				logger.info("Getting leaderboard from $url")
				lastLeaderboard = runBlocking { httpClient.get(url) }
				lastRequestTimestamp = timeNow
				return lastLeaderboard
			} catch (e: Exception) {
				logger.error("Error trying to retrieve leaderboard", e)
				event.replyEmbed {
					setColor(Color.RED)
					setDescription(
						"""
						An error occurred trying to retrieve the leaderboard!
						${e::class.simpleName}
						The session token might be invalid - please try set a new one with `${prefix}session <token>`
					""".trimIndent()
					)
				}
				return null
			}
		} else {
			logger.info("Getting cached leaderboard")
			return lastLeaderboard
		}
	}

	@SubscribeEvent
	fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot)
			return

		val message = event.message.contentRaw
		val selfId = event.jda.selfUser.id
		val command = when {
			message.startsWith(prefix) -> message.substringAfter(prefix)
			message.startsWith("<@") -> {
				mentionRegex.find(message)
					?.takeIf { it.groupValues[1] == selfId }
					?.let {
						message.substring(it.range.last + 1)
					} ?: return
			}
			else -> return
		}.trim()
		logger.info("Command: {}", command)

		val argString = command.substringAfter(" ", "")
		when {
			command.startsWith("aoc") ->
				if (argString.isBlank()) commandLeaderboard(event) else commandLeaderboardMember(event, argString)
			command.startsWith("event") -> commandEvent(event, argString.substringBefore(" "))
			command.startsWith("leaderboard") -> commandLeaderboardId(event, argString.substringBefore(" "))
			command.startsWith("session") -> commandSession(event, argString.substringBefore(" "))
		}
	}

	private fun commandLeaderboard(event: MessageReceivedEvent) {
		logger.info("commandLeaderboard: Start")
		val stopwatch = StopWatch().apply { start("Get") }
		val leaderboard = getLeaderboard(event) ?: return

		stopwatch.stopAndStart("Parse")
		val members = leaderboard.members.values.sortedByDescending { it.localScore }
		val leaderboardList = StringBuilder().apply {
			members.forEachIndexed { i, member ->
				if (i > 0)
					append("\n")
				if (i < 3) {
					when (i) {
						0 -> append(AocLeaderboardBot.emojiGold)
						1 -> append(AocLeaderboardBot.emojiSilver)
						2 -> append(AocLeaderboardBot.emojiBronze)
					}
					append(" ")
				}
				append("**")
				append(i + 1)
				append(")** ")
				append(member.localScore)
				append(" ")
				append(AocLeaderboardBot.emojiStar)
				append(" ")
				append(member.stars)
				append(" - ")
				append(member.displayName)
			}
		}

		stopwatch.stopAndStart("Send")
		event.replyEmbed {
			setTitle("Advent Of Code ${leaderboard.event} Leaderboard", webUrl)
			setDescription(leaderboardList)
			setFooter(lastLeaderboardAge)
		}

		stopwatch.stop()
		logger.info("commandLeaderboard: Finished -> ${stopwatch.toStringMs()}")
	}

	private fun commandLeaderboardMember(event: MessageReceivedEvent, memberName: String) {
		logger.info("commandLeaderboardMember: Start")
		val stopwatch = StopWatch().apply { start("Get") }
		val leaderboard = getLeaderboard(event) ?: return

		stopwatch.stopAndStart("Parse")
		val member = leaderboard.members.values.find { it.name.equals(memberName, true) }
			?: memberName.toIntOrNull()?.let { memberId -> leaderboard.members.values.find { it.id == memberId } }

		stopwatch.stopAndStart("Send")
		member?.run {
			event.replyEmbed {
				setTitle("Advent Of Code ${leaderboard.event} Leaderboard", webUrl)
				setDescription(
					"""
					|**Name:** $displayName
					|**Stars:** $stars
					|**Local Score:** $localScore
					|**Global Score:** $globalScore
					|
					|**Days Completed:**
					|$completionsString
				""".trimMargin()
				)
				setFooter(lastLeaderboardAge)
			}
		} ?: run {
			event.reply("Couldn't find leaderboard member with name '$memberName`!")
		}

		stopwatch.stop()
		logger.info("commandLeaderboardMember: Finished -> ${stopwatch.toStringMs()}")
	}

	private fun commandEvent(event: MessageReceivedEvent, eventName: String) {
		logger.info("commandEvent")
		if (eventName.isBlank())
			event.reply("No event provided!")
		else {
			setUrl(event = eventName)
			lastRequestTimestamp = 0
			logger.info("commandEvent: Set event to $event")
			event.reply("Successfully set event to `${this.event}`")
		}
	}

	private fun commandLeaderboardId(event: MessageReceivedEvent, leaderboardId: String) {
		logger.info("commandEvent")
		if (leaderboardId.isBlank())
			event.reply("No leaderboard ID provided!")
		else {
			leaderboardId.toIntOrNull()?.let {
				setUrl(leaderboardId = it)
				lastRequestTimestamp = 0
				logger.info("commandEvent: Set leaderboard ID to $it")
				event.reply("Successfully set leaderboard ID to `$it`")
			} ?: run {
				event.reply("Provided leaderboard ID `$leaderboardId` is not a valid number!")
			}
		}
	}

	private fun commandSession(event: MessageReceivedEvent, session: String) {
		logger.info("commandSession")
		if (session.isBlank())
			event.reply("No session cookie provided!")
		else {
			sessionCookieStorage.set(session)
			lastRequestTimestamp = 0
			logger.info("commandSession: Set session cookie to $session")
			event.reply("Successfully set session cookie to `${session}`")
		}
	}
}
