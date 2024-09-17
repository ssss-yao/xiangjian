package com.yao.xiangjian.activity

import android.Manifest
import android.R.attr
import android.R.attr.path
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.yao.xiangjian.AppData
import com.yao.xiangjian.R
import com.yao.xiangjian.dao.UserDao
import com.yao.xiangjian.database.AppDatabase
import com.yao.xiangjian.databinding.ActivityAddUserBinding
import com.yao.xiangjian.entity.User
import com.yao.xiangjian.service.ForegroundService
import com.yao.xiangjian.utils.toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID


class AddUserActivity : AppCompatActivity() {
    lateinit var userDao: UserDao
    companion object{
        lateinit var inflate: ActivityAddUserBinding
        lateinit var imgPath: String
        var OldImgPath =""
    }

    private val pickImageResult =
        //注册ActivityResult的观察者 当启动的Activity返回结果时 这个观察者会被触发
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // 使用Glide加载选择的图片
                inflate?.let { it1 -> Glide.with(this).load(it).into(it1.imageView) }
                OldImgPath = getRealPathFromUri(this,uri).toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inflate = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(inflate.root)
        init()
        // 设置ImageView的点击事件，打开图片库
        inflate.imageView.setOnClickListener(View.OnClickListener {
            //检查权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                pickImageResult.launch("image/*")
            }else{
                //请求权限
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        })
        //添加联系人
        inflate.addUser.setOnClickListener {
            if (inflate.etUsername.text.isEmpty()){
                "微信名未输入,请检查".toast(this)
            }else{
                val user:User=getUser()
                val hasUser = userDao.findUserByName(user.name)
                if (hasUser!=null){
                    "添加未成功，请检查已有此人".toast(this)
                }else{
                    val id = userDao.insertUser(user)
                    if (id!=null){
                        "添加成功".toast(this)
                    }else{
                        "添加失败".toast(this)
                    }
                }
            }
            //恢复默认图片
            OldImgPath=""
            Glide.with(this).load(R.drawable.imgadd).into(inflate.imageView)
        }
        //删除联系人
        inflate.deleteUser.setOnClickListener {
            inflate.etUsername.let {
                val name = inflate.etUsername.text.toString()
                val user = userDao.findUserByName(name)
                if (user==null){
                    "删除未完成，请检查列表是否有此人".toast(this)
                }else{
                    val file = File(user.imagePath)
                    if (file.exists()){
                        file.delete()
                    }
                    val delInt = userDao.deleteUserByName(name)
                    if (delInt!=null){
                        "删除成功".toast(this)
                    }else{
                        "删除失败".toast(this)
                    }
                }
            }
        }
        inflate.startService.setOnClickListener {
            if (isServiceRunning(this,ForegroundService::class.java)){
                "前台服务已启动".toast(this)
            }else{
                val intent = Intent(this, ForegroundService::class.java)
                startService(intent) // 启动Service
                "前台服务正在启动,稍后在通知栏出现~".toast(this)
            }
        }
        val editor=getSharedPreferences("autoAnswer", Context.MODE_PRIVATE)
        inflate.autookphone.isChecked=editor.getBoolean("video",false)
        inflate.autookphone.setOnCheckedChangeListener { buttonView, isChecked ->
            editor.edit().putBoolean("video",isChecked).commit()
        }
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val serviceList = activityManager.getRunningServices(Integer.MAX_VALUE)

        for (service in serviceList) {
            if (serviceClass.name.equals(service.service.className)) {
                return true
            }
        }
        return false
    }


    private fun init() {
        val file = File(filesDir.path, "userImage")
        if (!file.exists()){
            file.mkdir()
        }
        imgPath = file.path
        userDao = AppDatabase.getDatabase(this).userDao()
    }

    private fun getUser(): User {
        val name = inflate.etUsername.text.toString()
        val phone = inflate.etPhoneNumber.text.toString()
        var newName=""
        if (OldImgPath.isEmpty()){
            val image =
                createTextBitmapToImage(name, Resources.getSystem().displayMetrics.widthPixels,300,100f, imgPath + "/" + UUID.randomUUID() + ".png")
            newName=image.name
        }else{
            newName = OldImgPath.splitToSequence("/").last()
            val newImgPath=imgPath+"/"+ newName
            copyImage(OldImgPath,newImgPath)
        }
        val newImgPath=imgPath+"/"+ newName
        return User(name,phone,newImgPath)
    }

    private fun copyImage(sourcePath: String?, destinationPath: String?) {
        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null
        try {
            fis = FileInputStream(sourcePath)
            fos = FileOutputStream(destinationPath)

            val buffer = ByteArray(1024)
            var length: Int
            while ((fis.read(buffer).also { length = it }) > 0) {
                fos.write(buffer, 0, length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fis?.close()
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }

        return null
    }

    fun createTextBitmapToImage(text: String, width: Int, height: Int, textSize: Float,path: String): File {
        // 创建一个指定大小的Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // 初始化Canvas和Paint
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 设置背景颜色为蓝色
        paint.color = Color.CYAN
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 设置文字颜色为黑色
        paint.color = Color.BLACK
        paint.textSize = textSize
        paint.typeface = Typeface.DEFAULT_BOLD
        // 居中显示文字
        val textHeight = paint.descent() + paint.ascent()
        val textBounds = paint.getFontSpacing()
        val textY = (height - textHeight) / 2f
        val textX = (width - paint.measureText(text)) / 2f
        // 绘制文字
        canvas.drawText(text, textX, textY, paint)

        // 保存图片到文件
        val file = File(path)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        return file
    }
}