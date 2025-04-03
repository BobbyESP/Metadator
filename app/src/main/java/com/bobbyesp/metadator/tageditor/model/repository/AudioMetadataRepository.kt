package com.bobbyesp.metadator.tageditor.model.repository

import com.kyant.taglib.AudioProperties
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Picture
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.Metadata

interface AudioMetadataRepository {
    suspend fun getMetadata(path: String): Result<Metadata>
    suspend fun getAudioProperties(path: String, style: AudioPropertiesReadStyle): Result<AudioProperties>
    suspend fun writePropertyMap(path: String, propertyMap: PropertyMap): Result<Boolean>
    suspend fun writePictures(path: String, pictures: List<Picture>): Result<Boolean>
}