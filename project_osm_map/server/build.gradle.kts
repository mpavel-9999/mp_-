// Локальный API над схемой mp_service: читает PostgreSQL и отдает JSON приложению.
// Запуск: MP_DB_PASSWORD=<пароль> gradlew :server:run
plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.4")
}

application {
    mainClass.set("ru.mpavel.osmmap.server.ApiServer")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
