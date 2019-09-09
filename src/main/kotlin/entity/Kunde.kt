/*
 * Copyright (C) 2013 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package de.hska.kunde.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import de.hska.kunde.entity.Kunde.Companion.ID_PATTERN
import de.hska.kunde.entity.Kunde.Companion.NACHNAME_PATTERN
import java.net.URL
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Past
import javax.validation.constraints.Pattern
import org.hibernate.validator.constraints.UniqueElements

/**
 * Unveränderliche Daten eines Kunden. In DDD ist Kunde ist ein _Aggregate Root_.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @property id ID eines Kunden als UUID [ID_PATTERN]].
 * @property nachname Nachname eines Kunden mit einem bestimmten Muster [NACHNAME_PATTERN].
 * @property email Email eines Kunden.
 * @property kategorie Kategorie eines Kunden mit Werten zwischen
 *      [MIN_KATEGORIE] und [MAX_KATEGORIE].
 * @property newsletter Flag, ob es ein Newsletter-Abo gibt.
 * @property geburtsdatum Das Geburtsdatum eines Kunden.
 * @property umsatz Der Umsatz eines Kunden.
 * @property homepage Die Homepage eines Kunden.
 * @property geschlecht Das Geschlecht eines Kunden.
 * @property familienstand Der Familienstand eines Kunden.
 * @property interessen Die Interessen eines Kunden.
 * @property adresse Die Adresse eines Kunden.
 */
@JsonPropertyOrder(
    "nachname", "email", "kategorie", "newsletter", "geburtsdatum",
    "umsatz", "homepage", "geschlecht", "familienstand", "interessen",
    "adresse"
)
data class Kunde(
    @get:Pattern(regexp = ID_PATTERN, message = "{kunde.id.pattern}")
    @JsonIgnore
    val id: String?,

    @get:NotEmpty(message = "{kunde.nachname.notEmpty}")
    @get:Pattern(
        regexp = NACHNAME_PATTERN,
        message = "{kunde.nachname.pattern}"
    )
    val nachname: String,

    @get:Email(message = "{kunde.email.pattern}")
    val email: String,

    @get:Min(value = MIN_KATEGORIE, message = "{kunde.kategorie.min}")
    @get:Max(value = MAX_KATEGORIE, message = "{kunde.kategorie.min}")
    val kategorie: Int = 0,

    val newsletter: Boolean = false,

    @get:Past(message = "{kunde.geburtsdatum.past}")
    // In einer "Data Class": keine Aufbereitung der Konstruktor-Argumente
    // @JsonFormat(shape = STRING)
    // @field:JsonDeserialize(using = DateDeserializer.class)
    val geburtsdatum: LocalDate?,

    val umsatz: Umsatz?,

    val homepage: URL?,

    val geschlecht: GeschlechtType?,

    val familienstand: FamilienstandType?,

    @get:UniqueElements(message = "{kunde.interessen.uniqueElements}")
    val interessen: List<InteresseType>?,

    @get:Valid
    val adresse: Adresse
) {
    /**
     * Vergleich mit einem anderen Objekt oder null.
     * @param other Das zu vergleichende Objekt oder null
     * @return True, falls das zu vergleichende (Kunde-) Objekt die gleiche
     *      Emailadresse hat.
     */
    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Kunde
        return email == other.email
    }

    /**
     * Hashwert aufgrund der Emailadresse.
     * @return Der Hashwert.
     */
    override fun hashCode() = email.hashCode()

    companion object {
        private const val HEX_PATTERN = "[\\dA-Fa-f]"

        /**
         * Muster für eine UUID.
         */
        const val ID_PATTERN =
            "$HEX_PATTERN{8}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-" +
                "$HEX_PATTERN{4}-$HEX_PATTERN{12}"

        private const val NACHNAME_PREFIX = "o'|von|von der|von und zu|van"

        private const val NAME_PATTERN = "[A-ZÄÖÜ][a-zäöüß]+"

        /**
         * Muster für einen Nachnamen
         */
        const val NACHNAME_PATTERN =
            "($NACHNAME_PREFIX)?$NAME_PATTERN(-$NAME_PATTERN)?"

        /**
         * Maximaler Wert für eine Kategorie
         */
        const val MIN_KATEGORIE = 0L

        /**
         * Minimaler Wert für eine Kategorie
         */
        const val MAX_KATEGORIE = 9L
    }
}
