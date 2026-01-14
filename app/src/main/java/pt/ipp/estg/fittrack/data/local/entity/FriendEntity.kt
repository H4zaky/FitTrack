package pt.ipp.estg.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "friends",
    primaryKeys = ["ownerUid", "phone"],
    indices = [
        Index(value = ["ownerUid"], name = "index_friends_ownerUid")
    ]
)
data class FriendEntity(
    val ownerUid: String,
    val phone: String,
    val name: String,
    val createdAt: Long,
    val uid: String? = null
)
