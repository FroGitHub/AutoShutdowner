package windowsController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class WindowControllerFacade {

    private final WindowService windowService;
    private final SmartWindowsCloser smartWindowsCloser;
    private Map<String, Integer> nameToPidMap;

    public WindowControllerFacade() {
        this.windowService = new WindowService();
        this.smartWindowsCloser = new SmartWindowsCloser(windowService);
        refreshWindowList();
    }

    public void refreshWindowList() {
        this.nameToPidMap = windowService.getOpenWindows().stream()
                .collect(Collectors.toMap(
                        item -> item.title(),
                        item -> item.pid(),
                        (existing, replacement) -> existing // Keep the first one found in case of duplicates
                ));
    }

    public List<String> getWindowNames() {
        return nameToPidMap.keySet().stream().sorted().toList();
    }

    private void closeWindow(String windowName) {
        Integer pid = nameToPidMap.get(windowName);
        if (pid != null) {
            smartWindowsCloser.smartClose(pid);
        }
    }
    public void closeAllWindows(List<String> windowNames) {
        windowNames.forEach(this::closeWindow);
        smartWindowsCloser.close();
    }

    public boolean areWindowsClosed(List<String> shutdownTargets) {
        refreshWindowList();
        return shutdownTargets.stream().noneMatch(nameToPidMap::containsKey);
    }
}
