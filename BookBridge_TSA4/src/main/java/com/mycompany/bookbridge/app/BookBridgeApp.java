package com.mycompany.bookbridge.app;

import com.mycompany.bookbridge.gui.BookBridgeFrame;
import javax.swing.SwingUtilities;

public class BookBridgeApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BookBridgeFrame frame = new BookBridgeFrame();
            frame.setVisible(true);
        });
    }
}