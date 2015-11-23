package org.wikibrain.webapi;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.server.Request;
import org.json.simple.JSONObject;
import org.wikibrain.core.lang.Language;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class WikiBrainWebRequest {

    // For timings
    private ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    private long startCpuTime;
    private long startUserTime;
    private long elapsedUserTime;
    private long elapsedCpuTime;

    private final String target;
    private final Request request;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    public WikiBrainWebRequest(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        startCpuTime = getCpuTime();
        startUserTime = getUserTime();

        this.target = target;
        this.request = request;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }

    private long getUserTime() {
        return bean.isCurrentThreadCpuTimeSupported( ) ? bean.getCurrentThreadUserTime() : 0L;
    }

    private long getCpuTime() {
        return bean.isCurrentThreadCpuTimeSupported( ) ? bean.getCurrentThreadCpuTime( ) : 0L;
    }

    public String getParam(String key) {
        return request.getParameter(key);
    }

    public String getParam(String key, String defaultValue) {
        return hasParam(key) ? getParam(key) : defaultValue;
    }

    public Language getLanguage() {
        String code = getParamOrDie("lang");
        if (!Language.hasLangCode(code)) {
            throw new WikiBrainWebException("Unknown language code: " + code);
        }
        return Language.getByLangCode(code);
    }

    public boolean hasParam(String key) {
        String value = getParam(key);
        return (value != null && !value.trim().isEmpty());
    }

    public String getParamOrDie(String key) {
        String val = getParam(key);
        if (val == null) {
            throw new WikiBrainWebException("Missing parameter " + key);
        } else {
            return val;
        }
    }

    public String getUserToken() {
        return "";    // TODO: this should return a unique token describing the user.
    }

    /**
     * Writes a new json response whose results hashmap contains one key and one value.
     * @param keysAndValues key1, value1, key2, value2, ...
     */
    public void writeJsonResponse(Object ...keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        Map obj = new HashMap();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            if (!(keysAndValues[i] instanceof String)) {
                throw new IllegalArgumentException("Invalid key: " + keysAndValues[i]);
            }
            obj.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        writeJsonResponse(obj);
    }

    public void writeJsonResponse(Map object) {
        if (!object.containsKey("success")) {
            object.put("success", true);
        }
        if (!object.containsKey("message")) {
            object.put("message", "");
        }

        this.elapsedCpuTime = getCpuTime() - startCpuTime;
        this.elapsedUserTime = getUserTime() - startUserTime;
        Map<String, Double> resources = new HashMap<String, Double>();
        resources.put("cpuTime", elapsedCpuTime / 1.0E9);
        resources.put("userTime", elapsedUserTime / 1.0E9);
        object.put("diagnostics", resources);

        httpServletResponse.setContentType("application/json;charset=utf-8");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        try {
            httpServletResponse.getWriter().println(JSONObject.toJSONString(object));
        } catch (IOException e) {
            throw new WikiBrainWebException(e);
        }
        request.setHandled(true);
    }

    public void writeError(Exception e) {
        Map<String, Object> errorObj = new HashMap<String, Object>();
        errorObj.put("message", e.getMessage());
        errorObj.put("type", e.getClass().getName());
        errorObj.put("details", ExceptionUtils.getStackTrace(e));

        writeJsonResponse(
                "success", false,
                "message", e.getMessage(),
                "error", errorObj
        );
    }
}
