// Kotlin встроен в AGP 9; версия поднята до 2.4.21: текущие библиотеки собраны Kotlin 2.4,
// а встроенный 2.2.10 их метаданные не читает (план Э3, docs/v0.7/plans/E3_app.md)
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
