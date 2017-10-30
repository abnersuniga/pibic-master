package carskit.alg.cars.transformation.modeling;

import carskit.alg.baseline.cf.ItemKNN;
import carskit.alg.baseline.cf.ItemKNNUnary;
import carskit.alg.baseline.cf.SVDPlusPlus;
import carskit.alg.baseline.cf.UserKNN;
import carskit.alg.baseline.ranking.BPR;
import carskit.data.structure.SparseMatrix;
import carskit.generic.Recommender;
import happy.coding.io.LineConfiger;
import librec.data.MatrixEntry;
import java.io.PrintWriter;

public class LARS extends Recommender {

    private String rec;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec 	= LARS.algoOptions.getString("-CF");

        // writeMatrix(trainMatrix);

        Recommender recUsed = getRecommender(trainMatrix, testMatrix, fold);

        // Model Building
        recUsed.execute();

        for(MatrixEntry me : testMatrix){
            double predictRating;

            StringBuilder sb  = new StringBuilder();
            String userID = rateDao.getUserId(rateDao.getUserIdFromUI(me.row()));
            String itemID = rateDao.getItemId(rateDao.getItemIdFromUI(me.row())).substring(2);
            String[] contextIDs = rateDao.getContextId(me.column()).split(",");

            for(String contextID : contextIDs){

                predictRating = recUsed.recommend(Integer.parseInt(userID),Integer.parseInt(itemID),Integer.parseInt(contextID));
                System.out.println(predictRating);
            }

        }


    }



    private Recommender getRecommender(SparseMatrix train, SparseMatrix test, int fold){

        Recommender recsys = null;
        switch (this.rec) {
            case "itemknn":
                recsys = new ItemKNN(train, test, fold);
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
