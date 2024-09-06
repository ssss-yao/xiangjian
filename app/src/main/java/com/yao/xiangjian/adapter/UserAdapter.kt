package com.yao.xiangjian.adapter

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yao.xiangjian.AppData
import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yao.xiangjian.MainActivity
import com.yao.xiangjian.R
import com.yao.xiangjian.dao.UserDao
import com.yao.xiangjian.database.AppDatabase
import com.yao.xiangjian.entity.User
import com.yao.xiangjian.utils.toast

class UserAdapter(val context: Context, val userList: List<User>): RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.userImage)
        val userName: TextView = view.findViewById(R.id.userName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false)
        val viewHolder = ViewHolder(view)
        var userDao: UserDao= AppDatabase.getDatabase(context).userDao()
        Log.d("UserAdapter", "findUserByName")
        try {
            viewHolder.userImage.setOnClickListener{
                val userByName = userDao.findUserByName(viewHolder.userName.text.toString())
                //电话为空，打视频
                if (userByName.phone.isEmpty()){
                    AppData.updateValue(viewHolder.userName.text.toString())
                    val intent=Intent()
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
                    intent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
                    context.startActivity(intent)
                }else{
                    //电话不为空，打电话
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)!= PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CALL_PHONE),1);
                        }else {
                            val telecomManager:TelecomManager  =context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager;
                            val extras: Bundle =Bundle()
                            extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true);
                            val uri:Uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, userByName.phone, null);
                            telecomManager.placeCall(uri, extras);
                        }
                    }
                }

            }
        }catch (e:Exception){
            "出错了，请删除联系人后重新尝试".toast(context)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.name
        Glide.with(context).load(user.imagePath).into(holder.userImage)
    }
    override fun getItemCount(): Int =userList.size
}