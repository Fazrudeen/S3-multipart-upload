package com.project.uploads.model

data class ChunkUploadsStartRequest(
    val contentType: String,
    val fileSize: Long
)

data class ChunkUploadsStartResponse(
    val uploadId: String,
    val uploadKey: String
)