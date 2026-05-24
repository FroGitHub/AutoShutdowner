package windowsController;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.List;

public class WindowService {

    private static final int WM_CLOSE = 0x0010;
    private static final int MAX_TITLE_LENGTH = 512;

    public record WindowItem(WinDef.HWND hwnd, String title, int pid) {}

    public List<WindowItem> getOpenWindows() {
        List<WindowItem> result = new ArrayList<>();

        User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            if (User32.INSTANCE.IsWindowVisible(hwnd)) {
                String title = getWindowTitle(hwnd);
                if (!title.isEmpty()) {
                    result.add(new WindowItem(hwnd, title, getWindowPid(hwnd)));
                }
            }
            return true;
        }, null);

        return result;
    }

    public void sendCloseMessage(int targetPid) {
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (getWindowPid(hwnd) == targetPid) {
                User32.INSTANCE.PostMessage(hwnd, WM_CLOSE, null, null);
            }
            return true;
        }, null);
    }

    public int getWindowPid(WinDef.HWND hwnd) {
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
        return pid.getValue();
    }

    private String getWindowTitle(WinDef.HWND hwnd) {
        char[] buffer = new char[MAX_TITLE_LENGTH];
        User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
        return new String(buffer).trim();
    }
}
