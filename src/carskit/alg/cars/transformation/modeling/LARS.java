package carskit.alg.cars.transformation.modeling;

import carskit.alg.baseline.cf.ItemKNN;
import carskit.alg.baseline.cf.ItemKNNUnary;
import carskit.alg.baseline.cf.UserKNN;
import carskit.data.structure.SparseMatrix;
import carskit.generic.Recommender;
import happy.coding.io.LineConfiger;
import librec.data.MatrixEntry;

public class LARS extends Recommender {

    private String rec;
    private String userIDTarget;
    private String userLat;
    private String userLong;

    public LARS(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
        super(trainMatrix, testMatrix, fold);

        LARS.algoOptions	= new LineConfiger(cf.getString("recommender"));
        this.rec 	        = LARS.algoOptions.getString("-cf");
        this.userIDTarget 	= LARS.algoOptions.getString("-user");
        this.userLat 	    = LARS.algoOptions.getString("-lat");
        this.userLong 	    = LARS.algoOptions.getString("-long");


        // writeMatrix(trainMatrix);

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

                predictRating = recUsed.predict(userID,itemID);
                System.out.println("userID: " + userID + "\t\titemID: " + itemID +
                        "\t\tcontextID: " + contextID + "\t\tRating: " +predictRating);
            }

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
