package brightspark.aocleaderboardbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.AnnotatedEventManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Config {
	@Bean
	fun jda(
        listener: Listener,
        @Value("\${token}") token: String
    ): JDA = JDABuilder.createLight(token)
		.setEventManager(AnnotatedEventManager())
		.addEventListeners(listener)
		.build()

	@Bean
	fun objectMapper(): ObjectMapper = jacksonMapperBuilder()
		.propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
		.build()

	@Bean
	fun httpClient(
		objectMapper: ObjectMapper,
		sessionCookieStorage: SessionCookieStorage
	): HttpClient = HttpClient(CIO) {
		install(JsonFeature) {
			serializer = JacksonSerializer(jackson = objectMapper)
		}
		install(HttpCookies) {
			storage = sessionCookieStorage
		}
	}
}
