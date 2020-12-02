package brightspark.aocleaderboardbot

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.util.StopWatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun MessageReceivedEvent.replyEmbed(builder: EmbedBuilder.() -> Unit) =
    this.channel.sendMessage(EmbedBuilder().apply(builder).build()).queue()

fun MessageReceivedEvent.reply(message: String) = replyEmbed { setDescription(message) }

fun StopWatch.stopAndStart(taskName: String) {
    this.stop()
    this.start(taskName)
}

fun StopWatch.toStringMs(): String = StringBuilder().apply {
    for (task in taskInfo) {
        append("; [").append(task.taskName).append("] took ").append(TimeUnit.NANOSECONDS.toMillis(task.timeNanos)).append(" ms")
        val percent = (100.0 * task.timeNanos / totalTimeNanos).roundToInt()
        append(" = ").append(percent).append("%")
    }
}.toString()
