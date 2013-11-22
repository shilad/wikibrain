package org.wikapidia.cookbook.wikiwalker.ui;

import org.wikapidia.cookbook.wikiwalker.GraphSearcher;
import org.wikapidia.cookbook.wikiwalker.Node;
import org.wikapidia.cookbook.wikiwalker.Utils;
import org.wikapidia.cookbook.wikiwalker.WikAPIdiaWrapper;
import org.wikapidia.core.model.LocalPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class WalkerViz extends JComponent implements MouseListener {
    private static final Color COLOR_SUBNODE = new Color(100, 100, 100);
    private static final Color COLOR_LINE = new Color(180, 180, 180);
    private static final Color COLOR_NODE = new Color(50, 50, 50);

    private static final int NODE_SPACING = 120;
    private static final int NODE_DIAMETER = 20;
    private static final int SUBNODE_DIAMETER = 14;

    private LocalPage start;
    private LocalPage end;
    private List<Node> path;
    private GraphSearcher searcher = new GraphSearcher();

    public WalkerViz(LocalPage start, LocalPage end) {
        super();

        setLayout(null);
        setPages(start, end);
        addMouseListener(this);
    }

    public void setPages(LocalPage start, LocalPage end) {
        removeAll();

        // HACK: start and end nodes MUST be marked as interesting
        for (LocalPage page : Arrays.asList(start, end)) {
            WikAPIdiaWrapper.getInstance().setInteresting(page.getLanguage(), page.getLocalId(), true);
        }

        this.start = start;
        this.end = end;
        this.path = new ArrayList<Node>();
        this.path.add(new Node(start));
        repaint();
    }


    protected void layoutNodes() {
        removeAll();

        int shortestDistance = searcher.shortestDistance(start, end);

        Insets insets = getInsets();
        Dimension size = getSize();

        int x = insets.left + NODE_SPACING / 2;
        int y = insets.bottom + size.height / 2;

        for (Node node : path) {
            Ellipse ellipse = new Ellipse(x-NODE_SPACING, y, NODE_SPACING*3.5, NODE_SPACING*7);

            List<Node> children = node.getChildren();
            List<Point2D> points = ellipse.generatePoints(Math.PI * 0.2, -Math.PI * 0.2, children.size());
            for (int i = 0; i < children.size(); i++) {
                int x2 = (int) points.get(i).getX();
                int y2 = (int) points.get(i).getY();

                LocalPage childPage = children.get(i).getPage();
                NodeComponent child = new NodeComponent(node.getPage(), childPage,
                        COLOR_SUBNODE, SUBNODE_DIAMETER/2);
                child.setBounds(x2- SUBNODE_DIAMETER/2, y2 - SUBNODE_DIAMETER/2,
                        SUBNODE_DIAMETER, SUBNODE_DIAMETER);
                child.addMouseListener(this);
                add(child);
            }

            Color color;
            int shortestThroughNode = path.indexOf(node) + searcher.shortestDistance(node.getPage(), end);
            switch (shortestThroughNode - shortestDistance) {
                case 0: color = Color.GREEN; break;
                case 1: color = new Color(200, 255, 200); break;
                case 2: color = new Color(255, 200, 200); break;
                default: color = Color.RED;
            }

            NodeComponent nc = new NodeComponent(null, node.getPage(), color, NODE_DIAMETER/2);
            nc.setBounds(x - NODE_DIAMETER/2, y-NODE_DIAMETER/2, NODE_DIAMETER, NODE_DIAMETER);
            nc.addMouseListener(this);
            add(nc);

            x += NODE_SPACING;
        }

        // Add the goal
        x = insets.left + size.width - NODE_SPACING / 2;
        NodeComponent nc = new NodeComponent(null, end, COLOR_LINE, NODE_DIAMETER/2);
        nc.setBounds(x - NODE_DIAMETER/2, y-NODE_DIAMETER/2, NODE_DIAMETER, NODE_DIAMETER);
        nc.addMouseListener(this);
        add(nc);

    }

    protected List<NodeComponent> getChildComponents(LocalPage page) {
        List<NodeComponent> children = new ArrayList<NodeComponent>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof NodeComponent) {
                NodeComponent nc = (NodeComponent) c;
                if (nc.getParentPage() != null && nc.getParentPage().equals(page)) {
                    children.add(nc);
                }
            }
        }
        return children;
    }

    protected NodeComponent getParentComponent(LocalPage page) {
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof NodeComponent) {
                NodeComponent nc = (NodeComponent) c;
                if (nc.getParentPage() == null && nc.getPage().equals(page)) {
                    return nc;
                }
            }
        }
        return null;

    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getComponentCount() == 0) {
            layoutNodes();
        }
        int yOffset = 10;
        paintNode(g, yOffset, end);
        for (Node node : path) {
            paintNode(g, yOffset, node.getPage());
            yOffset *= -1;
        }
    }

    private void paintNode(Graphics g, int yOffset, LocalPage page) {
        NodeComponent nc = getParentComponent(page);
        int x = nc.getX() + nc.getWidth() / 2;
        int y = nc.getY() + nc.getHeight() / 2;
        Font orig = g.getFont();
        if (!page.equals(end)) {
            for (NodeComponent nc2 : getChildComponents(page)) {
                int x2 = nc2.getX() + nc2.getWidth()/2;
                int y2 = nc2.getY() + nc2.getHeight()/2;

                QuadCurve2D q = new QuadCurve2D.Float();
                q.setCurve(x, y, x + NODE_SPACING / 3, y, x2, y2);
                g.setColor(COLOR_LINE);
                ((Graphics2D)g).draw(q);

                // draw labels on the last page
                if (page.equals(path.get(path.size()-1).getPage())) {
                    double k = 1.0 * SUBNODE_DIAMETER / nc2.getLocation().distance(nc.getLocation());
                    int x3 = (int) (x2 + (x2-x) * k);
                    int y3 = (int) (y2 + (y2-y) * k) + 5;
                    g.setColor(COLOR_SUBNODE);
                    g.setFont(orig.deriveFont(10f));
                    g.drawString(nc2.getPage().getTitle().toString(), x3, y3);
                }
            }
        }
        g.setFont(orig);
        g.setColor(COLOR_NODE);
        g.drawString(page.getTitle().toString(), x - NODE_DIAMETER, y - NODE_DIAMETER + yOffset);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (!(mouseEvent.getComponent() instanceof NodeComponent)) {
            return;
        }
        NodeComponent nc = (NodeComponent) mouseEvent.getComponent();
        LocalPage clicked = nc.getPage();
        LocalPage clickedParent = nc.getParentPage();

        if (clickedParent == null) {    // A page on the main path
            for (int i = 0; i < path.size(); i++) {
                if (path.get(i).getPage().equals(clicked)) {
                    path = path.subList(0, i+1);
                    break;
                }
            }
        } else {                        // A page on the branch
            for (int i = 0; i < path.size(); i++) {
                if (path.get(i).getPage().equals(clickedParent)) {
                    path = path.subList(0, i+1);
                    path.add(new Node(clicked));
                    break;
                }
            }
        }
        layoutNodes();
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {}

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}

    @Override
    public void mouseExited(MouseEvent mouseEvent) {}

    public static void main(String args[]) {
        JFrame f = new JFrame("Wiki Walker");
        f.setSize(1000, 1000);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        WikAPIdiaWrapper wrapper =  new WikAPIdiaWrapper(Utils.PATH_DB);
        LocalPage obama = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Barack Obama");
        LocalPage minnesota = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Minnesota");

        WalkerViz  component = new WalkerViz(obama, minnesota);
        f.add(component);
        f.setVisible(true);
    }
}
