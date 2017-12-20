package carskit.alg.cars.transformation.modeling;

import carskit.alg.baseline.cf.ItemKNN;
import carskit.alg.baseline.cf.ItemKNNUnary;
import carskit.alg.baseline.cf.UserKNN;
import carskit.data.structure.SparseMatrix;
import carskit.generic.Recommender;
import happy.coding.io.LineConfiger;
import librec.data.MatrixEntry;

import java.security.Key;
import java.util.*;


public class LARS extends Recommender {

    private String rec;
    private String userID;
    private double userLat;
    private double userLong;
    private int k;
    private Map<Integer,Double> itemTravelPenalty;
    private double maxDistanceGlobal, minDistanceGlobal;
    private Map<String, Double> R;
    private ItemKNN recUsed;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec 	        = LARS.algoOptions.getString("-cf");
        this.userID 	    = LARS.algoOptions.getString("-user");
        this.userLat 	    = LARS.algoOptions.getDouble("-lat");
        this.userLong 	    = LARS.algoOptions.getDouble("-long");
        this.k              = LARS.algoOptions.getInt("-k");

        this.itemTravelPenalty = new HashMap<Integer,Double>();
        this.R = new LinkedHashMap<String,Double>();

        recUsed = new ItemKNN(trainMatrix, testMatrix, fold);
        // Model Building
        recUsed.execute();

        buildTravelPenaltyModel(trainMatrix, this.userLat, this.userLong);

        /*
        for(int i = 0; i < this.k; i++){
            double recScore, p, travelPenalty;

            Map.Entry<Integer, Double> minEntry = nextLowestTravelPenalty();
            p = recUsed.predict(Integer.parseInt(this.userID), minEntry.getKey());
            travelPenalty = normalizeDistance(minEntry.getValue(), 5, 1);

            recScore = p - travelPenalty;
            R.put(minEntry.getKey(),recScore);
        }
        */

        for(MatrixEntry me : trainMatrix){

            double recScore, p, travelPenalty;
            int itemID = rateDao.getItemIdFromUI(me.row());
            p = recUsed.predict(Integer.parseInt(this.userID), itemID);
            travelPenalty = normalizeDistance(itemTravelPenalty.get(itemID), 5, 1);
            recScore = p - travelPenalty;

            R.put(String.valueOf(itemID),recScore);
        }

        R = sortByValue(R);
        System.out.println(R.toString());
        System.out.println(maxDistanceGlobal);
        System.out.println(minDistanceGlobal);



        //writeMatrix(trainMatrix);
        //writeMatrix(testMatrix);


        /*
        //Recommender recUsed = getRecommender(trainMatrix, testMatrix, fold);

        ItemKNN recUsed = new ItemKNN(trainMatrix, testMatrix, fold);
        // Model Building
        recUsed.execute();

        for(MatrixEntry me : testMatrix){
            double predictRating;


            int userID = rateDao.getUserIdFromUI(me.row());
            int itemID = rateDao.getItemIdFromUI(me.row());

            String[] contextIDs = rateDao.getContextId(me.column()).split(",");

            for(String contextID : contextIDs){

                predictRating = recUsed.predict(0,itemID);
                System.out.println("userID: " + userID + "\t\titemID: " + itemID +
                        "\t\tcontextID: " + contextID + "\t\tRating: " +predictRating);
            }

        }
        */


    }

    @Override
    public double predict(int u, int j, int c) throws Exception {

        double recScore, p, travelPenalty;

        p = recUsed.predict(u, j);
        travelPenalty = normalizeDistance(itemTravelPenalty.get(j), 5, 1);
        recScore = p - travelPenalty;
        System.out.println(recScore + "=" + p + "-" + travelPenalty);
        return recScore;
    }

    public static Map<String, Double> sortByValue(Map<String, Double> unsorted_map){

        Map<String, Double> sorted_map = new LinkedHashMap<String, Double>();

        try{
            // 1. Convert Map to List of Map
            List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsorted_map.entrySet());

            // 2. Sort list with Collections.sort(), provide a custom Comparator
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> o1,
                                   Map.Entry<String, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
            for (Map.Entry<String, Double> entry : list) {
                sorted_map.put(entry.getKey(), entry.getValue());
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }

        return sorted_map;

    }

    private Double normalizeDistance(Double distance, int max, int min){
        double value = (distance - minDistanceGlobal) / (maxDistanceGlobal - minDistanceGlobal);
        return value * (max - min) + min;
    }

    /*
    private Map.Entry<Integer,Double> nextLowestTravelPenalty(){
        Map.Entry<Integer, Double> minEntry = null;
        for (Map.Entry<Integer, Double> entry : this.itemLowestTravelPenalty.entrySet()){
            if (minEntry == null || minEntry.getValue().compareTo(entry.getValue()) > 0)
            {
                minEntry = entry;
            }
        }
        itemLowestTravelPenalty.remove(minEntry.getKey());
        return minEntry;
    }
    */
    /*
    private void buildTravelPenaltyModel(SparseMatrix matrix, double userLat, double userLong){

        int i = 0;
        int itemID, maxID;
        double itemLat = 0;
        double itemLong = 0;
        double euclideanDistance, maxDistance, keyDistance;



        for(MatrixEntry me : matrix){

            itemID = rateDao.getItemIdFromUI(me.row());
            String[] contexts = rateDao.getContextId(me.column()).split(",");

            itemLat = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
            itemLong = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

            euclideanDistance = Math.sqrt((Math.pow(userLat - itemLat, 2.0) + Math.pow(userLong - itemLong,2.0)));

            Map.Entry<Integer, Double> maxEntry = null;
            for (Map.Entry<Integer, Double> entry : itemLowestTravelPenalty.entrySet()){
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }
            // Achando máximo global
            if(maxEntry != null && maxEntry.getValue() > this.maxDistanceGlobal) {
                this.maxDistanceGlobal = maxEntry.getValue();
            }

            if(maxEntry != null && euclideanDistance < maxEntry.getValue() && this.itemLowestTravelPenalty.size() > this.k){
                itemLowestTravelPenalty.remove(maxEntry.getKey());
                itemLowestTravelPenalty.put(itemID, euclideanDistance);
            }else{
                itemLowestTravelPenalty.put(itemID, euclideanDistance);
            }

        }

        // Achando mínimo global
        Map.Entry<Integer, Double> minEntry = null;
        for (Map.Entry<Integer, Double> entry : itemLowestTravelPenalty.entrySet()){
            if (minEntry == null || minEntry.getValue().compareTo(entry.getValue()) > 0)
            {
                minEntry = entry;
            }
        }

        if(minEntry != null){
            this.minDistanceGlobal = minEntry.getValue();
        }

        //System.out.println("maxDistanceGlobal: " + maxDistanceGlobal + "\t" + "minDistanceGlobal: " + minDistanceGlobal);
        //System.out.println(this.itemLowestTravelPenalty.toString());

    }
    */
    private void buildTravelPenaltyModel(SparseMatrix matrix, double userLat, double userLong){
        int i = 0;
        int itemID, maxID;
        double itemLat = 0;
        double itemLong = 0;
        double euclideanDistance;
        Boolean first = true;

        for(MatrixEntry me : matrix){

            itemID = rateDao.getItemIdFromUI(me.row());
            String[] contexts = rateDao.getContextId(me.column()).split(",");

            itemLat = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
            itemLong = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

            euclideanDistance = Math.sqrt((Math.pow(userLat - itemLat, 2.0) + Math.pow(userLong - itemLong,2.0)));

            if(first || euclideanDistance < minDistanceGlobal){
                minDistanceGlobal = euclideanDistance;
            }

            if(first || euclideanDistance > maxDistanceGlobal){
                maxDistanceGlobal = euclideanDistance;
            }
            first = false;


            itemTravelPenalty.put(itemID, euclideanDistance);
        }
    }



    private Recommender getRecommender(SparseMatrix train, SparseMatrix test, int fold){

        Recommender recsys = null;
        switch (this.rec) {
            case "itemknn":
                recsys = new ItemKNN(train, test, fold);
                break;
            case "itemknnunary":
                recsys = new ItemKNNUnary(train, test,fold);
                break;
            case "userknn":
                recsys = new UserKNN(train, test, fold);
                break;
        }
        System.out.println("Collaborative filtering algorithm " + this.rec.toUpperCase() + "\n");

        return recsys;
    }

    private void writeMatrix(SparseMatrix matrix){
        try{

            for(MatrixEntry me : matrix){
                StringBuilder sb  = new StringBuilder();
                String[] contexts = rateDao.getContextId(me.column()).split(",");

                sb.append("User CARSKit: " + rateDao.getUserIdFromUI(me.row()));
                sb.append("\t");
                sb.append("User: " + rateDao.getUserId(rateDao.getUserIdFromUI(me.row())));
                sb.append("\t");
                sb.append("Item: " + rateDao.getItemId(rateDao.getItemIdFromUI(me.row())));
                sb.append("\t");
                sb.append("Contexts: ");
                for(int i = 0; i < contexts.length; i++){
                    sb.append(rateDao.getContextConditionId(Integer.parseInt(contexts[i])));
                    sb.append("\t");
                }
                System.out.println(sb.toString() + "\n");

            }
        }catch(Exception e){

        }
    }
}

// DAO SegmentFinder Combined(item knn) 206
