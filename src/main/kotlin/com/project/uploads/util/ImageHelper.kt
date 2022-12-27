package com.project.uploads.com.project.uploads.util

object ImageHelper {
    fun validContentType(target: String?, contentTypes: Set<String>) {
        val isValidContentType = contentTypes.any {
            it.lowercase().equals(target?.lowercase(), true)
        }
        if (!isValidContentType) {
            throw Exception("Invalid content type $target. Valid content types are: $contentTypes")
        }
    }
}