/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package de.hska.kunde.rest

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.hska.kunde.Router.Companion.idPathVar
import de.hska.kunde.config.logger
import de.hska.kunde.entity.Kunde
import de.hska.kunde.rest.constraints.KundeConstraintViolation
import de.hska.kunde.rest.hateoas.KundeModelAssembler
import de.hska.kunde.rest.patch.InvalidInteresseException
import de.hska.kunde.rest.patch.KundePatcher
import de.hska.kunde.rest.patch.PatchOperation
import de.hska.kunde.service.KundeService
import java.net.URI
import javax.validation.ConstraintViolationException
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.bodyWithType
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.publisher.toMono

/**
 * Eine Handler-Function wird von der Router-Function
 * [de.hska.kunde.Router.router] aufgerufen, nimmt einen Request entgegen
 * und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen KundeHandler mit einem injizierten [KundeService]
 *      erzeugen.
 */
@Component
@Suppress("TooManyFunctions")
class KundeHandler(private val service: KundeService, private val modelAssembler: KundeModelAssembler) {
    /**
     * Suche anhand der Kunde-ID
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und dem gefundenen
     *      Kunden einschließlich HATEOAS-Links, oder aber Statuscode 204.
     */
    fun findById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return service.findById(id)
            .map { modelAssembler.toModel(it, request) }
            .flatMap { ok().bodyWithType(it.toMono()) }
            .switchIfEmpty(notFound().build())
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird
     * `Mono<List<Kunde>>` statt `Flux<Kunde>` zurückgeliefert, damit
     * auch der Statuscode 204 möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein Mono-Objekt mit dem Statuscode 200 und einer Liste mit den
     *      gefundenen Kunden einschließlich HATEOAS-Links, oder aber
     *      Statuscode 204.
     */
    fun find(request: ServerRequest): Mono<ServerResponse> {
        val queryParams = request.queryParams()

        // https://stackoverflow.com/questions/45903813/...
        //     ...webflux-functional-how-to-detect-an-empty-flux-and-return-404
        return service.find(queryParams)
            .map { kunde -> modelAssembler.toModel(kunde, request, false) }
            .collectList()
            .flatMap { kunden ->
                if (kunden.isEmpty()) {
                    notFound().build()
                } else {
                    // genau 1 Treffer bei der Suche anhand der Emailadresse
                    if (queryParams.keys.contains("email")) {
                        ok().bodyWithType(kunden[0].toMono())
                    } else {
                        ok().bodyWithType(kunden.toMono())
                    }
                }
            }
    }

    /**
     * Einen neuen Kunde-Datensatz anlegen.
     * @param request Der eingehende Request mit dem Kunde-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder
     *      Statuscode 400 falls Constraints verletzt sind oder der
     *      JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    fun create(request: ServerRequest) =
        request.bodyToMono<Kunde>()
            .flatMap { service.create(it) }
            .flatMap { kunde ->
                logger.debug("create: {}", kunde)
                val location = URI("${request.uri()}${kunde.id}")
                created(location).build()
            }
            .onErrorResume(ConstraintViolationException::class) {
                // Service-Funktion "create" und Parameter "kunde"
                handleConstraintViolation(it, "create.kunde.")
            }
            .onErrorResume(DecodingException::class) { handleDecodingException(it) }

    // z.B. Service-Funktion "create|update" mit Parameter "kunde" hat dann Meldungen mit "create.kunde.nachname:"
    private fun handleConstraintViolation(exception: ConstraintViolationException, deleteStr: String):
        Mono<ServerResponse> {
        val violations = exception.constraintViolations
        if (violations.isEmpty()) {
            return badRequest().build()
        }

        val kundeViolations = violations.map { violation ->
            KundeConstraintViolation(
                property = violation.propertyPath.toString().replace(deleteStr, ""),
                message = violation.message
            )
        }
        logger.debug("handleConstraintViolation(): {}", kundeViolations)
        return badRequest().bodyWithType(kundeViolations.toMono())
    }

    private fun handleDecodingException(e: DecodingException) = when (val exception = e.cause) {
        is JsonParseException -> {
            logger.debug("handleDecodingException(): JsonParseException={}", exception.message)
            val msg = exception.message ?: ""
            badRequest().bodyWithType(msg.toMono())
        }
        is InvalidFormatException -> {
            logger.debug("handleDecodingException(): InvalidFormatException={}", exception.message)
            val msg = exception.message ?: ""
            badRequest().bodyWithType(msg.toMono())
        }
        else -> status(INTERNAL_SERVER_ERROR).build()
    }

    /**
     * Einen vorhandenen Kunde-Datensatz überschreiben.
     * @param request Der eingehende Request mit dem neuen Kunde-Datensatz im
     *      Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls
     *      Constraints verletzt sind oder der JSON-Datensatz syntaktisch nicht
     *      korrekt ist.
     */
    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return request.bodyToMono<Kunde>()
            .flatMap { service.update(it, id) }
            .flatMap { noContent().build() }
            .switchIfEmpty(notFound().build())
            .onErrorResume(ConstraintViolationException::class) {
                // Service-Funktion "update" und Parameter "kunde"
                handleConstraintViolation(it, "update.kunde.")
            }
            .onErrorResume(DecodingException::class) { handleDecodingException(it) }
    }

    /**
     * Einen vorhandenen Kunde-Datensatz durch PATCH aktualisieren.
     * @param request Der eingehende Request mit dem PATCH-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls
     *      Constraints verletzt sind oder der JSON-Datensatz syntaktisch nicht
     *      korrekt ist.
     */
    @Suppress("LongMethod")
    fun patch(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)

        return request.bodyToFlux<PatchOperation>()
            // Die einzelnen Patch-Operationen als Liste in einem Mono
            .collectList()
            .flatMap { patchOps ->
                service.findById(id)
                    .flatMap {
                        val kundePatched = KundePatcher.patch(it, patchOps)
                        logger.debug("patch(): {}", kundePatched)
                        service.update(kundePatched, id)
                    }
                    .flatMap { noContent().build() }
                    .switchIfEmpty(notFound().build())
            }
            .onErrorResume(ConstraintViolationException::class) {
                // Service-Funktion "update" und Parameter "kunde"
                handleConstraintViolation(it, "update.kunde.")
            }
            .onErrorResume(InvalidInteresseException::class) {
                val msg = it.message
                if (msg == null) {
                    badRequest().build()
                } else {
                    badRequest().bodyWithType(msg.toMono())
                }
            }
            .onErrorResume(DecodingException::class) { handleDecodingException(it) }
    }

    /**
     * Einen vorhandenen Kunden anhand seiner ID löschen.
     * @param request Der eingehende Request mit der ID als Pfad-Parameter.
     * @return Response mit Statuscode 204.
     */
    fun deleteById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return service.deleteById(id).flatMap { noContent().build() }
    }

    /**
     * Einen vorhandenen Kunden anhand seiner Emailadresse löschen.
     * @param request Der eingehende Request mit der Emailadresse als
     *      Query-Parameter.
     * @return Response mit Statuscode 204.
     */
    fun deleteByEmail(request: ServerRequest): Mono<ServerResponse> {
        val email = request.queryParam("email")
        return if (email.isPresent) {
            return service.deleteByEmail(email.get())
                .flatMap { noContent().build() }
        } else {
            notFound().build()
        }
    }

    private companion object {
        val logger by lazy { logger() }
    }
}
