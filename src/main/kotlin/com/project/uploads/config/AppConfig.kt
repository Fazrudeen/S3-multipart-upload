package com.project.uploads.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {

    @Value("\${toss.access-key}")
    lateinit var accessKey: String

    @Value("\${toss.secret-key}")
    lateinit var secretKey: String

    @Value("\${toss.url}")
    lateinit var endpointUrl: String

    @Value("\${toss.bucket}")
    lateinit var bucket: String

    @Value("\${publish.image-asset-types}")
    lateinit var imageAssetTypes: List<String>

    @Value("\${publish.content-asset-types}")
    lateinit var contentAssetTypes: List<String>

    var forwardSlash = "/"

}