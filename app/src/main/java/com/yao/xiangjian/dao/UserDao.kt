package com.yao.xiangjian.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yao.xiangjian.entity.User

@Dao
interface UserDao {

    @Insert
    fun insertUser(user: User): Long

    @Update
    fun updateUser(newUser: User)

    @Query("select * from User")
    fun loadAllUsers(): List<User>

    @Query("select * from User where name =:name")
    fun findUserByName(name: String): User

    @Delete
    fun deleteUser(user: User)

    @Query("delete from User where name = :name")
    fun deleteUserByName(name: String): Int
}