package com.yao.xiangjian

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.yao.xiangjian.activity.AddUserActivity
import com.yao.xiangjian.adapter.UserAdapter
import com.yao.xiangjian.dao.UserDao
import com.yao.xiangjian.database.AppDatabase
import com.yao.xiangjian.databinding.ActivityMainBinding
import com.yao.xiangjian.entity.User
import java.net.InetAddress


class MainActivity : AppCompatActivity() {
    var userList = ArrayList<User>()
    lateinit var userDao:UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflate = ActivityMainBinding.inflate(layoutInflater)
        setContentView(inflate.root)
        userDao = AppDatabase.getDatabase(this).userDao()
        //检查是否开启了无障碍服务
        //isStartService()

        //初始化数据，并设置好布局格式
        initUsers()
        val layoutManager = GridLayoutManager(this, 1)
        inflate.recyclerView.layoutManager = layoutManager
        val adapter = UserAdapter(this, userList)
        inflate.recyclerView.adapter = adapter
        
        //下拉刷新
        inflate.swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        inflate.swipeRefresh.setOnRefreshListener {
            refreshUsers(adapter,inflate)
        }

        //长按+号跳转到添加页
        inflate.btnAdd.setOnLongClickListener {
            startActivity(Intent(this,AddUserActivity::class.java))
            true
        }
    }

    override fun onStart() {
        super.onStart()
        //STREAM_VOICE_CALL（通话）、STREAM_SYSTEM（系统声音）、STREAM_RING（铃声）、STREAM_MUSIC（音乐）和STREAM_ALARM（闹铃）
        val types = arrayOf(AudioManager.STREAM_VOICE_CALL,AudioManager.STREAM_SYSTEM,AudioManager.STREAM_RING,
            AudioManager.STREAM_MUSIC)
        //取消静音、勿扰模式。电话通话声音最大，音乐媒体声音最大
        setVolume(types)
    }
    private fun setVolume(types: Array<Int>) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationmanager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //判断“勿扰”权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            && !notificationmanager.isNotificationPolicyAccessGranted) {
            val intent:Intent =Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }else{
            //取消勿扰模式 currentInterruptionFilter勿扰分级器
            if (notificationmanager.currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL){
                notificationmanager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
            //设置铃声模式RINGER_MODE_NORMAL（普通）、RINGER_MODE_SILENT（静音）、RINGER_MODE_VIBRATE（震动）
            if (audioManager.ringerMode!=AudioManager.RINGER_MODE_NORMAL){
                audioManager.ringerMode=AudioManager.RINGER_MODE_NORMAL
            }
            //设置声音大小
            for(type in types){
                val curVolume = audioManager.getStreamVolume(type)
                val maxVolume = audioManager.getStreamMaxVolume(type)
                if (curVolume<maxVolume){
                    audioManager.setStreamVolume(type, maxVolume, 0)
                }
            }
        }

        //只想打开wifi，没别的目的
    }

    private fun refreshUsers(adapter: UserAdapter, inflate: ActivityMainBinding) {
        initUsers()
        //通知数据有变化,重新赋值渲染
        adapter.notifyDataSetChanged()
        //false表示刷新事件结束，并隐藏刷新进度条
        inflate.swipeRefresh.isRefreshing = false
    }

    private fun initUsers() {
        userList.clear()
        userList.addAll(userDao.loadAllUsers() as ArrayList<User>)
    }

    /**
     * 判断是否开启了无障碍服务,没启动则跳转到启动页面让用户自己开启
     */
    /*private fun isStartService(){
        val accessibilityManager:AccessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (!accessibilityManager.isTouchExplorationEnabled){
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }*/
}