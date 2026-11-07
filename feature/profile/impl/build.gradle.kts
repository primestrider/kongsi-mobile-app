plugins {
    id("kongsi.feature.impl")
}

android {
    namespace = "com.primestrider.kongsi.feature.profile.impl"
}

dependencies {
    api(projects.feature.profile.api)
    // Profile links to the payment accounts page of the bill feature.
    implementation(projects.feature.bill.api)
    implementation(projects.core.datastore)
    implementation(projects.core.model)
}
