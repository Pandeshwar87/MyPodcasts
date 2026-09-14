package com.example.mypodcasts.data.local

import androidx.room.TypeConverter
import com.example.mypodcasts.data.model.MediaType
import com.example.mypodcasts.data.model.SourceType

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = try {
        SourceType.valueOf(value)
    } catch (e: Exception) {
        SourceType.OTHER
    }

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = try {
        MediaType.valueOf(value)
    } catch (e: Exception) {
        MediaType.AUDIO
    }
}
