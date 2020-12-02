package brightspark.aocleaderboardbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AocLeaderboardBot {
    companion object {
        const val emojiStar = "⭐"
        const val emojiGold = "\uD83E\uDD47"
        const val emojiSilver = "\uD83E\uDD48"
        const val emojiBronze = "\uD83E\uDD49"
    }
}

fun main(args: Array<String>) {
    runApplication<AocLeaderboardBot>(*args)
}
