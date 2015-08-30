package org.wikibrain.webapi;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jooq.tools.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.wikify.Wikifier;
import org.wikibrain.utils.WpCollectionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class WikiBrainServer extends AbstractHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WikiBrainServer.class);
    private final Env env;
    private final LocalPageDao pageDao;
    private final LocalLinkDao linkDao;
    private final LocalCategoryMemberDao catDao;
    private WebEntityParser entityParser;

    public WikiBrainServer(Env env) throws ConfigurationException, DaoException {
        this.env = env;
        this.entityParser = new WebEntityParser(env);
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.linkDao = env.getConfigurator().get(LocalLinkDao.class);
        this.catDao = env.getConfigurator().get(LocalCategoryMemberDao.class);

        // Warm up necessary components
        for (Language l : env.getLanguages()) {
            LOG.info("warming up components for language: " + l);
            getSr(l);
            env.getConfigurator().get(Wikifier.class, "websail", "language", l.getLangCode());
        }

        LOG.info("warming up pagerank");
        LocalPage p = pageDao.get(new DaoFilter().setLimit(1)).iterator().next();
        linkDao.getPageRank(p.toLocalId());
    }

    @Override
    public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        WikiBrainWebRequest req = new WikiBrainWebRequest(target, request, httpServletRequest, httpServletResponse);
        LOG.info("received request for {}, URL {}?{}", target, request.getRequestURL(), request.getQueryString());

        try {
            // TODO: add logging
            if (target.equals("/languages")) {
                doLanguages(req);
            } else if (target.equals("/similarity")) {
                doSimilarity(req);
            } else if (target.equals("/cosimilarity")) {
                throw new UnsupportedOperationException();
            } else if (target.equals("/mostSimilar")) {
                doMostSimilar(req);
            } else if (target.equals("/wikify")) {
                doWikify(req);
            } else if (target.equals("/pageRank")) {
                doPageRank(req);
            } else if (target.equals("/articlesInCategory")) {
                doArticlesInCategory(req);
            } else if (target.equals("/categoriesForArticle")) {
                doCategoriesForArticle(req);
            }
        } catch (WikiBrainWebException e) {
            req.writeError(e);
        } catch (ConfigurationException e) {
            req.writeError(e);
        } catch (DaoException e) {
            req.writeError(e);
        }
    }

    private void doLanguages(WikiBrainWebRequest req) {
        List<String> langs = new ArrayList<String>();
        for (Language l : env.getLanguages()) {
            langs.add(l.getLangCode());
        }
        Collections.sort(langs);
        req.writeJsonResponse("languages", langs);
    }

    private SRMetric getSr(Language lang) throws ConfigurationException {
        return env.getConfigurator().get(SRMetric.class, "simple-ensemble", "language", lang.getLangCode());
    }

    private void doSimilarity(WikiBrainWebRequest req) throws ConfigurationException, DaoException {
        // TODO: support explanations
        Language lang = req.getLanguage();
        List<WebEntity> entities = entityParser.extractEntityList(req);
        if (entities.size() != 2) {
            throw new WikiBrainWebException("Similarity requires exactly two entities");
        }
        WebEntity entity1 = entities.get(0);
        WebEntity entity2 = entities.get(1);
        SRMetric sr = getSr(lang);
        SRResult r = null;
        switch (entity1.getType()) {
            case ARTICLE_ID: case TITLE:
                r = sr.similarity(entity1.getArticleId(), entity2.getArticleId(), false);
                break;
            case PHRASE:
                r = sr.similarity(entity1.getPhrase(), entity2.getPhrase(), false);
                break;
            default:
                throw new WikiBrainWebException("Unsupported entity type: " + entity1.getType());
        }
        Double sim = (r != null && r.isValid()) ? r.getScore() : null;
        req.writeJsonResponse("score", sim, "entity1", entity1.toJson(), "entity2", entity2.toJson());
    }

    private void doMostSimilar(WikiBrainWebRequest req) throws DaoException, ConfigurationException {
        Language lang = req.getLanguage();
        WebEntity entity = entityParser.extractEntity(req);
        int n = Integer.valueOf(req.getParam("n", "10"));
        SRMetric sr = getSr(lang);
        SRResultList results;
        switch (entity.getType()) {
            case ARTICLE_ID: case TITLE:
                results = sr.mostSimilar(entity.getArticleId(), n);
                break;
            case PHRASE:
                results = sr.mostSimilar(entity.getPhrase(), n);
                break;
            default:
                throw new WikiBrainWebException("Unsupported entity type: " + entity.getType());
        }
        List jsonResults = new ArrayList();
        for (SRResult r : results) {
            LocalPage page = pageDao.getById(lang, r.getId());
            Map obj = new HashMap();
            obj.put("articleId", r.getId());
            obj.put("score", r.getScore());
            obj.put("lang", lang.getLangCode());
            obj.put("title", page == null ? "Unknown" : page.getTitle().getCanonicalTitle());
            jsonResults.add(obj);
        }
        req.writeJsonResponse("results", jsonResults);
    }

    private void doPageRank(WikiBrainWebRequest req) throws ConfigurationException, DaoException {
        Language lang = req.getLanguage();
        WebEntity entity = entityParser.extractEntity(req);
        if (entity.getArticleId() < 0) {
            throw new WikiBrainWebException("articleId or title parameter required.");
        }
        int id = entity.getArticleId();
        double pageRank = linkDao.getPageRank(lang, id);
        req.writeJsonResponse(
                "article", pageJson(lang, id),
                "pageRank", pageRank
            );
    }

    private void doCategoriesForArticle(WikiBrainWebRequest req) throws ConfigurationException, DaoException {
        Language lang = req.getLanguage();
        WebEntity entity = entityParser.extractEntity(req);
        if (entity.getArticleId() < 0) {
            throw new WikiBrainWebException("articleId or title parameter required.");
        }
        Set<LocalPage> candidates = extractCategories(req, lang);
        boolean weighted = Boolean.valueOf(req.getParam("weighted", "true"));
        TIntDoubleMap distances = catDao.getCategoryDistances(candidates, entity.getArticleId(), weighted);
        List distanceJson = new ArrayList();
        for (int catId : WpCollectionUtils.sortMapKeys(distances, false)) {
            Map articleJson = pageJson(lang, catId);
            articleJson.put("distance", distances.get(catId));
            distanceJson.add(articleJson);
        }
        req.writeJsonResponse(
                "article", entity.toJson(),
                "distances", distanceJson
        );
    }

    private Set<LocalPage> extractCategories(WikiBrainWebRequest req, Language lang) throws DaoException {
        Set<LocalPage> candidates = new HashSet<LocalPage>();
        if (req.hasParam("categoryIds")) {
            String ids[] = req.getParam("categoryIds").split("\\|");
            for (String sid : ids) {
                LocalPage p = pageDao.getById(lang, Integer.valueOf(sid));
                if (p == null) throw new WikiBrainWebException("No " + lang + " article loaded with id " + sid);
                candidates.add(p);
            }
        } else if (req.hasParam("categoryTitles")) {
            String titles[] = req.getParam("categoryTitles").split("\\|");
            for (String t : titles) {
                LocalPage p = pageDao.getByTitle(lang, t);
                if (p == null) throw new WikiBrainWebException("No " + lang + " article loaded with title " + t);
                candidates.add(p);
            }
        } else {
            candidates = catDao.guessTopLevelCategories(lang);
            if (candidates == null || candidates.isEmpty()) {
                throw new WikiBrainWebException("No candidates specified and no top-level categories found.");
            }
        }
        return candidates;
    }

    private void doArticlesInCategory(WikiBrainWebRequest req) throws DaoException {
        Language lang = req.getLanguage();
        TIntSet pageIds = null;

        LocalPage target;
        if (req.hasParam("targetCategoryId")) {
            target = pageDao.getById(lang, Integer.valueOf(req.getParam("targetCategoryId")));
        } else if (req.hasParam("targetCategoryTitle")) {
            target = pageDao.getByTitle(lang, NameSpace.CATEGORY, req.getParam("targetCategoryTitle"));
        } else {
            throw new WikiBrainWebException("Either targetCategoryId or targetCategoryTitle must be specified");
        }

        if (req.hasParam("titles")) {
            pageIds = new TIntHashSet();
            for (String t : req.getParam("titles").split("\\|")) {
                int id = pageDao.getIdByTitle(t, lang, NameSpace.ARTICLE);
                if (id < 0) {
                    throw new WikiBrainWebException("No " + lang + " article loaded with title " + t);
                }
                pageIds.add(id);
            }
        } else if (req.hasParam("articleIds")) {
            for (String id : req.getParam("articleIds").split("\\|")) {
                pageIds.add(Integer.valueOf(id));
            }
        }
        Set<LocalPage> candidates = extractCategories(req, lang);
        if (!candidates.contains(target)) {
            throw new WikiBrainWebException("target category " + target + " not contained in [" + StringUtils.join(candidates) + "]");
        }

        boolean weighted = Boolean.valueOf(req.getParam("weighted", "true"));
        Map<LocalPage, TIntDoubleMap> distances = catDao.getClosestCategories(candidates, pageIds, weighted);
        final List distanceJson = new ArrayList();

        if (distances.containsKey(target)) {
            for (int pageId : WpCollectionUtils.sortMapKeys(distances.get(target), false)) {
                Map json = pageJson(lang, pageId);
                json.put("distance", distances.get(target).get(pageId));
                distanceJson.add(json);
            }
        }

        req.writeJsonResponse(
                "category", pageJson(target),
                "distances", distanceJson
        );
    }

    private Map pageJson(LocalPage p) {
        if (p == null) {
            return null;
        }
        Map json = new HashMap();
        json.put("articleId", p.getLocalId());
        json.put("title", p.getTitle().getCanonicalTitle());
        json.put("lang", p.getLanguage().getLangCode());
        return json;
    }

    private Map pageJson(Language lang, int pageId) throws DaoException {
        return pageJson(pageDao.getById(lang, pageId));
    }

    private void doWikify(WikiBrainWebRequest req) throws ConfigurationException, DaoException {
        Language lang = req.getLanguage();
        Wikifier wf = env.getConfigurator().get(Wikifier.class, "websail", "language", lang.getLangCode());
        String text = req.getParamOrDie("text");
        List jsonConcepts = new ArrayList();
        for (LocalLink ll : wf.wikify(text)) {
            LocalPage page = pageDao.getById(lang, ll.getDestId());
            Map obj = new HashMap();
            obj.put("index", ll.getLocation());
            obj.put("text", ll.getAnchorText());
            obj.put("lang", lang.getLangCode());
            obj.put("articleId", ll.getDestId());
            obj.put("title", page == null ? "Unknown" : page.getTitle().getCanonicalTitle());
            jsonConcepts.add(obj);
        }
        req.writeJsonResponse("text", text, "references", jsonConcepts);
    }

    public static void main(String args[]) throws Exception {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("port")
                        .withDescription("Server port number")
                        .create("p"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("listeners")
                        .withDescription("Size of listener queue")
                        .create("q"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("DumpLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();

        int port = Integer.valueOf(cmd.getOptionValue("p", "8000"));
        int queueSize = Integer.valueOf(cmd.getOptionValue("q", "100"));
        Server server = new Server(new QueuedThreadPool(queueSize, 20));
        server.setHandler(new WikiBrainServer(env));
        ServerConnector sc = new ServerConnector(server);
        sc.setPort(port);
        server.setConnectors(new Connector[]{sc});
        server.start();
        server.join();
    }
}
