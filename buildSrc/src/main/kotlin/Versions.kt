/*
 * Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
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


@Suppress("unused", "KDocMissingDocumentation", "MemberVisibilityCanBePrivate")
object Versions {
    const val kotlin = "1.3.50-eap-86"
    const val springBoot = "2.2.0.M5"

    object Plugins {
        const val kotlin = Versions.kotlin
        const val allOpen = Versions.kotlin
        const val noArg = Versions.kotlin
        const val kapt = Versions.kotlin

        const val springBoot = Versions.springBoot
        const val testLogger = "1.7.0"
    }

    const val annotations = "17.0.0"
    const val paranamer = "2.8"
    const val springSecurityRsa = "1.0.8.RELEASE"

    const val springCloud = "Hoxton.M2"
    const val hibernateValidator = "6.1.0.Alpha6"
    const val jackson = "2.10.0.pr1"
    const val reactiveStreams = "1.0.3-RC1"
    const val tomcat = "9.0.24"

    const val mockk = "1.9.3"
}
