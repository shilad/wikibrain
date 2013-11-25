package org.wikapidia.cookbook.wikiwalker;

import org.wikapidia.core.model.LocalPage;

import java.util.*;

/**
 * A class that finds the shortest path between two nodes.
 * @author Shilad Sen
 */
public class GraphSearcher {
    /**
     * Wrapper library. Needed to create Nodes.
     */
    private final WikAPIdiaWrapper wrapper;

    /**
     * Creates the new graph searcher.
     * @param wrapper
     */
    public GraphSearcher(WikAPIdiaWrapper wrapper) {
        this.wrapper = wrapper;
    }

    private Node walk(LocalPage start, LocalPage end) {
        Set<Integer> visited = new HashSet<Integer>();
        Queue<Node> toVisit = new LinkedList<Node>();
        toVisit.add(new Node(wrapper, start));
        while (!toVisit.isEmpty()) {
            Node next = toVisit.remove();
            if (next.getPageId() == end.getLocalId()) {
                return next;
            }
            for (Node child : next.getChildren()) {
                if (visited.contains(child.getPageId())) {
                    continue;
                }
                if (child.getPageId() == end.getLocalId()) {
                    return child;
                }
                visited.add(child.getPageId());
                toVisit.add(child);
            }
        }
        return null;
    }

    /**
     * Calculates the shortest distance between two pages as determined by number of links.
     * A page should have distance 0 to itself.
     * @param start The starting page.
     * @param end The ending page.
     * @return The distance between two pages, or -1 if they are not connected.
     */
    public int shortestDistance(LocalPage start, LocalPage end) {
        markAsInteresting(start, end);
        Node last = walk(start, end);
        return (last == null) ? -1 : last.getDepth();
    }

    /**
     * Calculates the shortest path between two pages as determined by number of links.
     * A page should have a path of to itself containing only itself (length 1).
     * @param start The starting page.
     * @param end The ending page.
     * @return A list of the path between the pages (beginning with start and finishing with end)
     * or null if no path exists.
     */
    public List<LocalPage> shortestPath(LocalPage start, LocalPage end) {
        markAsInteresting(start, end);

        Node last = walk(start, end);
        if (last == null) {
            return null;
        }
        List<LocalPage> path = new ArrayList<LocalPage>();
        do {
            path.add(last.getPage());
            last = last.getParent();
        } while (last != null);
        Collections.reverse(path);
        return path;
    }

    /**
     * HACK ALERT: The WikAPIdia wrapper tries to determine "uninteresting" pages such as
     * lists, dates, years, etc. The Node filters out these uninteresting pages when you
     * ask it for children.
     *
     * BUT, the src and dest MUST be marked as interesting, or our graph search algorithm
     * will never find them!
     *
     * @param src
     * @param dest
     */
    private void markAsInteresting(LocalPage src, LocalPage dest) {
        for (LocalPage page : Arrays.asList(src, dest)) {
            wrapper.setInteresting(page.getLanguage(), page.getLocalId(), true);
        }
    }
}
