package org.wikapidia.cookbook.wikiwalker.ui;

import org.wikapidia.core.model.LocalPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Shilad Sen
 */
public class NodeComponent extends JComponent implements MouseListener {
    private final int radius;
    private final LocalPage page;
    private final Color color;
    private final LocalPage parent;
    private Color fillColor;

    public NodeComponent(LocalPage parent, LocalPage page, Color color, int radius) {
        this.parent = parent;
        this.page = page;
        this.color = color;
        this.fillColor = color;
        this.radius = radius;
        setToolTipText(page.getTitle().toString());
        addMouseListener(this);
    }

    protected void paintComponent(Graphics g) {
        g.setColor(color);
        g.drawOval(0, 0, radius * 2 - 1, radius * 2 - 1);
        g.setColor(fillColor);
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
        fillColor = Color.ORANGE;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        fillColor = color;
        repaint();
    }
}
