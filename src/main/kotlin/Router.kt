/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.hska.kunde

import de.hska.kunde.entity.Kunde
import de.hska.kunde.html.HtmlHandler
import de.hska.kunde.rest.KundeHandler
import de.hska.kunde.rest.KundeStreamHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.router

/**
 * Interface, um das Routing mit _Spring WebFlux_ funktional zu konfigurieren.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface Router {
    /**
     * Bean-Function, um das Routing mit _Spring WebFlux_ funktional zu
     * konfigurieren.
     *
     * @param handler Objekt der Handler-Klasse [KundeHandler] zur Behandlung
     *      von Requests.
     * @param streamHandler Objekt der Handler-Klasse [KundeStreamHandler]
     *      zur Behandlung von Requests mit Streaming.
     * @param htmlHandler Objekt der Handler-Klasse [HtmlHandler]
     *      um HTML-Seiten durch ThymeLeaf bereitzustellen.
     * @return Die konfigurierte Router-Function.
     */
    @Bean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection", "LongMethod")
    fun router(
        handler: KundeHandler,
        streamHandler: KundeStreamHandler,
        htmlHandler: HtmlHandler
    ) = router {
        // https://github.com/spring-projects/spring-framework/blob/master/...
        //       ..spring-webflux/src/main/kotlin/org/springframework/web/...
        //       ...reactive/function/server/RouterFunctionDsl.kt
        "/".nest {
            accept(APPLICATION_JSON).nest {
                GET("/", handler::find)
                GET("/$idPathPattern", handler::findById)
            }

            contentType(APPLICATION_JSON).nest {
                POST("/", handler::create)
                PUT("/$idPathPattern", handler::update)
                PATCH("/$idPathPattern", handler::patch)
            }

            DELETE("/$idPathPattern", handler::deleteById)
            DELETE("/", handler::deleteByEmail)

            accept(TEXT_EVENT_STREAM).nest {
                GET("/", streamHandler::findAll)
            }

            accept(TEXT_HTML).nest {
                GET("/home", htmlHandler::home)
                GET("/suche", htmlHandler::find)
                GET("/details", htmlHandler::details)
            }
        }
    }

    companion object {
        /**
         * Name der Pfadvariablen für IDs.
         */
        const val idPathVar = "id"

        private const val idPathPattern = "{$idPathVar:${Kunde.ID_PATTERN}}"
    }
}
