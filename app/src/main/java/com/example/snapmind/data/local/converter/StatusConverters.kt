package com.example.snapmind.data.local.converter

import androidx.room.TypeConverter
import com.example.snapmind.data.local.entity.GeminiMemoStatus
import com.example.snapmind.data.local.entity.OptionalRemoteProcessingStatus
import com.example.snapmind.data.local.entity.StandardProcessingStatus
import com.example.snapmind.data.local.entity.TagAssignedBy

class StatusConverters {

    @TypeConverter
    fun fromStandardProcessingStatus(value: StandardProcessingStatus): String = value.name

    @TypeConverter
    fun toStandardProcessingStatus(value: String): StandardProcessingStatus =
        StandardProcessingStatus.valueOf(value)

    @TypeConverter
    fun fromOptionalRemoteProcessingStatus(value: OptionalRemoteProcessingStatus): String = value.name

    @TypeConverter
    fun toOptionalRemoteProcessingStatus(value: String): OptionalRemoteProcessingStatus =
        OptionalRemoteProcessingStatus.valueOf(value)

    @TypeConverter
    fun fromGeminiMemoStatus(value: GeminiMemoStatus): String = value.name

    @TypeConverter
    fun toGeminiMemoStatus(value: String): GeminiMemoStatus = GeminiMemoStatus.valueOf(value)

    @TypeConverter
    fun fromTagAssignedBy(value: TagAssignedBy): String = value.name

    @TypeConverter
    fun toTagAssignedBy(value: String): TagAssignedBy = TagAssignedBy.valueOf(value)
}
