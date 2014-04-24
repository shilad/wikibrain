package org.wikibrain.cookbook.wikiwalker;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in the article link graph.
 * The children of a node are the pages it links to.
 *
 * @author Shilad Sen
 */
public class Node {
    private final Language language;
    private final Node parent;
    private final int pageId;
    private final int depth;
    private final WikiBrainWrapper wrapper;

    /**
     * Construct a new node for a particular page.
     * @param wrapper
     * @param page
     */
    public Node(WikiBrainWrapper wrapper, LocalPage page) {
        this(wrapper, page.getLanguage(), page.getLocalId(), 0, null);
    }

    /**
     * This constuctor is only used internally when creating children.
     * @param wrapper
     * @param language
     * @param pageId
     * @param depth
     * @param parent
     */
    private Node(WikiBrainWrapper wrapper, Language language, int pageId, int depth, Node parent) {
        this.wrapper = wrapper;
        this.language = language;
        this.pageId = pageId;
        this.depth = depth;
        this.parent = parent;
    }

    /**
     * Returns the nodes linked to by the current node.
     * @return
     */
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<Node>();
        for (Integer childId : wrapper.getLinkedIds(language, pageId)) {
            if (wrapper.isInteresting(language, childId)) {
                result.add(new Node(wrapper, language, childId, depth+1, this));
            }
        }
        return result;
    }

    /**
     * Returns the local page associated with the node.
     * Note that this is relatively slow compared to getChildren(), so it should
     * only be called after the shortest path is found, not during the search itself.
     * @return
     */
    public LocalPage getPage() {
        return wrapper.getLocalPageById(language, pageId);
    }

    /**
     * @return A unique id for the page
     */
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
