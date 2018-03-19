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
    private Map<Integer,Double[]> itemsLocalization;
    private Recommender recUsed;
    private Double maxUserToItemDistance, minUserToItemDistance;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec = LARS.algoOptions.getString("-cf");

        recUsed = getRecommender(trainMatrix, testMatrix, fold);
        recUsed.execute();

        itemsLocalization = getItemsLocalization(trainMatrix);
        getMinAndMaxDistances(trainMatrix);


        System.out.println("min: " + this.minUserToItemDistance);
        System.out.println("max: " + this.maxUserToItemDistance);
    }

    @Override
    public double predict(int u, int j, int c) throws Exception {

        double recScore, p, travelPenalty, contextLat, contextLong, itemLat, itemLong, euclideanDistance;
        String[] contexts = rateDao.getContextId(c).split(",");

        contextLat = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
        contextLong = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

        itemLat = itemsLocalization.get(j)[0];
        itemLong = itemsLocalization.get(j)[1];

        euclideanDistance = Math.sqrt((Math.pow(itemLat - contextLat, 2.0) + Math.pow(itemLong - contextLong,2.0)));

        travelPenalty = normalizeDistance(euclideanDistance, recUsed.maxRate, recUsed.minRate);

        p = recUsed.recommend(u, j, c);
        recScore = p - travelPenalty;

        /*
        if (recScore > maxRate)
            recScore = maxRate;

        if (recScore < minRate)
            recScore = minRate;
        */

        //System.out.println("p = " + p + "\tp - travelP = " + recScore);

        return recScore;
    }

    private Double normalizeDistance(Double distance, Double max, Double min){
        double value = (distance - minUserToItemDistance) / (maxUserToItemDistance - minUserToItemDistance);
        return value * (max - min) + min;
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

    private Map<Integer,Double[]> getItemsLocalization(SparseMatrix matrix) {
        int itemId;
        double itemLat, itemLong;
        Double[] loc;
        Map<Integer,Double[]> itemDistances = new HashMap<Integer,Double[]>();

        for(MatrixEntry me : matrix){

            itemId = rateDao.getItemIdFromUI(me.row());
            String[] contexts = rateDao.getContextId(me.column()).split(",");

            itemLat = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
            itemLong = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

            loc = new Double[2];
            loc[0] = itemLat;
            loc[1] = itemLong;

            itemDistances.put(itemId, loc);
        }

        return itemDistances;
    }

    private void getMinAndMaxDistances(SparseMatrix matrix) {
        int itemIdI,itemIdJ;
        double itemLatI, itemLongI, itemLatJ, itemLongJ, euclideanDistance;
        boolean first = true;
        String[] contexts;

        for(MatrixEntry mei : matrix){

            itemIdI = rateDao.getItemIdFromUI(mei.row());
            contexts = rateDao.getContextId(mei.column()).split(",");

            itemLatI = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
            itemLongI = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

            for(MatrixEntry mej : matrix){

                itemIdJ = rateDao.getItemIdFromUI(mej.row());
                contexts = rateDao.getContextId(mej.column()).split(",");

                itemLatJ = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
                itemLongJ = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

                euclideanDistance = Math.sqrt((Math.pow(itemLatI - itemLatJ, 2.0) + Math.pow(itemLongI - itemLongJ,2.0)));

                if(first || euclideanDistance > maxUserToItemDistance){
                    maxUserToItemDistance = euclideanDistance;
                }

                if(first || euclideanDistance < minUserToItemDistance){
                    minUserToItemDistance = euclideanDistance;
                }

                first = false;
            }
        }
    }

}

