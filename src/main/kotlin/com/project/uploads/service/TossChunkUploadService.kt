package com.project.uploads.service

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PartETag
import com.amazonaws.services.s3.model.UploadPartRequest
import com.project.uploads.com.project.uploads.util.ImageHelper
import com.project.uploads.config.AppConfig
import com.project.uploads.model.ChunkUploadsStartRequest
import com.project.uploads.model.ChunkUploadsStartResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

@Service
class TossChunkUploadService(
    @Value("\${toss.min-file-size}") val minFileSize: Long,
    @Value("\${toss.max-file-size}") val maxFileSize: Long,
    private val s3Config: AmazonS3
) {

    @Autowired
    private lateinit var appConfig: AppConfig

    private val dispatcher = Executors.newFixedThreadPool(20).asCoroutineDispatcher()

    companion object {
        private val logger = LoggerFactory.getLogger(TossChunkUploadService::class.java)
        private val CHUNK_DATE_FORMAT = SimpleDateFormat("MMddYYYY")
    }

    suspend fun beginChunkUpload(contentType: String, file: MultipartFile): String {
//        uploadFileValidator(chunkPublishStartRequest);
        val chunkUploadsStartResponse = initiateUpload(contentType, file.size);
        return chunkUploadProcessing(chunkUploadsStartResponse, file);
    }

    suspend fun initiateUpload(contentType: String, fileSize: Long): ChunkUploadsStartResponse {
        val metadata = ObjectMetadata()
        metadata.contentType = contentType
        metadata.contentLength = fileSize
        val assetId = UUID.randomUUID().toString()
        val uploadKey = "FazTestingFolder" + appConfig.forwardSlash +
                "${CHUNK_DATE_FORMAT.format(Date())}" + appConfig.forwardSlash + assetId
        val request = InitiateMultipartUploadRequest(appConfig.bucket, uploadKey, metadata)
            .withCannedACL(CannedAccessControlList.PublicRead);
        val multipartInit = s3Config.initiateMultipartUpload(request)
        return ChunkUploadsStartResponse(
            uploadId = multipartInit.uploadId,
            uploadKey = uploadKey
        )
    }

    suspend fun chunkUploadProcessing(chunkUploadsStartResponse: ChunkUploadsStartResponse, file: MultipartFile): String {
        val partETagList = ArrayList<PartETag>()
        var filePosition: Long = 0
        val contentLength: Long = file.size
        val chunkSize: Long = minFileSize
        var partNumber = 1
        var chunkStartTime = System.currentTimeMillis()
        while (filePosition < contentLength) {
            // Last part can be less than 5 MB. Adjust part size.
            val chunkSize = Math.min(chunkSize, contentLength - filePosition)
            logger.info("Started transferring file :: Part$partNumber size::$chunkSize")
            val uploadRequest: UploadPartRequest = UploadPartRequest()
                .withBucketName(appConfig.bucket)
                .withKey(chunkUploadsStartResponse.uploadKey)
                .withUploadId(chunkUploadsStartResponse.uploadId)
                .withPartNumber(partNumber)
                .withPartSize(chunkSize)
                .withInputStream(file.inputStream)
                .withFileOffset(filePosition)
            logger.info("before runblocking - mainthread ${Thread.currentThread().name} ")

            runBlocking {
                val partEtag = async(dispatcher) {
                    logger.info("Async start ${Thread.currentThread().name} ")
                    uploadPartAsync(uploadRequest)
                }
                partETagList.add(partEtag.await())
            }
            logger.info("continue for next part mainthread ${Thread.currentThread().name} ")
            filePosition += chunkSize
            partNumber++
        }
        val uploadedURL = completeUpload(chunkUploadsStartResponse, partETagList)
        logger.info("Time taken to upload all the chunks for ${chunkUploadsStartResponse.uploadId}: ${System.currentTimeMillis() - chunkStartTime} ms")
        return uploadedURL
    }

    @Async
    fun uploadPartAsync(uploadRequest: UploadPartRequest) : PartETag {
        return s3Config.uploadPart(uploadRequest).partETag
    }

    fun completeUpload(chunkUploadsStartResponse: ChunkUploadsStartResponse, partETags: List<PartETag>): String {
        try {
            val completeRequest = CompleteMultipartUploadRequest(
                appConfig.bucket,
                chunkUploadsStartResponse.uploadKey,
                chunkUploadsStartResponse.uploadId,
                partETags)
            s3Config.completeMultipartUpload(completeRequest)
            return s3Config.getUrl(appConfig.bucket, chunkUploadsStartResponse.uploadKey).toString()
        } catch (e: ExecutionException) {
            e.printStackTrace()
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    suspend fun uploadFileValidator(chunkPublishStartRequest: ChunkUploadsStartRequest) {

        // validate file max limit
        if (chunkPublishStartRequest.fileSize > maxFileSize) {
            throw Exception("chunk size should not be greater than  ${maxFileSize} bytes" );
        }

        // validate file min limit
        if (chunkPublishStartRequest.fileSize < minFileSize) {
            throw Exception("chunk size should be greater than  ${minFileSize} bytes" );
        }

        // Validate allowed content type
        ImageHelper.validContentType(chunkPublishStartRequest.contentType,
            appConfig.contentAssetTypes.union(appConfig.imageAssetTypes))

    }
}
