package org.wikibrain.spatial.loader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Created by vonea001 on 6/23/14.
 */
public class ShapeFileInterpreter {

    private final GADMConverter gadmConverter = new GADMConverter();

    /**
     * Copied from GADMConverter
     *
     * @param outputFolder
     * @param level        //TODO: reduce memory usage
     *                     Converts raw GADM shapefile into WikiBrain readable type
     *                     Recommended JVM max heapsize = 4G
     */
//    public void convertShpFile(File rawFile, SpatialDataFolder outputFolder, int level) throws IOException, WikiBrainException {
//        if ((level != 0) && (level != 1)) throw new IllegalArgumentException("Level must be 0 or 1");
//
//        File outputFile = new File(outputFolder.getRefSysFolder("earth").getCanonicalPath() + "/" + "gadm" + level + ".shp");
//        ListMultimap<String, String> countryStatePair = ArrayListMultimap.create();
//
//        final SimpleFeatureType WIKITYPE = getOutputFeatureType(level);
//
//        final SimpleFeatureSource outputFeatureSource = getOutputDataFeatureSource(outputFile, WIKITYPE);
//
//        final Transaction transaction = new DefaultTransaction("create");
//
//        final SimpleFeatureCollection inputCollection = gadmConverter.getInputCollection(rawFile);
//        SimpleFeatureIterator inputFeatures = inputCollection.features();
//
//        final ConcurrentLinkedQueue<List<SimpleFeature>> writeQueue = new ConcurrentLinkedQueue<List<SimpleFeature>>();
//
//        try {
//
//            while (inputFeatures.hasNext()) {
//                SimpleFeature feature = inputFeatures.next();
//                String country = ((String) feature.getAttribute(4)).intern();
//                String state = ((String) feature.getAttribute(6)).intern();
//                if (!countryStatePair.containsEntry(country, state))
//                    countryStatePair.put(country, state);
//            }
//
//            final Multimap<String, String> countryState = countryStatePair;
//
//            inputFeatures.close();
//
//
//            final SimpleFeatureCollection levelOneInput = getInputCollection(outputFolder.getMainShapefile("gadm1", RefSys.EARTH));
//
//            exceptionList = new ArrayList<String>();
//
//            LOG.log(Level.INFO, "Start processing polygons for level " + level + " administrative districts.");
//
//
//            if (level == 1) {
//                for (String country : countryState.keySet()) {
//
//                    ParallelForEach.loop(countryState.get(country), new Procedure<String>() {
//                        @Override
//                        public void call(String state) throws Exception {
//
//                            List<SimpleFeature> features = inputFeatureHandler(inputCollection, state, 1, WIKITYPE, countryState);
//                            writeQueue.add(features);
//                            writeToShpFile(outputFeatureSource, WIKITYPE, transaction, writeQueue.poll());
//                        }
//                    });
//                }
//
//
//            } else {
//
//                ParallelForEach.loop(countryState.keySet(), new Procedure<String>() {
//                    @Override
//                    public void call(String country) throws Exception {
//
//                        List<SimpleFeature> features = inputFeatureHandler(levelOneInput, country, 0, WIKITYPE, countryState);
//                        writeQueue.add(features);
//                        writeToShpFile(outputFeatureSource, WIKITYPE, transaction, writeQueue.poll());
//
//                    }
//                });
//
//                /*LOG.log(Level.INFO, "Start processing polygons where exceptions occurred.");
//                int count = 0;
//                for (String country : exceptionList) {
//                    count++;
//                    LOG.log(Level.INFO, "Combining polygons for " + country + "(" + count + "/" + exceptionList.size() + ")");
//                    List<SimpleFeature> features = inputFeatureHandler(levelOneInput, country, 0, WIKITYPE, countryState);
//                    writeQueue.add(features);
//                    writeToShpFile(outputFeatureSource, WIKITYPE, transaction, writeQueue.poll());
//                }*/
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            transaction.close();
//            countryCount.set(0);
//        }
//
//
//    }
}
