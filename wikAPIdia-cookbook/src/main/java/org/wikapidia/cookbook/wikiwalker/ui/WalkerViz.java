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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class WalkerViz extends JComponent implements MouseListener {
    private static final int NODE_SPACING = 170;
    private static final int NODE_DIAMETER = 20;
    private static final int SUBNODE_DIAMETER = 10;
    private final LocalPage start;
    private final LocalPage end;

    private List<Node> path;

    private GraphSearcher searcher = new GraphSearcher();

    public WalkerViz(LocalPage start, LocalPage end) {
        super();
        this.start = start;
        this.end = end;
        this.path = new ArrayList<Node>();
        this.path.add(new Node(start));
        setLayout(null);
        addMouseListener(this);
    }


    protected void layoutNodes() {
        removeAll();

        int shortestDistance = searcher.shortestDistance(start, end);

        Insets insets = getInsets();
        Dimension size = getSize();

        int x = insets.left + NODE_SPACING / 2;
        int y = insets.bottom + size.height / 2;

        for (Node node : path) {
            int childCenterX = x - NODE_SPACING * 2;
            int childCenterY = y;

            List<Node> children = node.getChildren();
            for (int i = 0; i < Math.min(children.size(), 30); i++) {
                int sign = 1 - 2 * (i % 2);
                int r = (int) (NODE_SPACING * 2.7);
                double theta = sign * Math.PI/150 * i / 2;
                int x2 = (int)(childCenterX + r * Math.cos(theta*3));
                int y2 = (int)(childCenterY + r * Math.sin(theta));

                LocalPage childPage = children.get(i).getPage();
                NodeComponent child = new NodeComponent(node.getPage(), childPage,
                        Color.LIGHT_GRAY, SUBNODE_DIAMETER/2);
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
        for (Node node : path) {
            NodeComponent nc = getParentComponent(node.getPage());
            int x = nc.getX() + nc.getWidth() / 2;
            int y = nc.getY() + nc.getHeight() / 2;
            for (NodeComponent nc2 : getChildComponents(node.getPage())) {
                int x2 = nc2.getX() + nc2.getWidth()/2;
                int y2 = nc2.getY() + nc2.getHeight()/2;
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(x, y, x2, y2);
            }
            g.setColor(Color.BLACK);
            g.drawString(node.getPage().getTitle().toString(), x - NODE_DIAMETER, y - NODE_DIAMETER + yOffset);
            yOffset *= -1;
        }
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
    public void mouseReleased(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
    }


    public static void main(String args[]) {
        JFrame f = new JFrame("Wiki Walker");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1000, 500);

        // create a fake path
        List<Node> path = new ArrayList<Node>();
        WikAPIdiaWrapper wrapper =  new WikAPIdiaWrapper(Utils.PATH_DB);
        LocalPage obama = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Barack Obama");
        LocalPage minnesota = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Minnesota");

        WalkerViz  component = new WalkerViz(obama, minnesota);
        f.add(component);
        f.setVisible(true);
    }
}
