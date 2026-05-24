package ui.window;

public class WindowItem {

    private final String title;
    private boolean selected;

    public WindowItem(String title) {
        this.title = title;
        this.selected = false;
    }

    public String getTitle() {
        return title;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
