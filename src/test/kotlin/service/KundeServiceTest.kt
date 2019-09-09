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

package de.hska.service

import de.hska.kunde.entity.Adresse
import de.hska.kunde.entity.FamilienstandType.LEDIG
import de.hska.kunde.entity.GeschlechtType.WEIBLICH
import de.hska.kunde.entity.InteresseType.LESEN
import de.hska.kunde.entity.InteresseType.REISEN
import de.hska.kunde.entity.Kunde
import de.hska.kunde.entity.Umsatz
import de.hska.kunde.service.KundeService
import java.math.BigDecimal.ONE
import java.net.URL
import java.time.LocalDate
import java.util.Currency
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
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
import org.springframework.util.CollectionUtils.toMultiValueMap

@Tag("service")
@DisplayName("Anwendungskern fuer Kunden testen")
@ExtendWith(SoftAssertionsExtension::class)
@DisabledOnJre(value = [JAVA_8, JAVA_9, JAVA_10, JAVA_11])
@Suppress("ClassName")
class KundeServiceTest {
    private var service = KundeService()

    // -------------------------------------------------------------------------
    // L E S E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Nested
        inner class `Suche anhand der ID` {
            @Test
            @Order(1000)
            fun `Suche mit vorhandener ID`() {
                // arrange
                var id = randomUUID().toString()
                if (id[0] == 'f') {
                    id = id.replaceFirst("f", "1")
                }

                // act
                val kunde = service.findById(id).block()

                // assert
                assertThat(kunde?.id).isEqualTo(id)
            }

            @Test
            @Order(1100)
            fun `Suche mit nicht-vorhandener ID`() {
                // arrange
                val id = "f" + randomUUID().toString().substring(1)

                // act
                val kunde = service.findById(id).block()

                // assert
                assertThat(kunde).isNull()
            }
        }

        @Test
        @Order(2000)
        fun `Suche nach allen Kunden`() {
            // act
            val kunden = service.findAll().collectList().block()

            // act
            assertThat(kunden)
                .isNotNull
                .isNotEmpty
        }

        @ParameterizedTest
        @ValueSource(strings = [NACHNAME])
        @Order(2100)
        fun `Suche mit vorhandenem Nachnamen`(nachname: String) {
            // arrange
            val nachnameLower = nachname.toLowerCase()
            val params =
                toMultiValueMap(mapOf("nachname" to listOf(nachnameLower)))

            // act
            val kunden = service.find(params).collectList().block()

            // assert
            assertThat(kunden)
                .isNotNull
                .isNotEmpty
                .allMatch { kunde -> kunde.nachname.toLowerCase() == nachnameLower }
        }
    }

    // -------------------------------------------------------------------------
    // S C H R E I B E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Schreiben {
        @ParameterizedTest
        @CsvSource(
            "$NEUER_NACHNAME, $NEUE_EMAIL, $NEUES_GEBURTSDATUM, $CURRENCY_CODE, $NEUE_HOMEPAGE, $NEUE_PLZ, $NEUER_ORT"
        )
        @Order(5000)
        fun `Neuanlegen eines neuen Kunden`(args: ArgumentsAccessor, softly: SoftAssertions) {
            // arrange
            val neuerKunde = Kunde(
                id = null,
                nachname = args.get<String>(0),
                email = args.get<String>(1),
                newsletter = true,
                geburtsdatum = args.get<LocalDate>(2),
                umsatz = Umsatz(ONE, Currency.getInstance(args.get<String>(3))),
                homepage = args.get<URL>(4),
                geschlecht = WEIBLICH,
                familienstand = LEDIG,
                interessen = listOf(LESEN, REISEN),
                adresse = Adresse(plz = args.get<String>(5), ort = args.get<String>(6))
            )

            // act
            val kunde = service.create(neuerKunde).block()

            // assert
            with(softly) {
                assertThat(kunde).isNotNull
                assertThat(kunde?.id).isNotNull
                assertThat(kunde?.email).isEqualTo(NEUE_EMAIL)
                assertThat(kunde?.adresse?.plz).isEqualTo(NEUE_PLZ)
            }
        }

        @Test
        @Order(6000)
        fun `Aendern eines vorhandenen Kunden`() {
            // arrange
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("f", "1")
            }
            val kunde = service.findById(id).block()
            assertThat(kunde).isNotNull
            kunde as Kunde
            val kundeUpdated = kunde.copy(nachname = NEUER_NACHNAME)

            // act
            val kundeGeaendert = service.update(kundeUpdated, id).block()

            // assert
            assertThat(kundeGeaendert).isNotNull
        }

        @Test
        @Order(6000)
        fun `Loeschen eines vorhandenen Kunden`() {
            // arrange
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("f", "1")
            }

            // act
            val kunde = service.deleteById(id).block()

            // assert
            assertThat(kunde).isNotNull
        }
    }

    private companion object {
        const val NACHNAME = "Test"
        const val NEUE_PLZ = "12345"
        const val NEUER_ORT = "Testort"
        const val NEUER_NACHNAME = "Neuernachname"
        const val NEUE_EMAIL = "email@test.de"
        const val NEUES_GEBURTSDATUM = "2018-01-01"
        const val CURRENCY_CODE = "EUR"
        const val NEUE_HOMEPAGE = "https://test.de"
    }
}
