package org.wikibrain.spatial.maxima;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.io.FileUtils;
import org.wikibrain.spatial.cookbook.tflevaluate.MatrixGenerator;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class ExportEnhancer {

    private Map<String, Integer> stringIdMap;
    private Map<Integer,String> idsStringMap;

    private Map<Integer,Integer> idToIndexForDistanceMatrix;
    private Map<Integer,Integer> idToIndexForSRMatrix;
    private Map<Integer,Integer> idToIndexForGraphMatrix;
    private Map<Integer, Integer> idToScaleCategory;

    private float[][] distanceMatrix;
    private float[][] srMatrix;
    private float[][] graphMatrix;
    private Map<String, Set<Integer>> neighbors;
    private Map<String, String> oldNeighbors;
    private Map<Integer, Integer> pageRanks;
    private List<String> locations;

    public static final String fileBase = "/scratch/results/";//"/Users/shilad/Documents/IntelliJ/geo-sr/SRSurvey/dat/";


    public ExportEnhancer() throws IOException {
        buildIdsStringMap();
        readMatrices();
        readInIdToScaleInfo();
        readNeighbors();
        fixNeighbors();
        readPopularity();
    }

    private void readPopularity() throws IOException {
        pageRanks = new HashMap<Integer, Integer>();
        int rank = 1;
        for (String line : FileUtils.readLines(new File("PageHitListFullEnglish.txt"))) {
            String tokens[] = line.trim().split("\t", -1);
            pageRanks.put(Integer.valueOf(tokens[0]), rank++);
        }
    }

    private void readMatrices() {
        MatrixGenerator.MatrixWithHeader distanceMatrixWithHeader = MatrixGenerator.loadMatrixFile("distancematrix_en");
        distanceMatrix = distanceMatrixWithHeader.matrix;
        idToIndexForDistanceMatrix= distanceMatrixWithHeader.idToIndex;

        MatrixGenerator.MatrixWithHeader srMatrixWithHeader = MatrixGenerator.loadMatrixFile("srmatrix_en");
        srMatrix= srMatrixWithHeader.matrix;
        idToIndexForSRMatrix= srMatrixWithHeader.idToIndex;

        MatrixGenerator.MatrixWithHeader graphMatrixWithHeader = MatrixGenerator.loadMatrixFile("graphmatrix_en");
        graphMatrix= graphMatrixWithHeader.matrix;
        idToIndexForGraphMatrix= graphMatrixWithHeader.idToIndex;
    }

    private void buildIdsStringMap() throws FileNotFoundException {
        stringIdMap = new HashMap<String, Integer>();
        idsStringMap= new HashMap<Integer, String>();
        File file = new File("IDsToTitles.txt");
        Scanner scanner = new Scanner(file);
        while(scanner.hasNextLine()){
            String next= scanner.nextLine();
            java.util.StringTokenizer st= new java.util.StringTokenizer(next,"\t",false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            idsStringMap.put(id, name);
            stringIdMap.put(name, id);
        }
        scanner.close();
    }


    private void readInIdToScaleInfo() throws FileNotFoundException{
        idToScaleCategory= new HashMap<Integer, Integer>();
        Scanner scanner= new Scanner(new File("geometryToScale.txt"));
        for (int i = 0; i <7 ; i++) { //Throw out the first 7 lines because they are information
            scanner.nextLine();
        }
        while(scanner.hasNextLine()){
            String s= scanner.nextLine();
            String[] info= s.split("\t", -1);
            idToScaleCategory.put(Integer.parseInt(info[0]),Integer.parseInt(info[1]));
        }
    }

    public Map<String, Set<Integer>> readNeighbors() throws FileNotFoundException {
        Scanner scan = new Scanner(new File("citiesToNeighbors4.txt"));
        this.neighbors = new HashMap<String, Set<Integer>>();
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            Set<Integer> set = new HashSet<Integer>();
            String id = array[0];
            double population = Double.parseDouble(array[1]);
            for (int i = 2; i < array.length; i++) {
                set.add(Integer.parseInt(array[i]));
            }
            neighbors.put(id, set);
        }
        return neighbors;
    }

    public void fixNeighbors() throws FileNotFoundException {
        Map<Integer,String>[] cities = new Map[2];
        String[] files = {"cities1000.txt","cities1000.old.txt"};
        int newCities = 0, oldCities = 1;
        // country matching
        String[] array2 = {"United States of America", "Canada", "Brazil", "Pakistan", "India", "France", "Spain", "United Kingdom", "Australia"};
        String[] countryCodes = {"US", "CA", "BR", "PK", "IN", "FR", "ES", "GB", "AU"};
        // codes to country names
        Map<String, String> countryNames = new HashMap<String, String>();
        for (int i = 0; i < countryCodes.length; i++) {
            countryNames.put(countryCodes[i], array2[i]);
        }
        // set of codes
        Set<String> countries = new HashSet<String>();
        countries.addAll(Arrays.asList(countryCodes));
        // read in file that gives conversion from state code names to state actual names
        Map<String, String> stateCodesToNames = new HashMap<String, String>();
        Scanner scan = new Scanner(new File("admin1CodesASCII.txt"));
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] array = line.split("\t");
            stateCodesToNames.put(array[0], array[1]);
        }
        scan.close();
        // read city names
        for (int i=0; i<2; i++){
            scan = new Scanner(new File(files[i]));
            cities[i] = new HashMap<Integer, String>();
            while(scan.hasNextLine()) {
                String line = scan.nextLine();
                String[] array = line.split("\t");
                String country = array[8];

                // if it's in a country we care about
                if (countries.contains(country)) {
                    String name = array[1];
                    String state = array[10];
                    Integer id = Integer.parseInt(array[0]);

                    // store the information
                    state = stateCodesToNames.get(country + "." + state);
                    country = countryNames.get(country);
                    name = country+","+state+","+name;
                    cities[i].put(id,name);
                }
            }
            scan.close();
        }
        oldNeighbors = new HashMap<String, String>();
        for (Integer id: cities[oldCities].keySet()){
            oldNeighbors.put(cities[oldCities].get(id),cities[newCities].get(id));
        }

    }

    public void enhance(File personFile, File questionFile, File newQuestionFile) throws IOException {
        Map<String, Set<Integer>> knownLocations = readKnownLocations(personFile);
        BufferedReader reader = WpIOUtils.openBufferedReader(questionFile);
        String header[] = reader.readLine().trim().split("\t", -1);

        BufferedWriter writer = WpIOUtils.openWriter(newQuestionFile);
        writeRow(writer, header, "km", "graph", "sr", "wpId1", "wpId2", "scale1", "scale2", "pop1", "pop2", "known1", "known2");

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            try {
                enhanceLine(header, knownLocations, line, writer);
            }catch (Exception e) {
                e.printStackTrace();
                System.err.println("error enhancing line: " + line.trim());
            }
        }
        writer.close();
        reader.close();
    }

    private void enhanceLine(String[] header, Map<String, Set<Integer>> knownLocations, String line, BufferedWriter writer) throws IOException {
        // Find useful column values
        int workerCol = -1;
        int location1Col = -1;
        int location2Col = -1;
        for (int i = 0; i < header.length; i++) {
            if (header[i].equals("amazonId")) {
                workerCol = i;
            } else if (header[i].equals("location1")) {
                location1Col = i;
            } else if (header[i].equals("location2")) {
                location2Col = i;
            }
        }

        if (line.endsWith("\n")) { line = line.substring(0, line.length() - 1); }
        String [] tokens = line.split("\t", -1);

        // If you don't trim, bad things happen
        String workerId = tokens[workerCol].trim();
        int wpId1 = -1, wpId2 = -1, pageRank1 = -1, pageRank2 = -1, scale1 = -1, scale2 = -1;
        double graphDist = -1.0, kmDist = -1.0, srDist = -1.0;

        try {

            wpId1 = stringIdMap.get(tokens[location1Col]);
            wpId2 = stringIdMap.get(tokens[location2Col]);

            scale1 = idToScaleCategory.get(wpId1);
            scale2 = idToScaleCategory.get(wpId2);
            pageRank1 = pageRanks.get(wpId1);
            pageRank2 = pageRanks.get(wpId2);

            graphDist = graphMatrix[idToIndexForGraphMatrix.get(wpId1)][idToIndexForGraphMatrix.get(wpId2)];
            kmDist = distanceMatrix[idToIndexForDistanceMatrix.get(wpId1)][idToIndexForDistanceMatrix.get(wpId2)];
            srDist = srMatrix[idToIndexForSRMatrix.get(wpId1)][idToIndexForSRMatrix.get(wpId2)];

        } catch (Exception e) {
            System.err.println("didn't find information for line " + line.trim());
        }

        writeRow(writer, tokens,
                kmDist, graphDist, srDist,
                wpId1, wpId2,
                scale1, scale2,
                pageRank1, pageRank2,
                knownLocations.get(workerId).contains(wpId1),
                knownLocations.get(workerId).contains(wpId2)
            );
    }

    private void writeRow(BufferedWriter writer, String [] originalRow, Object ... newCols) throws IOException {
        for (int i = 0; i < originalRow.length; i++) {
            if (i > 0) {
                writer.write("\t");
            }
            writer.write(originalRow[i]);
        }
        for (Object o : newCols) {
            writer.write("\t" + o.toString());
        }
        writer.write("\n");
    }

    public Map<String, Set<Integer>> readKnownLocations(File personFile) throws IOException {
        Map<String, Set<Integer>> knownLocations = new HashMap<String, Set<Integer>>();

        int numFields = -1;
        int idCol = -1;
        TIntList locationCols = new TIntArrayList();

        locations = new ArrayList<String>();
        for (String line : FileUtils.readLines(personFile)) {
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length()-1);
            }
            String tokens[] = line.split("\t", -1);

            // Grab location columns for header
            if (numFields < 0) {
                numFields = tokens.length;
                for (int i = 0;i < tokens.length; i++) {
                    if (tokens[i].startsWith("home_")) {
                        locationCols.add(i);
                    }
                    if (tokens[i].equals("amazonId")) {
                        idCol = i;
                    }
                }
                continue;
            }

            if (tokens.length != numFields) {
                System.err.println("invalid line: '" + line + "'");
                continue;
            }


            if (line.contains("A18FSRHD10KF0Y")){
                System.out.println(line);
            }

            String id = tokens[idCol].trim();
            Set<Integer> known = new HashSet<Integer>();
            for (int i : locationCols.toArray()) {
                String s = tokens[i].trim();
                if (s.length() == 0) {
                    continue;
                }
                if (!s.contains("|")) {
                    continue;   // country
                }
                s = s.replaceAll("\\|", ",");

                if (neighbors.containsKey(s)) {
                    locations.add(s);
                    known.addAll(neighbors.get(s));
                } else {
                    if (oldNeighbors.get(s) != null) {
                        locations.add(oldNeighbors.get(s));
                        known.addAll(neighbors.get(oldNeighbors.get(s)));
                    } else {
                        System.err.println("unknown city "+s+" for turker: " + id);
                    }
                }
                if (id.equals("A18FSRHD10KF0Y")){
                    System.out.println(known.size());
                }
            }

            knownLocations.put(id, known);
        }

        return knownLocations;
    }

    public static void main(String args[]) throws IOException {
        ExportEnhancer enhancer = new ExportEnhancer();
        enhancer.enhance(
                new File(fileBase+"people.tsv"),
                new File(fileBase+"questions.tsv"),
                new File(fileBase+"questions.enhanced.tsv")
        );
    }

    public List<String> getLocations(){
        return locations;
    }
}
