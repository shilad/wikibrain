package org.wikibrain.cookbook.wikiwalker.ui;

import org.wikibrain.core.model.LocalPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A visual representation of a node that consists of a circle.
 * The parent page is the page preceding it in the path if the node is NOT on the main path.
 *
 * @author Shilad Sen
 */
public class NodeComponent extends JComponent implements MouseListener {
    private final int radius;
    private final LocalPage page;
    private Color color;
    private final LocalPage parent;
    private Color currentColor;

    public NodeComponent(LocalPage parent, LocalPage page, Color color, int radius) {
        this.parent = parent;
        this.page = page;
        this.color = color;
        this.currentColor = color;
        this.radius = radius;
        setToolTipText(page.getTitle().toString());
        addMouseListener(this);
    }

    public void setColor(Color color) {
        this.color = color;
        this.currentColor = color;
        this.repaint();
    }

    protected void paintComponent(Graphics g) {
        g.setColor(color);
        g.drawOval(0, 0, radius * 2 - 1, radius * 2 - 1);
        g.setColor(currentColor);
        g.fillOval(1, 1, radius * 2 - 3, radius * 2 - 3);
    }

    public LocalPage getPage() {
        return page;
    }

    public LocalPage getParentPage() {
        return parent;
    }

    @Override
    public String toString() {
        return "NodeComponent{" +
                "radius=" + radius +
                ", page=" + page +
                ", color=" + color +
                ", parent=" + parent +
                '}';
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {}

    @Override
    public void mousePressed(MouseEvent mouseEvent) {}

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {}

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        currentColor = Color.ORANGE;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        currentColor = color;
        repaint();
    }
}
