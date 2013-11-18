package org.wikapidia.cookbook.wikiwalker;

import org.wikapidia.core.model.LocalPage;

import java.util.*;

/**
 * @author Shilad Sen
 */
public class GraphSearcher {

    private Node walk(LocalPage start, LocalPage end) {
        Set<Integer> visited = new HashSet<Integer>();
        Queue<Node> toVisit = new LinkedList<Node>();
        toVisit.add(new Node(start));
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

    public int shortestDistance(LocalPage start, LocalPage end) {
        Node last = walk(start, end);
        return (last == null) ? Integer.MAX_VALUE : last.getDepth();
    }
    public List<LocalPage> shortestPath(LocalPage start, LocalPage end) {
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
}
