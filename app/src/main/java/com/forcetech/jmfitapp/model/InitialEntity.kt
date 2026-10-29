package com.forcetech.jmfitapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "initial_entity")
data class InitialEntity(
    @PrimaryKey
    val id: Int = 1
)
