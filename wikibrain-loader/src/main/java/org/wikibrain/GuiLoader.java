package org.wikibrain;

import org.wikibrain.loader.GraphicLoader;

import javax.swing.*;

/**
 * @author Shilad Sen
 */
public class GuiLoader {
    public static void main(String args[]) {
        GraphicLoader w = new GraphicLoader();
        w.setVisible(true);
        w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
