package org.wikibrain.atlasify;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by toby on 2/2/15.
 */
public class AtlasifyLogger {

    public static class logLogin{
        private String userId;
        private String browser;
        private String language;

        public logLogin(){

        }

        public logLogin(String userId, String browser, String language){
            this.userId = userId;
            this.browser = browser;
            this.language = language;
        }



        public String getUserId(){
            return userId;
        }

        public String getBrowser(){
            return browser;
        }

        public String getLanguage(){
            return language;
        }

    }

    public static class logQuery{
        private String userId;
        private String type;
        private String keyword;
        private String refSys;
        private String centroid;
        private String browser;
        private String language;

        public logQuery(){

        }

        public logQuery(String userId, String type, String keyword, String refSys, String centroid, String browser, String language){
            this.userId = userId;
            this.type = type;
            this.keyword = keyword;
            this.refSys = refSys;
            this.centroid = centroid;
            this.browser = browser;
            this.language = language;
        }

        public String getUserId(){
            return userId;
        }

        public String getType(){
            return type;
        }

        public String getKeyword(){
            return keyword;
        }

        public String getRefSys(){
            return refSys;
        }

        public String getCentroid(){
            return centroid;
        }

        public String getBrowser(){
            return browser;
        }

        public String getLanguage(){
            return language;
        }

    }

    CSVWriter logLoginWriter;
    CSVWriter logQueryWriter;

    AtlasifyLogger(String loginPath, String queryPath)throws IOException{
        logLoginWriter = new CSVWriter(new FileWriter(new File(loginPath), true), ',');
        logQueryWriter = new CSVWriter(new FileWriter(new File(queryPath), true), ',');
    }

    public void LoginLogger(logLogin data, String ip) throws IOException{
        String[] row = new String[5];
        row[0] = data.userId;
        row[1] = data.language;
        row[2] = data.browser;
        row[3] = ip;
        row[4] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        logLoginWriter.writeNext(row);
        logLoginWriter.flush();
    }

    public void QueryLogger(logQuery data, String ip) throws IOException{
        String[] row = new String[10];
        row[0] = data.userId;
        row[1] = data.centroid;
        row[2] = data.refSys;
        row[4] = data.keyword;
        row[5] = data.type;
        row[6] = data.browser;
        row[7] = data.language;
        row[8] = ip;
        row[9] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        logQueryWriter.writeNext(row);
        logQueryWriter.flush();
    }



}
