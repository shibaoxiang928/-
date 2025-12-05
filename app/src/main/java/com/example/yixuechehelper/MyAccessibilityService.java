package com.example.yixuechehelper;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "YiXueCheService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 只处理窗口内容变化或状态变化
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // 情况1：视频结束后，点击“开始学习”
        // 查找包含“开始学习”文本的节点
        if (clickButtonByText(rootNode, "开始学习")) {
            Log.d(TAG, "Clicked 开始学习");
            return; // 点击后返回，避免重复操作
        }

        // 情况2：点击“切换播放”
        if (clickButtonByText(rootNode, "切换播放")) {
            Log.d(TAG, "Clicked 切换播放");
            return;
        }

        // 情况2前置：点击绿色那一栏的播放按钮
        // 只有在没有弹窗（即上面两个没匹配到）的时候才尝试点击播放
        // 查找文本或描述完全等于“播放”的节点，或者包含“播放”但不是“正在播放”
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
                // 1. 必须包含“播放”
                // 2. 不能包含“切换”（因为那是弹窗按钮，虽然上面已经处理过，但防止漏网）
                // 3. 不能包含“中”（防止点击“播放中”状态标签）
                // 4. 或者是纯粹的“播放”图标（通常描述为“播放”）
                
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

    // 递归查找可点击的父节点

    // 递归查找可点击的父节点
    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        // 如果当前节点不可点击，尝试点击父节点
        return performClick(node.getParent());
    }
    
    private void attemptToClickPlayButton(AccessibilityNodeInfo root) {
         // 查找描述或文本包含“播放”的节点
         // 这里只是一个备用逻辑，因为“绿色栏”无法通过颜色识别
         // 假设列表项的播放按钮包含“播放”文字或描述
         // 为了防止误触，我们这里只在找不到弹窗按钮时尝试，并且需要更精确的过滤条件
         // 但由于缺乏布局信息，这里暂时留空或做简单尝试
         // 如果用户反馈需要，可以启用下面的逻辑：
         /*
         List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText("播放");
         if (nodes != null) {
             for (AccessibilityNodeInfo node : nodes) {
                 // 排除“切换播放”按钮（因为上面已经处理过，但为了保险）
                 if (node.getText() != null && node.getText().toString().contains("切换播放")) continue;
                 
                 // 尝试点击
                 if (performClick(node)) return;
             }
         }
         */
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service Interrupted");
    }
}
