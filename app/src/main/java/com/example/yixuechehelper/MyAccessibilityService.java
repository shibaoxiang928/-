package com.example.yixuechehelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "YiXueCheService";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndClick();
            // 每2秒轮询一次，防止系统不发送事件
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        // 监听所有类型的事件，确保不错过弹窗
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | 
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
        Log.d(TAG, "Service Connected and Configured");
        
        // 启动轮询
        handler.post(checkRunnable);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 事件触发时也执行一次检查
        checkAndClick();
    }

    private void checkAndClick() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // 情况3 (新增)：上报失败，点击“重新上报”
        // 放在最前面优先处理，因为这是一个错误弹窗
        if (clickButtonByText(rootNode, "重新上报")) {
            Log.d(TAG, "Clicked 重新上报");
            return;
        }

        // 情况1：视频结束后，点击“开始学习”
        if (clickButtonByText(rootNode, "开始学习")) {
            Log.d(TAG, "Clicked 开始学习");
            return; 
        }

        // 情况2：点击“切换播放”
        if (clickButtonByText(rootNode, "切换播放")) {
            Log.d(TAG, "Clicked 切换播放");
            return;
        }

        // 情况2前置：点击绿色那一栏的播放按钮
        // 只有在没有弹窗的时候才尝试点击播放
        if (clickPlayButton(rootNode)) {
            Log.d(TAG, "Clicked 播放 button");
        }
    }

    private boolean clickButtonByText(AccessibilityNodeInfo root, String text) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (performClick(node)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean clickPlayButton(AccessibilityNodeInfo root) {
        // 查找所有包含“播放”的节点
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText("播放");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                CharSequence text = node.getText();
                CharSequence desc = node.getContentDescription();
                String sText = text != null ? text.toString() : "";
                String sDesc = desc != null ? desc.toString() : "";

                // 过滤逻辑：
                boolean isPlayText = sText.equals("播放") || sDesc.equals("播放");
                boolean containsPlay = sText.contains("播放") || sDesc.contains("播放");
                boolean isNotSwitch = !sText.contains("切换") && !sDesc.contains("切换");
                boolean isNotPlaying = !sText.contains("中") && !sDesc.contains("中"); // 排除“播放中”

                if ((isPlayText || (containsPlay && isNotSwitch && isNotPlaying))) {
                     if (performClick(node)) {
                         return true;
                     }
                }
            }
        }
        return false;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        // 如果当前节点不可点击，尝试点击父节点
        return performClick(node.getParent());
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service Interrupted");
    }
}
