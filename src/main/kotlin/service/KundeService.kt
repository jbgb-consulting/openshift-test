@file:Suppress("TooManyFunctions")

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
package de.hska.kunde.service

import de.hska.kunde.config.logger
import de.hska.kunde.entity.Adresse
import de.hska.kunde.entity.FamilienstandType.VERHEIRATET
import de.hska.kunde.entity.GeschlechtType.WEIBLICH
import de.hska.kunde.entity.InteresseType.LESEN
import de.hska.kunde.entity.InteresseType.REISEN
import de.hska.kunde.entity.Kunde
import de.hska.kunde.entity.Umsatz
import java.math.BigDecimal.ONE
import java.net.URL
import java.time.LocalDate
import java.util.Currency.getInstance
import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import javax.validation.Valid
import kotlin.random.Random
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

/**
 * Anwendungslogik für Kunden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
@Validated
class KundeService {
    /**
     * Einen Kunden anhand seiner ID suchen
     * @param id Die Id des gesuchten Kunden
     * @return Der gefundene Kunde oder ein leeres Mono-Objekt
     */
    fun findById(id: String) = if (id[0].toLowerCase() == 'f') {
        logger.debug("findById: kein Kunde gefunden")
        Mono.empty()
    } else {
        val kunde = createKunde(id)
        logger.debug("findById: {}", kunde)
        kunde.toMono()
    }

    private fun findByEmail(email: String): Mono<Kunde> {
        var id = randomUUID().toString()
        if (id[0] == 'f') {
            // damit findById nicht empty() liefert (s.u.)
            id = id.replaceFirst("f", "1")
        }

        return findById(id).flatMap {
            it.copy(email = email)
                .toMono()
                .doOnNext { kunde -> logger.debug("findByEmail: {}", kunde) }
        }
    }

    /**
     * Kunden anhand von Suchkriterien suchen
     * @param queryParams Die Suchkriterien
     * @return Die gefundenen Kunden oder ein leeres Flux-Objekt
     */
    @Suppress("ReturnCount")
    fun find(queryParams: MultiValueMap<String, String>): Flux<Kunde> {
        if (queryParams.isEmpty()) {
            return findAll()
        }

        for ((key, value) in queryParams) {
            // nicht mehrfach das gleiche Suchkriterium, z.B. nachname=Aaa&nachname=Bbb
            if (value.size != 1) {
                return Flux.empty()
            }

            val paramValue = value[0]
            when (key) {
                "email" -> return findByEmail(paramValue).flux()
                "nachname" -> return findByNachname(paramValue)
            }
        }

        return Flux.empty()
    }

    /**
     * Alle Kunden als Flux ermitteln, wie sie später auch von der DB kommen.
     * @return Alle Kunden
     */
    fun findAll(): Flux<Kunde> {
        val kunden = ArrayList<Kunde>(maxKunden)
        repeat(maxKunden) {
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("f", "1")
            }
            val kunde = createKunde(id)
            kunden.add(kunde)
        }
        logger.debug("findAll: {}", kunden)
        return kunden.toFlux()
    }

    @Suppress("ReturnCount", "LongMethod")
    private fun findByNachname(nachname: String): Flux<Kunde> {
        if (nachname == "") {
            return findAll()
        }

        if (nachname[0] == 'Z') {
            return Flux.empty()
        }

        val anzahl = nachname.length
        val kunden = ArrayList<Kunde>(anzahl)
        repeat(anzahl) {
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("f", "1")
            }
            val kunde = createKunde(id, nachname)
            kunden.add(kunde)
        }
        logger.debug("findByNachname: {}", kunden)
        return kunden.toFlux()
    }

    /**
     * Einen neuen Kunden anlegen.
     * @param kunde Das Objekt des neu anzulegenden Kunden.
     * @return Der neu angelegte Kunde mit generierter ID
     */
    fun create(@Valid kunde: Kunde): Mono<Kunde> {
        val neuerKunde = kunde.copy(id = randomUUID().toString())
        logger.debug("create(): {}", neuerKunde)
        return neuerKunde.toMono()
    }

    /**
     * Einen vorhandenen Kunden aktualisieren.
     * @param kunde Das Objekt mit den neuen Daten (ohne ID)
     * @param id ID des zu aktualisierenden Kunden
     * @return Der aktualisierte Kunde oder ein leeres Mono-Objekt, falls
     * es keinen Kunden mit der angegebenen ID gibt
     */
    fun update(@Valid kunde: Kunde, id: String) =
        findById(id)
            .flatMap {
                val kundeMitId = kunde.copy(id = id)
                logger.debug("update(): {}", kundeMitId)
                kundeMitId.toMono()
            }

    /**
     * Einen vorhandenen Kunden löschen.
     * @param kundeId Die ID des zu löschenden Kunden.
     */
    fun deleteById(kundeId: String) = findById(kundeId)

    /**
     * Einen vorhandenen Kunden löschen.
     * @param email Die Email des zu löschenden Kunden.
     */
    fun deleteByEmail(email: String) = findByEmail(email)

    private fun createKunde(id: String) = createKunde(id, nachnamen.random())

    @Suppress("LongMethod")
    private fun createKunde(id: String, nachname: String): Kunde {
        @Suppress("MagicNumber")
        val minusYears = Random.nextLong(1, 60)
        val geburtsdatum = LocalDate.now().minusYears(minusYears)
        val homepage = URL("https://www.hska.de")
        val umsatz = Umsatz(betrag = ONE, waehrung = getInstance(GERMANY))
        val adresse = Adresse(plz = "12345", ort = "Testort")

        return Kunde(
            id = id,
            nachname = nachname,
            email = "$nachname@hska.de",
            newsletter = true,
            geburtsdatum = geburtsdatum,
            umsatz = umsatz,
            homepage = homepage,
            geschlecht = WEIBLICH,
            familienstand = VERHEIRATET,
            interessen = listOf(LESEN, REISEN),
            adresse = adresse
        )
    }

    private companion object {
        const val maxKunden = 8
        val nachnamen = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon")
        val logger by lazy { logger() }
    }
}
