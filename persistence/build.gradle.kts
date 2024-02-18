plugins {
    id("java-library")
}

group = "sk.tuke.meta"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("javax.persistence:persistence-api:1.0.2")
    api("org.xerial:sqlite-jdbc:3.41.2.2")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

}

tasks.test {
    useJUnitPlatform()
}
