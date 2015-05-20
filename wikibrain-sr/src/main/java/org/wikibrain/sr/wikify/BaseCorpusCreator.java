package org.wikibrain.sr.wikify;

import gnu.trove.TCollections;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.nlp.Dictionary;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.phrases.LinkProbabilityDao;
import org.wikibrain.phrases.PhraseTokenizer;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.utils.WpThreadUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public abstract class BaseCorpusCreator {
    private static final Logger LOG = LoggerFactory.getLogger(BaseCorpusCreator.class);

    private final Language language;
    private final StringTokenizer tokenizer = new StringTokenizer();

    private final Wikifier wikifier;
    private final LocalPageDao pageDao;
    private Dictionary dictionary;
    private BufferedWriter corpus;
    private TIntObjectMap<String> mentionUrls = TCollections.synchronizedMap(new TIntObjectHashMap<String>());

    private boolean joinPhrases = true;
    private final PhraseTokenizer phraseTokenizer;

    public BaseCorpusCreator(Language language, LocalPageDao pageDao, Wikifier wikifier, LinkProbabilityDao linkProbDao) {
        this.language = language;
        this.pageDao = pageDao;
        this.wikifier = wikifier;
        this.phraseTokenizer = new PhraseTokenizer(linkProbDao);
    }

    /**
     * @return A list of Strings in the corpus.
     * Each string should be at least sentence granularity.
     * They could be a higher level (paragraph, document).
     */
    public abstract Iterator<IdAndText> getCorpus() throws DaoException;

    public void write(File dir) throws IOException, DaoException {
        if (dir.exists()) {
            FileUtils.deleteQuietly(dir);
        }
        dir.mkdirs();
        dictionary = new Dictionary(language, Dictionary.WordStorage.ON_DISK);
        corpus = WpIOUtils.openWriter(new File(dir, "corpus.txt"));
        corpus.write(String.format("@WikiBrainCorpus\t%s\t%s\t%s\t%s\n",
                this.language.getLangCode(),
                this.getClass().getName(),
                wikifier.getClass().getName(),
                new Date().toString()
            ));
        ParallelForEach.iterate(getCorpus(), new Procedure<IdAndText>() {
            @Override
            public void call(IdAndText text) throws Exception {
                processText(text);
            }
        }, 10000);
        corpus.close();
        dictionary.write(new File(dir, "dictionary.txt"));
    }

    private void processText(IdAndText text) throws IOException, DaoException {
        List<LocalLink> mentions;
        if (text.getId() >= 0) {
            mentions = wikifier.wikify(text.getId(), text.getText());
        } else {
            mentions = wikifier.wikify(text.getText());
        }
        LocalPage page = pageDao.getById(language, text.getId());
        String title = (page == null) ? "Unknown" : page.getTitle().getCanonicalTitle();
        StringBuilder document = new StringBuilder();
        document.append("\n@WikiBrainDoc\t" + text.getId() + "\t" + title + "\n");

        for (Token sentence : tokenizer.getSentenceTokens(language, text.getText())) {
            List<String> tokens = addMentions(sentence, mentions);
            if (tokens == null) {
                continue;
            }
            String finalSentence = joinPhrases(tokens);
            document.append(finalSentence);
            document.append('\n');
            dictionary.countNormalizedText(finalSentence);
        }
        synchronized (corpus) {
            corpus.write(document.toString() + "\n");
        }
    }

    private String joinPhrases(List<String> words) throws DaoException {
        if (words.isEmpty()) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        for (String phrase : phraseTokenizer.makePhrases(language, words)) {
            if (buffer.length() > 0) buffer.append(' ');
            buffer.append(phrase.replaceAll(" ", "_"));
        }
        return buffer.toString();
    }

    private List<String> addMentions(Token sentence, List<LocalLink> mentions) throws IOException, DaoException {
        List<Token> words = tokenizer.getWordTokens(language, sentence);
        if (words.isEmpty()) {
            return null;
        }

        // Accumulators
        List<String> line = new ArrayList<String>();

        // Process each word token
        // Warning: If mentions do not align with sentence tokens, this will break...
        for (int m = 0, w = 0; w < words.size(); w++) {
            Token token = words.get(w);

            // Advance mention while it starts before the current token
            while (m < mentions.size() && mentions.get(m).getLocation() < token.getBegin()) {
                m++;
            }

            String phrase = token.getToken();

            // If start of mention occurs in token, advance tokens as necessary
            if (m < mentions.size() && mentions.get(m).getLocation() < token.getEnd()) {
                int end = mentions.get(m).getLocation() + mentions.get(m).getAnchorText().length();

                // While next word begins before mention ends, append next word
                while (w+1 < words.size() && words.get(w+1).getBegin() < end) {
                    if (phrase.length() > 0) {
                        phrase += "_";
                    }
                    w++;
                    phrase += words.get(w).getToken();
                }
                phrase += ":" + getMentionUrl(mentions.get(m).getDestId());
            }

            phrase = phrase.trim();
            if (phrase.length() == 0) {
                continue;
            }
            if (phrase.contains("\n")) {
                throw new IllegalStateException();
            }
            line.add(phrase);
        }

        return line;
    }

    private String getMentionUrl(int wpId) throws DaoException {
        if (!mentionUrls.containsKey(wpId)) {
            LocalPage page = pageDao.getById(language, wpId);
            if (page == null) {
                mentionUrls.put(wpId, "/w/" + language.getLangCode() + "/-1/Unknown_page");
            } else {
                mentionUrls.put(wpId, page.getCompactUrl());
            }
        }
        return mentionUrls.get(wpId);
    }

    public void setJoinPhrases(boolean joinPhrases) {
        this.joinPhrases = joinPhrases;
    }
}
