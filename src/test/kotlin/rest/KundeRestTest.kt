/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
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
@file:Suppress("PackageDirectoryMismatch")

package de.hska.kunde.rest

import de.hska.kunde.config.Settings.DEV
import de.hska.kunde.entity.Adresse
import de.hska.kunde.entity.GeschlechtType.WEIBLICH
import de.hska.kunde.entity.InteresseType.LESEN
import de.hska.kunde.entity.InteresseType.REISEN
import de.hska.kunde.entity.InteresseType.SPORT
import de.hska.kunde.entity.Kunde
import de.hska.kunde.entity.Umsatz
import de.hska.kunde.rest.constraints.KundeConstraintViolation
import de.hska.kunde.rest.patch.PatchOperation
import java.math.BigDecimal
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.Currency
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnJre
import org.junit.jupiter.api.condition.JRE.JAVA_10
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_8
import org.junit.jupiter.api.condition.JRE.JAVA_9
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.hateoas.EntityModel
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.bodyWithType
import reactor.kotlin.core.publisher.toMono

@Tag("rest")
@DisplayName("REST-Schnittstelle fuer Kunden testen")
@ExtendWith(SpringExtension::class, SoftAssertionsExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@TestPropertySource(locations = ["/rest-test.properties"])
@DisabledOnJre(value = [JAVA_8, JAVA_9, JAVA_10, JAVA_11])
@Suppress("ClassName")
class KundeRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private var baseUrl = "http://$HOST:$port"
    private var client = WebClient.builder()
        // .filter(basicAuthentication(USERNAME, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        assertThat(ctx.getBean<KundeHandler>()).isNotNull
        assertThat(ctx.getBean<KundeStreamHandler>()).isNotNull
    }

    @Test
    fun `Immer erfolgreich`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(true).isTrue()
    }

    @Test
    @Disabled("Noch nicht fertig")
    fun `Noch nicht fertig`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(false).isFalse()
    }

    // -------------------------------------------------------------------------
    // L E S E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Nested
        inner class `Suche anhand der ID` {
            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(1000)
            fun `Suche mit vorhandener ID`(id: String, softly: SoftAssertions) {
                // act
                val kundeModel = client.get()
                    .uri(ID_PATH, id)
                    .retrieve()
                    .bodyToMono<EntityModel<Kunde>>()
                    .block()

                // assert
                assertThat(kundeModel).isNotNull
                kundeModel as EntityModel<Kunde>
                val kunde = kundeModel.content
                assertThat(kunde).isNotNull
                kunde as Kunde
                with(softly) {
                    assertThat(kunde.nachname).isNotEqualTo("")
                    assertThat(kunde.email).isNotEqualTo("")
                    val selfLink = kundeModel.getLink("self").get().href
                    assertThat(selfLink).endsWith("/$id")
                    assertThat(kunde.id).isNull()
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_INVALID, ID_NICHT_VORHANDEN])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(1100)
            fun `Suche mit syntaktisch ungueltiger oder nicht-vorhandener ID`(id: String) {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .exchange()
                    .block()

                // assert
                assertThat(response?.statusCode()).isEqualTo(NOT_FOUND)
            }
        }

        @Test
        @WithMockUser(USERNAME, roles = [ADMIN])
        @Order(2000)
        fun `Suche nach allen Kunden`() {
            // act
            val kunden = client.get()
                .retrieve()
                .bodyToFlux<Kunde>()
                .collectList()
                .block()

            // assert
            assertThat(kunden)
                .isNotNull
                .isNotEmpty
        }

        @ParameterizedTest
        @ValueSource(strings = [NACHNAME])
        @WithMockUser(USERNAME, roles = [ADMIN])
        @Order(2100)
        fun `Suche mit vorhandenem Nachnamen`(nachname: String) {
            // arrange
            val nachnameLower = nachname.toLowerCase()

            // act
            val kunden = client.get()
                .uri {
                    it.path(KUNDE_PATH)
                        .queryParam(NACHNAME_PARAM, nachnameLower)
                        .build()
                }
                .retrieve()
                .bodyToFlux<Kunde>()
                .collectList()
                .block()

            // assert
            assertThat(kunden)
                .isNotEmpty
                .allMatch { kunde -> kunde.nachname.toLowerCase() == nachnameLower }
        }

        @Test
        @WithMockUser(USERNAME, roles = [ADMIN])
        @Order(2200)
        fun `Suche nach allen Kunden mit Streaming`() {
            // act
            val kunden = client.get()
                .uri("/")
                .accept(TEXT_EVENT_STREAM)
                .exchange()
                .flatMapMany { it.bodyToFlux<Kunde>() }
                .collectList()
                .block()

            // assert
            assertThat(kunden).isNotEmpty
        }
    }

    // -------------------------------------------------------------------------
    // S C H R E I B E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Schreiben {
        @Nested
        inner class Erzeugen {
            @ParameterizedTest
            @CsvSource(
                "$NEUER_NACHNAME, $NEUE_EMAIL, $NEUES_GEBURTSDATUM, $CURRENCY_CODE, $NEUE_HOMEPAGE, $NEUE_PLZ, " +
                    NEUER_ORT
            )
            @Order(5000)
            fun `Neuanlegen eines neuen Kunden`(args: ArgumentsAccessor) {
                // arrange
                val neuerKunde = Kunde(
                    id = null,
                    nachname = args.get<String>(0),
                    email = args.get<String>(1),
                    newsletter = true,
                    geburtsdatum = args.get<LocalDate>(2),
                    umsatz = Umsatz(BigDecimal.ONE, Currency.getInstance(args.get<String>(3))),
                    homepage = args.get<URL>(4),
                    geschlecht = WEIBLICH,
                    familienstand = null,
                    interessen = listOf(LESEN, REISEN),
                    adresse = Adresse(plz = args.get<String>(5), ort = args.get<String>(6))
                )

                // act
                val response = client.post()
                    .bodyWithType(neuerKunde.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(CREATED)
                assertThat(response.headers()).isNotNull
                val location = response.headers().asHttpHeaders().location
                assertThat(location).isNotNull()
                location as URI
                val locationStr = location.toString()
                assertThat(locationStr).isNotEqualTo("")
                val indexLastSlash = locationStr.lastIndexOf('/')
                assertThat(indexLastSlash).isPositive()
                val id = locationStr.substring(indexLastSlash + 1)
                assertThat(id).isNotNull()
            }

            @ParameterizedTest
            @CsvSource(
                "$NEUER_NACHNAME_INVALID, $NEUE_EMAIL_INVALID, " +
                    "$NEUE_PLZ_INVALID, $NEUER_ORT, $NEUES_GEBURTSDATUM"
            )
            @Order(5100)
            fun `Neuanlegen eines neuen Kunden mit ungueltigen Werten`(
                nachname: String,
                email: String,
                plz: String,
                ort: String,
                geburtsdatum: LocalDate
            ) {
                // arrange
                val adresse = Adresse(plz = plz, ort = ort)
                val neuerKunde = Kunde(
                    id = null,
                    nachname = nachname,
                    email = email,
                    newsletter = true,
                    geburtsdatum = geburtsdatum,
                    umsatz = null,
                    homepage = null,
                    geschlecht = WEIBLICH,
                    familienstand = null,
                    interessen = listOf(LESEN, REISEN),
                    adresse = adresse
                )

                // act
                val response = client.post()
                    .bodyWithType(neuerKunde.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                with(response) {
                    assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                    val violations =
                        bodyToFlux<KundeConstraintViolation>().collectList().block()
                    assertThat(violations).isNotNull
                    violations as List<KundeConstraintViolation>
                    assertThat(violations)
                        .hasSize(3)
                        .doesNotHaveDuplicates()
                    val violationMsgPredicate = { msg: String ->
                        msg.contains("ist nicht 5-stellig") ||
                            msg.contains("Bei Nachnamen ist nach einem") ||
                            msg.contains("Die EMail-Adresse")
                    }
                    violations
                        .map { it.message!! }
                        .forEach { msg ->
                            assertThat(msg).matches(violationMsgPredicate)
                        }
                }
            }
        }

        @Nested
        inner class Aendern {
            @ParameterizedTest
            @ValueSource(strings = [ID_UPDATE_PUT])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(6000)
            fun `Aendern eines vorhandenen Kunden durch PUT`(id: String) {
                // arrange
                val kundeOrig = client.get()
                    .uri(ID_PATH, id)
                    .retrieve()
                    .bodyToMono<Kunde>()
                    .block()
                assertThat(kundeOrig).isNotNull
                kundeOrig as Kunde
                val kunde = Kunde(
                    id = null,
                    nachname = kundeOrig.nachname,
                    email = "${kundeOrig.email}put",
                    adresse = kundeOrig.adresse,
                    familienstand = null,
                    geburtsdatum = null,
                    homepage = null,
                    geschlecht = null,
                    interessen = null,
                    kategorie = 0,
                    newsletter = false,
                    umsatz = null
                )

                // act
                val response = client.put()
                    .uri(ID_PATH, id)
                    .bodyWithType(kunde.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
                val hasBody = response.bodyToMono<String>()
                    .hasElement()
                    .block()
                assertThat(hasBody).isFalse()
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PATCH, $NEUE_EMAIL")
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(6100)
            fun `Aendern eines vorhandenen Kunden durch PATCH`(id: String, email: String) {
                // arrange
                val replaceOp = PatchOperation(
                    op = "replace",
                    path = "/email",
                    value = email
                )
                val addOp = PatchOperation(
                    op = "add",
                    path = "/interessen",
                    value = NEUES_INTERESSE.value
                )
                val removeOp = PatchOperation(
                    op = "remove",
                    path = "/interessen",
                    value = ZU_LOESCHENDES_INTERESSE.value
                )
                val operations = listOf(replaceOp, addOp, removeOp)

                // act
                val response = client.patch()
                    .uri(ID_PATH, id)
                    .bodyWithType(operations.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
                val hasBody = response.bodyToMono<String>()
                    .hasElement()
                    .block()
                assertThat(hasBody).isFalse()
            }
        }

        @Nested
        inner class Loeschen {
            @ParameterizedTest
            @ValueSource(strings = [ID_DELETE])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(7000)
            fun `Loeschen eines vorhandenen Kunden`(id: String) {
                // act
                val response = client.delete()
                    .uri(ID_PATH, id)
                    .exchange()
                    .block()

                // assert
                assertThat(response?.statusCode()).isEqualTo(NO_CONTENT)
            }

            @ParameterizedTest
            @ValueSource(strings = [EMAIL_DELETE])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(7100)
            fun `Loeschen eines vorhandenen Kunden mit Emailadresse`(email: String) {
                // act
                val response = client.delete()
                    .uri {
                        it.path(KUNDE_PATH)
                            .queryParam(EMAIL_PARAM, email)
                            .build()
                    }
                    .exchange()
                    .block()

                // assert
                @Suppress("UsePropertyAccessSyntax")
                assertThat(response?.statusCode()).isEqualTo(NO_CONTENT)
            }
        }
    }

    private companion object {
        const val HOST = "localhost"
        const val KUNDE_PATH = "/"
        const val USERNAME = "admin"
        const val ADMIN = "ADMIN"

        const val ID_VORHANDEN = "10000000-0000-0000-0000-000000000001"
        const val ID_INVALID = "10000000-0000-0000-0000-00000000000X"
        const val ID_NICHT_VORHANDEN = "f0000000-0000-0000-0000-000000000001"
        const val ID_UPDATE_PUT = "10000000-0000-0000-0000-000000000002"
        const val ID_UPDATE_PATCH = "10000000-0000-0000-0000-000000000003"
        const val ID_DELETE = "10000000-0000-0000-0000-000000000005"

        const val NACHNAME = "alpha"

        const val NEUE_PLZ = "12345"
        const val NEUER_ORT = "Testort"
        const val NEUER_NACHNAME = "Neuernachname"
        const val NEUE_EMAIL = "email@test.de"
        const val NEUES_GEBURTSDATUM = "2017-01-31"
        const val CURRENCY_CODE = "EUR"
        const val NEUE_HOMEPAGE = "https://test.de"

        const val NEUE_PLZ_INVALID = "1234"
        const val NEUER_NACHNAME_INVALID = "?!$"
        const val NEUE_EMAIL_INVALID = "email@"

        val NEUES_INTERESSE = SPORT
        val ZU_LOESCHENDES_INTERESSE = LESEN

        const val EMAIL_DELETE = "foo@bar.test"

        const val ID_PATH = "/{id}"
        const val NACHNAME_PARAM = "nachname"
        const val EMAIL_PARAM = "email"
    }
}
