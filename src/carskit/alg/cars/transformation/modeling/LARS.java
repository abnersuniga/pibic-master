package carskit.alg.cars.transformation.modeling;

import carskit.alg.baseline.cf.ItemKNN;
import carskit.alg.baseline.cf.ItemKNNUnary;
import carskit.alg.baseline.cf.UserKNN;
import carskit.data.structure.SparseMatrix;
import carskit.generic.Recommender;
import happy.coding.io.LineConfiger;
import happy.coding.io.Logs;
import librec.data.DataDAO;
import librec.data.MatrixEntry;
import java.util.*;


public class LARS extends Recommender {

    private String rec;
    private int userID;
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
        /* Escolha do recommender
        this.rec 	        = LARS.algoOptions.getString("-cf");
        */
        this.userID 	    = rateDao.getUserId(LARS.algoOptions.getString("-user").toLowerCase());
        this.userLat 	    = LARS.algoOptions.getDouble("-lat");
        this.userLong 	    = LARS.algoOptions.getDouble("-long");
        this.k              = LARS.algoOptions.getInt("-k");


        // Hash ID e penalidade
        this.itemTravelPenalty = new HashMap<Integer,Double>();
        this.R = new LinkedHashMap<String,Double>();

        recUsed = new ItemKNN(trainMatrix, testMatrix, fold);

        // Construindo modelo
        recUsed.execute();

        // Construindo o itemTravelPenalty
        buildTravelPenaltyModel(trainMatrix, this.userLat, this.userLong);

        for(MatrixEntry me : trainMatrix) {
            double recScore, p, travelPenalty;
            int itemID = rateDao.getItemIdFromUI(me.row());

            p = recUsed.predict(this.userID, itemID);
            travelPenalty = normalizeDistance(itemTravelPenalty.get(itemID), recUsed.maxRate, recUsed.minRate);

            recScore = p - travelPenalty;

            R.put(String.valueOf(itemID),recScore);
        }

        R = sortByValue(R);
        Logs.debug(R.toString());

        // Top K recomendações
        Logs.debug("USER " + rateDao.getUserId(this.userID));

        int i = 1;
        for(Map.Entry<String,Double> entry:R.entrySet()){
            String id = entry.getKey();
            Double score = entry.getValue();
            Logs.debug("ITEM " + i + " ID: " + rateDao.getItemId(Integer.parseInt(id)) + " SCORE: " + score);
            if(i == k) {
                break;
            } else {
                i++;
            }
        }

        writeMatrix(trainMatrix);
        writeMatrix(testMatrix);


    }

    @Override
    public double predict(int u, int j, int c) throws Exception {

        double recScore, p, travelPenalty;

        p = recUsed.predict(u, j);
        travelPenalty = normalizeDistance(itemTravelPenalty.get(j), recUsed.maxRate, recUsed.minRate);
        recScore = p - travelPenalty;

        if (recScore > maxRate)
            recScore = maxRate;

        if (recScore < minRate)
            recScore = minRate;

        return recScore;
    }

    public static Map<String, Double> sortByValue(Map<String, Double> unsorted_map){

        Map<String, Double> sorted_map = new LinkedHashMap<String, Double>();

        try{
            List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsorted_map.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> o1,
                                   Map.Entry<String, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            for (Map.Entry<String, Double> entry : list) {
                sorted_map.put(entry.getKey(), entry.getValue());
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }

        return sorted_map;

    }

    private Double normalizeDistance(Double distance, Double max, Double min){
        double value = (distance - minDistanceGlobal) / (maxDistanceGlobal - minDistanceGlobal);
        return value * (max - min) + min;
    }

    private void buildTravelPenaltyModel(SparseMatrix matrix, double userLat, double userLong){
        int itemID;
        double itemLat, itemLong, euclideanDistance;
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
        System.out.println("\n\n WRITEMATRIX \n\n");
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

