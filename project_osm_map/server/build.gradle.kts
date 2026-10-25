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
    implementation("com.graphhopper:graphhopper-core:10.2")   // маршруты по дорогам OSM (такси)
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

application {
    mainClass.set("ru.mpavel.osmmap.server.ApiServer")
    applicationDefaultJvmArgs = listOf("-Xmx3g", "-Dfile.encoding=UTF-8")
}

// Каталог запуска - модуль server/: относительные пути data/... указывают на server/data
tasks.named<JavaExec>("run") {
    workingDir = projectDir
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
