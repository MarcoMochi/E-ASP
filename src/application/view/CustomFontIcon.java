package application.view;

import org.kordamp.ikonli.javafx.FontIcon;

public class CustomFontIcon extends FontIcon {

    public CustomFontIcon(String name) {
        super(name);
        this.getStyleClass().add("icons-color");
    }
}
