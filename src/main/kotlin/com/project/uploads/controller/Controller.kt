package com.project.uploads.controller

import com.project.uploads.service.TossTransferManagerService
import com.project.uploads.service.TossChunkUploadService
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("toss_uploads_poc/v1")
class ChunkUploadsController(private val tossChunkUploadService: TossChunkUploadService,
                             private val tossTransferManagerService: TossTransferManagerService
) {


    @PostMapping(value = ["/part_upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun ChunkUpload(
        @RequestParam("file_type") contentType: String,
        @RequestParam("file") multipartFile: MultipartFile,
        @RequestParam("key") key: String
    ): String {
        return runBlocking { tossChunkUploadService.beginChunkUpload(contentType, multipartFile) }
    }

    @PostMapping(value = ["/full_upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun uploadFile(@RequestParam("file") multipartFile: MultipartFile,
                   @RequestParam("key") key: String): Boolean {
        val file = multipartFile.bytes
        val fileName = multipartFile.originalFilename
        return runBlocking { tossTransferManagerService.uploadFile(file, fileName) }
    }

    @PostMapping(value = ["/update_metadata"])
    fun updateMetadata(@RequestParam("upload_key") uploadKey: String,
                       @RequestParam("key") key: String): Boolean {
        return runBlocking { tossTransferManagerService.updateMetadata(uploadKey) }
    }
}