package org.wikibrain.wikidata;

import org.wikidata.wdtk.dumpfiles.DumpContentType;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.MwDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.WmfDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.WmfDumpFileManager;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class WikidataDumpHelper {
    private DumpProcessingController controller;
    private static final String PROJECT = "wikidatawiki";
    private static final DumpContentType JSON_TYPE = DumpContentType.JSON;

    public WikidataDumpHelper() {
        this.controller = new DumpProcessingController(PROJECT);
    }

    public String getMostRecentDate() throws IOException {
        WmfDumpFileManager manager = controller.getWmfDumpFileManager();
        MwDumpFile file = manager.findMostRecentDump(JSON_TYPE);
        return file.getDateStamp();
    }

    public String getMostRecentFile() throws IOException {
        WmfDumpFileManager manager = controller.getWmfDumpFileManager();
        MwDumpFile file = manager.findMostRecentDump(DumpContentType.JSON);
        String tstamp = file.getDateStamp();
        return WmfDumpFile.getDumpFileName(JSON_TYPE, PROJECT, tstamp);
    }

    public String getMostRecentUrl() throws IOException {
        String baseUrl = WmfDumpFile.getDumpFileWebDirectory(JSON_TYPE, PROJECT);
        return baseUrl + getMostRecentFile();
    }

    public static void main(String args[]) throws IOException {
        WikidataDumpHelper helper = new WikidataDumpHelper();
        System.out.println(helper.getMostRecentDate());
        System.out.println(helper.getMostRecentUrl());
    }
}
