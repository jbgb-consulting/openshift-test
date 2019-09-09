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

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Enum für Interessen. Dazu können auf der Clientseite z.B. Checkboxen
 * realisiert werden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @property value Der interne Wert
 */
enum class InteresseType(val value: String) {
    /**
     * _Sport_ mit dem internen Wert `S` für z.B. das Mapping in einem
     * JSON-Datensatz oder das Abspeichern in einer DB.
     */
    SPORT("S"),
    /**
     * _Lesen_ mit dem internen Wert `L` für z.B. das Mapping in einem
     * JSON-Datensatz oder das Abspeichern in einer DB.
     */
    LESEN("L"),
    /**
     * _Reisen_ mit dem internen Wert `R` für z.B. das Mapping in einem
     * JSON-Datensatz oder das Abspeichern in einer DB.
     */
    REISEN("R");

    /**
     * Einen enum-Wert als String mit dem internen Wert ausgeben. Dieser Wert
     * wird durch Jackson in einem JSON-Datensatz verwendet.
     * [https://github.com/FasterXML/jackson-databind/wiki]
     * @return Interner Wert
     */
    @JsonValue
    override fun toString() = value

    companion object {
        private val nameCache = HashMap<String, InteresseType>().apply {
            enumValues<InteresseType>().forEach {
                put(it.value, it)
                put(it.value.toLowerCase(), it)
                put(it.name, it)
                put(it.name.toLowerCase(), it)
            }
        }

        /**
         * Konvertierung eines Strings in einen Enum-Wert
         * @param value Der String, zu dem ein passender Enum-Wert ermittelt
         * werden soll.
         * Keine Unterscheidung zwischen Gross- und Kleinschreibung.
         * @return Passender Enum-Wert oder null
         */
        fun build(value: String?) = nameCache[value]
    }
}
