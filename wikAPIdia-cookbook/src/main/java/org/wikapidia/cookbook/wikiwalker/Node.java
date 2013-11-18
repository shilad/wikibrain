package org.wikapidia.cookbook.wikiwalker;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class Node {
    private final Language language;
    private final Node parent;
    private final int pageId;
    private final int depth;

    public Node(LocalPage page) {
        this(page.getLanguage(), page.getLocalId(), 0, null);
    }
    private Node(Language language, int pageId, int depth, Node parent) {
        this.language = language;
        this.pageId = pageId;
        this.depth = depth;
        this.parent = parent;
    }

    public List<Node> getChildren() {
        List<Node> result = new ArrayList<Node>();
        for (Integer childId : WikAPIdiaWrapper.getInstance().getLinkedIds(language, pageId)) {
            result.add(new Node(language, childId, depth+1, this));
            if (result.size() >= 30) {
                break;
            }
        }
        return result;
    }

    public LocalPage getPage() {
        return WikAPIdiaWrapper.getInstance().getLocalPageById(language, pageId);
    }

    public int getPageId() {
        return pageId;
    }

    public int getDepth() {
        return depth;
    }

    public Node getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "Node{" +
                "page=" + getPage() +
                '}';
    }
}
