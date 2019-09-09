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
package de.hska.kunde.html

import de.hska.kunde.service.KundeService
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Component
import org.springframework.ui.ConcurrentModel
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable
import reactor.core.publisher.Mono

/**
 * Eine Handler-Function wird von der Router-Function
 * [de.hska.kunde.Router.router] aufgerufen, nimmt einen Request entgegen
 * und erstellt den HTML-Response durch den Aufruf der Funktion `render`.
 * Die Daten werden an die (HTML-) _View_ durch ein (Concurrent-) _Model_
 * weitergegeben.
 *
 * Alternativen zu ThymeLeaf sind z.B. Mustache oder FreeMarker.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen HtmlHandler mit einem injizierten [KundeService] erzeugen.
 */
@Component
class HtmlHandler(private val service: KundeService) {
    /**
     * Startseite anzeigen
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und der eigentlichen Startseite.
     */
    fun home(request: ServerRequest) =
        ServerResponse.ok().contentType(TEXT_HTML).render("index")

    /**
     * Alle Kunden anzeigen
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und der Resultatseite.
     */
    fun find(request: ServerRequest): Mono<ServerResponse> {
        val kunden = ConcurrentModel()
            .addAttribute(
                "kunden",
                ReactiveDataDriverContextVariable(service.findAll(), 1)
            )

        return ServerResponse.ok().contentType(TEXT_HTML).render("suche", kunden)
    }

    /**
     * Einen Kunden zu einer gegebenen ID (als Query-Parameter) anzeigen
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und der HTML-Seite mit den Kundendaten.
     */
    fun details(request: ServerRequest): Mono<ServerResponse> {
        val kunde = ConcurrentModel()
        request.queryParam("id").ifPresent {
            kunde.addAttribute("kunde", service.findById(it))
        }

        return ServerResponse.ok().contentType(TEXT_HTML).render("details", kunde)
    }
}
