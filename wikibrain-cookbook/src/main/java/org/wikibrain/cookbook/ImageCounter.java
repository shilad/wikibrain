package org.wikibrain.cookbook;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.apache.commons.lang.StringUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by toby on 10/15/14.
 */
public class ImageCounter {


    static LocalPageDao lpDao;
    static RawPageDao rpDao;
    public ImageCounter(Env env) throws ConfigurationException{

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        lpDao = configurator.get(LocalPageDao.class);
        rpDao = configurator.get(RawPageDao.class);
    }

    public static String removeTemp(String body){
        while(body.contains("{{") && body.contains("}}")){
            int startpoint = body.indexOf("{{");
            int endpoint = body.indexOf("}}");
            if(endpoint <= startpoint)
                return body;
            body = body.substring(0, startpoint) + body.substring(endpoint + 2, body.length());

        }

        return body;
    }

    public static boolean checkPicture(String body){
        Set<String> imageNameSet = new HashSet<String>();
        imageNameSet.add(".jpg");
        imageNameSet.add(".JPG");
        imageNameSet.add(".jpeg");
        imageNameSet.add(".JPEG");
        imageNameSet.add(".gif");
        imageNameSet.add(".GIF");
        imageNameSet.add(".png");
        imageNameSet.add(".PNG");
        imageNameSet.add(".tiff");
        imageNameSet.add(".TIFF");
        imageNameSet.add(".svg");
        imageNameSet.add(".SVG");
        for(String s : imageNameSet){
            if(body.contains(s))
                return true;
        }
        return false;

    }
    public static String removeGallery(String body){
        while(body.contains("<gallery>") && body.contains("</gallery>")){
            int startIndex = body.indexOf("<gallery>");
            int endIndex = body.indexOf("</gallery>");
            if(endIndex <= startIndex)
                return body;
            if(endIndex + 10 >= body.length())
                return  body;
            body = body.substring(0, startIndex) + body.substring(endIndex + 10, body.length());
        }
        return body;
    }
	public static int countImage(String body, boolean countGallery) {
		
		int count = 0;
		if(countGallery == false)
			body = removeGallery(body);
		count += StringUtils.countMatches(body, ".jpg");
		count += StringUtils.countMatches(body, ".JPG");
		count += StringUtils.countMatches(body, ".jpeg");
		count += StringUtils.countMatches(body, ".JPEG");
		count += StringUtils.countMatches(body, ".gif");
		count += StringUtils.countMatches(body, ".GIF");
		count += StringUtils.countMatches(body, ".png");
		count += StringUtils.countMatches(body, ".PNG");
		count += StringUtils.countMatches(body, ".tiff");
		count += StringUtils.countMatches(body, ".TIFF");
		count += StringUtils.countMatches(body, ".svg");
		count += StringUtils.countMatches(body, ".SVG");
		return count;
		
		
		
	}
	/*
    public static int countImage(String body, boolean countGallery) {
        int count = 0;
        while(body.contains("File:") || body.contains("Image:")){
            if (countGallery == false)
                body = removeGallery(body);
            int firstFileIndex = body.indexOf("File:");
            int firstImageIndex = body.indexOf("Image:");
            int firstIndex;
            if(firstFileIndex == -1)
                firstIndex = firstImageIndex;
            else if(firstImageIndex == -1)
                firstIndex = firstFileIndex;
            else
                firstIndex = Math.min(firstFileIndex, firstImageIndex);
            if(firstIndex < 0)
                return 0;
            int lastIndex = body.substring(firstIndex).indexOf("\n");
            if(lastIndex < 0 ){
                if(checkPicture(body.substring(firstIndex)))
                    return 1;
                else
                    return 0;
            }


            if(checkPicture(body.substring(firstIndex, firstIndex + lastIndex))){
                count ++;
                body = body.substring(firstIndex + lastIndex);

            }
            else
                body = body.substring(firstIndex + lastIndex);


        }
        return count;

    }
		*/

    public static int getImageCount(LocalPage localPage, boolean getGallery) throws DaoException{
        return countImage(removeTemp(rpDao.getBody(localPage.getLanguage(), localPage.getLocalId())), getGallery);
    }

    public static int getImageCount(Integer LocalID, Language language, boolean getGallery) throws DaoException{
        return countImage(removeTemp(rpDao.getBody(language, LocalID)), getGallery);
    }

    public static void main(String args[]) throws Exception {

        // Prepare the environment
        Env env = EnvBuilder.envFromArgs(args);
        Configurator configurator = env.getConfigurator();
        LocalPageDao lpDao = configurator.get(LocalPageDao.class);
        RawPageDao rpDao = configurator.get(RawPageDao.class);

        ImageCounter imageCounter = new ImageCounter(env);

        //out.println(rp.getBody());
        System.out.println("************");

        System.out.println(imageCounter.getImageCount(lpDao.getByTitle(Language.SIMPLE, "National Gallery, London"), true));
        System.out.println(imageCounter.getImageCount(lpDao.getByTitle(Language.SIMPLE, "National Gallery, London"), false));

    }



}
