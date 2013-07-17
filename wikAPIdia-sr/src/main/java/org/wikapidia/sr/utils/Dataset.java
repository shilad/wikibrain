package org.wikapidia.sr.utils;

import org.wikapidia.core.lang.Language;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */
public class Dataset {
    public Language language;
    public List<KnownSim> data;

    public Dataset(Language language, List<KnownSim> data) {
        this.language = language;
        this.data = data;
    }

    public Dataset(List<Dataset> datasets) {
        this.language = datasets.get(0).getLanguage();
    }

    public Dataset(Language language, String path) throws IOException {
        this.language = language;
        this.data = KnownSim.read(new File(path));
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public List<KnownSim> getData() {
        return data;
    }

    public void setData(List<KnownSim> data) {
        this.data = data;
    }
}
