package com.forcetech.jmfitapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "productos")
data class Producto(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,

    val marca: String,

    val precio: BigDecimal,

    val objetivo: String,

    val presentacion: String,

    val activo: Boolean = true

)
