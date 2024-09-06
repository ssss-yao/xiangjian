package com.yao.xiangjian.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.Notification
import android.content.Context
import android.content.res.Resources
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.yao.xiangjian.AppData


class AutoCallVidoService: AccessibilityService() {

    companion object {
        var mService: AutoCallVidoService? = null
        var isAutoVideo:Boolean=false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType
        //获取自拍接听设置 true or false
        when(eventType){
            // 当Window发生变化时发送此事件。
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED->{
                //如果是com.tencent.mm.ui.LauncherUI，表示已跳转到wx界面
                val currentActivity= event.className
                Log.d("AutoSendMsgService",currentActivity.toString())
                try {
                    //自动接听
                    //1微信应用内

                    //自动接听配置
                    if (getSharedPreferences("autoAnswer", Context.MODE_PRIVATE).getBoolean("video",false)){
                        if (event.packageName=="com.tencent.mm" && event.text.any { it=="邀请你视频通话" }){
                            performClick((Resources.getSystem().displayMetrics.widthPixels/2).toFloat(),300f)
                        }
                        //2全屏的接听界面
                        if (currentActivity=="com.tencent.mm.plugin.voip.ui.VideoActivity"){
                            try {
                                Thread.sleep(1000)
                                var nodeInfo:AccessibilityNodeInfo
                                val nodeInfos =
                                    rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/nsv")
                                nodeInfo = nodeInfos.get(0)

                                while (nodeInfo.childCount==1){
                                    nodeInfo=nodeInfo.getChild(0)
                                }
                                //有10个，第10个是接听
                                if (nodeInfo.childCount<10){
                                    return
                                }
                                val child = nodeInfo.getChild(9)
                                val bounds = Rect()
                                child.getBoundsInScreen(bounds)
                                performClick(bounds.centerX().toFloat(),bounds.centerY().toFloat())
                                Log.d("AutoSendMsgService",currentActivity.toString())
                            }catch (e:Exception){
                                "自动接听出错啦啦啦啦~~~~"
                            }

                        }
                    }

                    var name=AppData.value
                    if (!name.isEmpty()){
                        //微信页里的联系人聊天页面
                        val chatView =
                            rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bl9")
                        Thread.sleep(1000)
                        if (currentActivity=="com.tencent.mm.plugin.profile.ui.ContactInfoUI" && name!=null){
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        if (!chatView.isEmpty()){
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        Thread.sleep(1000)
                        if(currentActivity == "com.tencent.mm.ui.LauncherUI"){
                            //1、底部导航栏有4个，到通讯录页面
                            val tables = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/icon_tv")
                            Thread.sleep(1000)
                            tables[1].click()
                            Thread.sleep(1000)
                            //2、查找对应名称联系人并点击进入
                            val isOkTwo =
                                findChildNodeAndClick("com.tencent.mm:id/mg", "com.tencent.mm:id/kbq", name)

                            AppData.updateValue("")
                            //3、进入页面点击音视频完成拨打视频操作（com.tencent.mm:id/o3b），查不到人或出错则退出，
                            if (isOkTwo){
                                //看了只有两，一个是发消息，一个是音视频通话，所以简单获取下标1
                                Thread.sleep(1500)
                                val yspView:List<AccessibilityNodeInfo> = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/o3b")
                                Thread.sleep(1000)
                                yspView[1].click()
                                Thread.sleep(1000)
                                //0下标视频通话，1下标语音通话，com.tencent.mm:id/obc
                                val yspView2:List<AccessibilityNodeInfo> = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/obc")
                                Thread.sleep(1000)
                                yspView2[0].click()
                            }
                        }
                    }

                }catch (e:Exception){
                    e.printStackTrace()
                    return
                }
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED->{
                if (getSharedPreferences("autoAnswer", Context.MODE_PRIVATE).getBoolean("video",false)){
                    //微信应用外用通知接听
                    val currentActivity= event.className
                    val notif = event.parcelableData as Notification?
                    if (notif != null && notif.category=="call") {
                        if (notif.extras.getString(Notification.EXTRA_TITLE)=="微信"){
                            Thread.sleep(1000)
                            performClick((Resources.getSystem().displayMetrics.widthPixels/2).toFloat(),300f)
                            Log.d("AutoSendMsgService", currentActivity.toString())
                        }
                    }
                }
            }
        }
    }

    private fun performClick(x: Float, y: Float) {
        val gestureBuilder = GestureDescription.Builder()
        val path: Path = Path()
        path.moveTo(x, y)
        gestureBuilder.addStroke(StrokeDescription(path, 0, 1))
        val gestureDescription = gestureBuilder.build()
        dispatchGesture(gestureDescription, null, null)
    }

    override fun onInterrupt() {
        Log.d("AutoSendMsgService","无障碍服务中断~")
        AppData.updateValue("updated value")
    }

    fun AccessibilityNodeInfo?.click(): Boolean {
        this ?: return false
        return if (isClickable) {
            performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            parent?.click() == true
        }
    }

    fun AccessibilityService?.findChildNodeAndClick(parentViewId: String, childViewId: String, name:String):Boolean{
        this ?: return false

        var node: AccessibilityNodeInfo? = null
        var hasPeople = true
        do {
            val parentNode: AccessibilityNodeInfo =
                rootInActiveWindow.findAccessibilityNodeInfosByViewId(parentViewId).firstOrNull() ?: return false
            val size = parentNode.childCount
            if (size <= 0) return false
            for (index in 0 until size) {
                parentNode.getChild(index).findAccessibilityNodeInfosByViewId(childViewId).firstOrNull()?.let {
                    Log.d("printNodeInfo", "当前页parentNode可见的元素=======${it.text}")
                    if (it.text.toString()==name){
                        node=it
                    }
                }
                if (node!=null){
                    break
                }
            }
            if (node!=null){
                break
            }
            rootInActiveWindow.findAccessibilityNodeInfosByViewId(parentViewId)
                .lastOrNull()?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD).let {
                    if (it != null) {
                        hasPeople=it
                    }
                }

            if(!hasPeople){
                return false
            }
            Thread.sleep(1000)
        } while (node==null)

        node.click()
        return hasPeople

    }
}