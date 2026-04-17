package com.Neko.YouXi.Root;

import com.Neko.YouXi.Root.ImGuiAidlService;
import com.Neko.YouXi.JniRoot;

public class ImGuiAidlService {
    private static JniRoot ipc;

    public static boolean isConnect() {
        return ipc != null;
    }

    public static void InItIPC(JniRoot ipc) {
        ImGuiAidlService.ipc = ipc;
    }

    public static JniRoot GetIPC() {
        return ipc;
    }
}
