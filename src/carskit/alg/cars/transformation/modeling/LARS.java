package carskit.alg.cars.transformation.modeling;

import carskit.alg.baseline.cf.ItemKNN;
import carskit.alg.baseline.cf.ItemKNNUnary;
import carskit.alg.baseline.cf.SVDPlusPlus;
import carskit.alg.baseline.cf.UserKNN;
import carskit.alg.baseline.ranking.BPR;
import carskit.alg.cars.transformation.prefiltering.CombinedReduction;
import carskit.data.structure.SparseMatrix;
import carskit.generic.Recommender;
import happy.coding.io.LineConfiger;
import librec.data.MatrixEntry;

import java.io.PrintWriter;

public class LARS extends Recommender {

    private String rec;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec 	= CombinedReduction.algoOptions.getString("-traditional");

        //writeMatrix(trainMatrix);

        ItemKNN itemKNNModel = new ItemKNN(trainMatrix, testMatrix, fold);
        try {
            itemKNNModel.initModel();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Recommender getRecommender(SparseMatrix train, SparseMatrix test, int fold){

        Recommender recsys = null;
        switch (this.rec) {
            case "itemknn":
                recsys = new ItemKNN(train,test, fold);
                break;
            case "itemknnunary":
                recsys = new ItemKNNUnary(train, test,fold);
                break;
            case "userknn":
                recsys = new UserKNN(train, test, fold);
                break;
            case "bpr":
                recsys = new BPR(train, test, fold);
                break;
            case "svd++":
                recsys = new SVDPlusPlus(train,test, fold);
                break;

        }
//		recsys.setItemFrequency(true);
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
