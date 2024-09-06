package com.yao.xiangjian.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class User(val name: String,val phone:String, val imagePath: String) {

    @PrimaryKey
    var id: String = UUID.randomUUID().toString()

}