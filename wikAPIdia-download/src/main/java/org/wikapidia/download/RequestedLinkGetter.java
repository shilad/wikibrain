package org.wikapidia.download;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 *
 */
public class RequestedLinkGetter {

//    private String requestDate;    // This is the date requested by the user.
//
//    private List<Date> getAvailableDates() {
//        List<Date> availableDate = new ArrayList<Date>();
//        try {
//            String langWikiPage = IOUtils.toString(new URL(getLanguageWikiUrl()).openStream());
//            List<String> availableLinks = getLinks(langWikiPage);
//            for (String availableLink :availableLinks) {
//                if (availableLink.matches("\\d{8}/")) {
//                    availableDate.add(stringToDate(availableLink.substring(0,8)));
//                }
//                return availableDate;
//            }
//        } catch (MalformedURLException e) {
//            System.err.println("Invalid URL to langWiki index page.");
//            e.printStackTrace();
//        } catch (IOException e) {
//            System.err.println("Can't get content of langWiki index page.");
//            e.printStackTrace();
//        } catch (java.text.ParseException e) {
//            System.err.println("Can't parse string to date.");
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    private String dateSelecter(List<Date> dateList) throws java.text.ParseException {
//        Date selectDate = dateList.get(0);
//        for (Date date : dateList) {
//            if (date.before(stringToDate(requestDate)) && (date.after(selectDate) || selectDate.after(stringToDate(requestDate)))) {
//                selectDate = date;
//            }
//        }
//        if (selectDate.before(stringToDate(requestDate))) {
//            return new SimpleDateFormat("yyyyMMdd").format(selectDate);
//        }
//        return null;
//    }
}
