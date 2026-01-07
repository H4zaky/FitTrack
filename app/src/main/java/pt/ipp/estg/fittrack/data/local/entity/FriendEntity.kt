package pt.ipp.estg.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val phone: String, // chave simples: telefone normalizado
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
