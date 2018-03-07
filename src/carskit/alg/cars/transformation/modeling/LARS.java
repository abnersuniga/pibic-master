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

    private double genericLat, genericLong;
    private String rec;
    private double maxDistanceGlobal, minDistanceGlobal;
    private Recommender recUsed;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec = LARS.algoOptions.getString("-cf");
        this.genericLat       = LARS.algoOptions.getDouble("-lat");
        this.genericLong      = LARS.algoOptions.getDouble("-long");

        recUsed = getRecommender(trainMatrix, testMatrix, fold);
        recUsed.execute();

    }

    @Override
    public double predict(int u, int j, int c) throws Exception {

        double recScore, p, travelPenalty, latContext, longContext, euclideanDistance;
        String[] contexts = rateDao.getContextId(c).split(",");

        latContext = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[0])).split(":")[1]);
        longContext = Double.parseDouble(rateDao.getContextConditionId(Integer.parseInt(contexts[1])).split(":")[1]);

        euclideanDistance = Math.sqrt((Math.pow(genericLat - latContext, 2.0) + Math.pow(genericLong - longContext,2.0)));

        travelPenalty = euclideanDistance;

        p = recUsed.recommend(u, j, c);
        recScore = p - travelPenalty;


        if (recScore > maxRate)
            recScore = maxRate;

        if (recScore < minRate)
            recScore = minRate;

        return recScore;
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

}

