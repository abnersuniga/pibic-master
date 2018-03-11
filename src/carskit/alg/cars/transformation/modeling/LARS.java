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
    private Map<Integer,Double[]> itemDistances;
    private Recommender recUsed;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec = LARS.algoOptions.getString("-cf");

        recUsed = getRecommender(trainMatrix, testMatrix, fold);
        recUsed.execute();

        itemDistances = getItemDistances(trainMatrix);
    }

    @Override
    public double predict(int u, int j, int c) throws Exception {

        double recScore, p, travelPenalty, contextLat, contextLong, itemLat, itemLong, euclideanDistance;
        String[] contexts = rateDao.getContextId(c).split(",");

        contextLat = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
        contextLong = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

        itemLat = itemDistances.get(j)[0];
        itemLong = itemDistances.get(j)[1];

        euclideanDistance = Math.sqrt((Math.pow(itemLat - contextLat, 2.0) + Math.pow(itemLong - contextLong,2.0)));

        travelPenalty = normalizeDistance(euclideanDistance, recUsed.maxRate, recUsed.minRate);

        p = recUsed.recommend(u, j, c);
        recScore = p - travelPenalty;

        System.out.println(recScore);

        if (recScore > maxRate)
            recScore = maxRate;

        if (recScore < minRate)
            recScore = minRate;

        return recScore;
    }

    private Double normalizeDistance(Double distance, Double max, Double min){
        double value = (distance - 0) / (125.42205755523861 - 0);
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

    private Map<Integer,Double[]> getItemDistances(SparseMatrix matrix) {
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

}

