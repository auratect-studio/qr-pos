package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ProductItem

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val category: String,
    val stock: Int = 99
) {
    fun toModel(): ProductItem = ProductItem(
        id = id,
        name = name,
        price = price,
        category = category,
        stock = stock
    )

    companion object {
        fun fromModel(model: ProductItem): ProductEntity = ProductEntity(
            id = model.id,
            name = model.name,
            price = model.price,
            category = model.category,
            stock = model.stock
        )
    }
}