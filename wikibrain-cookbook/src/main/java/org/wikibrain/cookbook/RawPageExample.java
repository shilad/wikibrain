package org.wikibrain.cookbook;

import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.phrases.PhraseAnalyzer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by toby on 10/15/14.
 */
public class RawPageExample {

    public static String removeTemp(String body){
        int startpoint = 0;
        int endpoint = 0;
        int count = 0;
        boolean flag = false;
        int length = body.length();
        for (int i = 0; i < length - 1; i++){
            if(body.substring(i, i+2).equals("{{")){
                if(flag == false){
                    flag = true;
                    startpoint = i;
                }
                count ++;
            }
            if(body.substring(i, i+2).equals("}}")){
                count --;
                if (count <= 0 && flag == true){
                    endpoint = i+2;
                    return removeTemp(body.substring(0, startpoint) + (body.substring(endpoint, length)));
                }

            }

        }
        if(endpoint == startpoint)
            return body;
        return removeTemp(body.substring(0, startpoint) + (body.substring(endpoint, length)));
    }

    public static boolean checkPicture(String body){
        Set<String> imageNameSet = new HashSet<String>();
        imageNameSet.add(".jpg");
        imageNameSet.add(".JPG");
        imageNameSet.add(".jpeg");
        imageNameSet.add(".JPED");
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
        int startIndex = body.indexOf("<gallery>");
        int endIndex = body.indexOf("</gallery>");
        if(startIndex == -1 || endIndex == -1)
            return body;
        else
            return removeGallery(body.substring(0, startIndex) + body.substring(endIndex + 10, body.length()));
    }
    public static int countImage(String body, boolean countGallery) {
        if (countGallery == false)
            body = removeGallery(body);
        body = removeTemp(body);
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


        if(checkPicture(body.substring(firstIndex, firstIndex + lastIndex)))
            return 1 + countImage(body.substring(firstIndex + lastIndex), true);
        else
            return 0 + countImage(body.substring(firstIndex + lastIndex), true);

    }

    public static void main(String args[]) throws Exception {

        // Prepare the environment
        Env env = EnvBuilder.envFromArgs(args);

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        LocalPageDao lpDao = configurator.get(LocalPageDao.class);
        RawPageDao rpDao = configurator.get(RawPageDao.class);
        LocalPage lp = lpDao.getByTitle(Language.SIMPLE, NameSpace.ARTICLE, "National Gallery, London");
        RawPage rp = rpDao.getById(Language.SIMPLE, lp.getLocalId());

        //out.println(rp.getBody());
        System.out.println("************");

        System.out.println(countImage(rp.getBody(), true));
        System.out.println(countImage(rp.getBody(), false));

    }



}
