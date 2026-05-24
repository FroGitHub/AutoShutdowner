package windowsController;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SmartWindowsCloser implements AutoCloseable {

    private final WindowService windowService;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public SmartWindowsCloser(WindowService windowService) {
        this.windowService = windowService;
    }

    public void smartClose(int pid) {
        windowService.sendCloseMessage(pid);
        // Schedule forceful termination if the process doesn't close gracefully
        scheduler.schedule(() -> killProcess(pid), 3, TimeUnit.SECONDS);
    }

    public void killProcess(int pid) {

        WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_TERMINATE,
                false,
                pid
        );

        if (handle == null) {
            return;
        }

        Kernel32.INSTANCE.TerminateProcess(handle, 1);

        Kernel32.INSTANCE.CloseHandle(handle);
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }
}
