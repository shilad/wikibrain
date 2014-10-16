package org.wikibrain.sr.wikify;

import gnu.trove.set.TIntSet;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.phrases.PrunedCounts;
import org.wikibrain.utils.Scoreboard;

/**
* @author Shilad Sen
*/
public class LinkInfo implements Comparable<LinkInfo> {
    private String anchortext;

    private double linkProbability;
    private PrunedCounts<Integer> prior;

    private Scoreboard<Integer> scores = new Scoreboard<Integer>(5);

    private Integer dest;
    private Double score;
    private Integer knownDest;

    private int startChar;
    private int endChar;

    public LinkInfo() {}

    public LinkInfo(Token token) {
        this.startChar = token.getBegin();
        this.endChar = token.getEnd();
        this.anchortext = token.getToken();
    }

    public LinkInfo(LocalLink link) {
        this.startChar = link.getLocation();
        this.endChar = startChar + link.getAnchorText().length();
        this.anchortext = link.getAnchorText();
        this.knownDest = link.getDestId();
    }

    public boolean hasOnePossibility() {
        return getPrior().size() == 1;
    }

    public int getTopPriorDestination() {
        return getPrior().keySet().iterator().next();
    }

    public void addScore(int wpId, double score) {
        getScores().add(wpId, score);
    }

    @Override
    public int compareTo(LinkInfo o) {
        if (getScore() == null && o.getScore() == null) {
            return 0;
        } else if (getScore() == null) {
            return 1;
        } else if (o.getScore() == null) {
            return -1;
        } else if (Double.isNaN(score) && Double.isNaN(o.score)) {
            return 0;
        } else if (Double.isNaN(score)) {
            return 1;
        } else if (Double.isNaN(o.score)) {
            return -1;
        } else {
            return -1 * score.compareTo(o.score);
        }
    }

    public boolean intersects(TIntSet used) {
        for (int i = getStartChar(); i < getEndChar(); i++) {
            if (used.contains(i)) {
                return true;
            }
        }
        return false;
    }

    public void markAsUsed(TIntSet used) {
        for (int i = getStartChar(); i < getEndChar(); i++) {
            used.add(i);
        }
    }

    /**
     * Text of possible link.
     */
    public String getAnchortext() {
        return anchortext;
    }

    public void setAnchortext(String anchortext) {
        this.anchortext = anchortext;
    }

    /**
     * Probability that specified text represents a link.
     */
    public double getLinkProbability() {
        return linkProbability;
    }

    public void setLinkProbability(double linkProbability) {
        this.linkProbability = linkProbability;
    }

    /** Prior distribution of links associated with text. */
    public PrunedCounts<Integer> getPrior() {
        return prior;
    }

    public void setPrior(PrunedCounts<Integer> prior) {
        this.prior = prior;
    }

    /** Scores for outbound pages, ordered by score (track top 5). */
    public Scoreboard<Integer> getScores() {
        return scores;
    }

    public void setScores(Scoreboard<Integer> scores) {
        this.scores = scores;
    }

    /** Wikipedia id of destination this is an existing link, otherwise null (used for training) */
    public Integer getKnownDest() {
        return knownDest;
    }

    public void setKnownDest(Integer knownDest) {
        this.knownDest = knownDest;
    }

    /** Range of the anchortext token. */
    public int getStartChar() {
        return startChar;
    }

    public void setStartChar(int startChar) {
        this.startChar = startChar;
    }

    public int getEndChar() {
        return endChar;
    }

    public void setEndChar(int endChar) {
        this.endChar = endChar;
    }

    public Integer getDest() {
        return dest;
    }

    public void setDest(Integer dest) {
        this.dest = dest;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public LocalLink toLocalLink(Language language, int wpId) {
        return new LocalLink(language, anchortext, wpId, dest, true, startChar, true, LocalLink.LocationType.NONE);
    }
}
