package com.project.uploads.config

import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config {

    @Autowired
    private lateinit var appConfig: AppConfig

    @Bean
    fun s3Client(): AmazonS3 {
        val credentials: AWSCredentials = BasicAWSCredentials(appConfig.accessKey, appConfig.secretKey)
        val clientConfig = ClientConfiguration()
        clientConfig.protocol = Protocol.HTTPS
        return AmazonS3ClientBuilder.standard().apply {
            withCredentials(AWSStaticCredentialsProvider(credentials))
            withClientConfiguration(clientConfig)
            withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(appConfig.endpointUrl, null))
            withPathStyleAccessEnabled(true)
        }.build()
    }

}