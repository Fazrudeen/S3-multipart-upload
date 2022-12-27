package com.project.uploads.service

import com.amazonaws.AmazonClientException
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.*
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.transfer.Transfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.transfer.Upload
import com.project.uploads.config.AppConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.concurrent.*


@Service
class TossTransferManagerService(
    @Value("\${toss.min-file-size}") val minFileSize: Long,
    @Value("\${toss.max-file-size}") val maxFileSize: Long,
    private val s3Config: AmazonS3
) {

    @Autowired
    private lateinit var appConfig: AppConfig

    companion object {
        private val logger = LoggerFactory.getLogger(TossTransferManagerService::class.java)
    }

    fun uploadFile(document: ByteArray?, fileName: String?): Boolean {
        return try {
            logger.info("Uploading file: {}", fileName)
            val metadata = ObjectMetadata()
            metadata.addUserMetadata("tennent_name", "Pipeline")
            metadata.addUserMetadata("size", document?.size.toString())
            val inputStream = ByteArrayInputStream(document)
            val transferManager: TransferManager = buildTransferManager(s3Config)
            val progressListener = ProgressListener { progressEvent ->
                logger.info(
                    "Transferred bytes: " + progressEvent.bytesTransferred)
            }
            var startTime = System.currentTimeMillis()
            val request = PutObjectRequest(
                appConfig.bucket, fileName,
                inputStream, metadata)
            request.generalProgressListener = progressListener
            val multipleFileUpload: Upload = transferManager.upload(
                request.withCannedAcl(CannedAccessControlList.PublicRead));
            multipleFileUpload.waitForCompletion()
            logger.info("Time taken to upload for upload file ${fileName}: ${System.currentTimeMillis() - startTime} ms")
            multipleFileUpload.state === Transfer.TransferState.Completed
        } catch (e: AmazonClientException) {
            val msg = String.format("Exception occurred while uploading file to TOSS Server for key:%s", fileName)
            logger.error(msg, e)
            throw Exception(msg, e)
        } catch (e: InterruptedException) {
            val msg = String.format("Exception occurred while uploading file to TOSS Server for key:%s", fileName)
            logger.error(msg, e)
            throw Exception(msg, e)
        }
    }

    fun buildTransferManager(s3Client: AmazonS3?): TransferManager {
        return  TransferManagerBuilder.standard()
            .withS3Client(s3Client)
            .withMultipartUploadThreshold((5 * 1024 * 1025).toLong())
            .withExecutorFactory { Executors.newFixedThreadPool(20) }
            .build()
    }

    fun updateMetadata(uploadKey: String?): Boolean {
        //List all the objects in bucket. getObject can be directly called
        val objectListing: List<S3ObjectSummary> = s3Config.listObjectsV2(appConfig.bucket).objectSummaries
        val singleObject: S3ObjectSummary = objectListing.filter { it.key == uploadKey }[0]
        val currentObject: S3Object = s3Config.getObject(appConfig.bucket, singleObject.key)
        val currentObjectMetadata: ObjectMetadata = currentObject.objectMetadata
        currentObjectMetadata.addUserMetadata("uploader", "fazrudeen"); // it will insert if metadata key not found and update if key found
        val request = PutObjectRequest(appConfig.bucket, uploadKey, currentObject.objectContent, currentObjectMetadata)
        s3Config.putObject(request)
        return true;
    }
}