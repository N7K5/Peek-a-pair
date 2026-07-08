plugins {
    id("com.android.application") version "8.7.3" apply false
}

tasks.wrapper {
    gradleVersion = "8.12"
    distributionType = Wrapper.DistributionType.BIN
}
