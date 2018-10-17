package ApplicationCommander;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static ApplicationCommander.Type.EyesClosed;
import static ApplicationCommander.Type.EyesOpened;

enum Type {EyesOpened, EyesClosed}

public class classifyData {

    static String csvSplitBy = ",";
    static int k = 11;

    public static void createEntropyFiles(String dataDir, String trainingFilePath, String resultDir) throws IOException, InterruptedException {

        String destinationFilePath = resultDir;
        final File folder = new File(dataDir);
        FileWriter writer = new FileWriter(destinationFilePath);
        Consumer consumer = new Consumer(0);

//        load training set
        ArrayList<ArrayList<Double>> trainingVectors = new ArrayList<>();
        ArrayList<String> trainingType = new ArrayList<>();
        try (BufferedReader trainingBr = new BufferedReader(new FileReader(trainingFilePath))) {
            String trainingSetLine = "";
            while ((trainingSetLine = trainingBr.readLine()) != null) {
                String[] splitLine = trainingSetLine.split(csvSplitBy);
                if(!splitLine[0].equals("LabelClassName")){
                    ArrayList<Double> tempVector = new ArrayList<>();
                    trainingType.add(splitLine[0]);
                    for(int i=1;i<15;i++)
                        tempVector.add(Double.parseDouble(splitLine[i]));
                    trainingVectors.add(tempVector);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        consumer.start();

        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                String fileName = fileEntry.getName();
                Type type;

                if(fileName.contains("EyesOpened"))
                    type = EyesOpened;
                else if(fileName.contains("EyesClosed"))
                    type = EyesClosed;
                String[] splitName = fileName.split("\\.");
                if(splitName[splitName.length - 1].equals("csv")){

                    String line = "";
                    ArrayList<ArrayList<Double>> dataArrayList = new ArrayList<>();
                    for(int i=0; i<14; i++){
                        ArrayList<Double> tempArray = new ArrayList<>();
                        dataArrayList.add(tempArray);
                    }
                    try (BufferedReader br = new BufferedReader(new FileReader(dataDir + fileName))) {
                        boolean markerFlag = false;
                        boolean allLinesSkipped = true;

                        while ((line = br.readLine()) != null) {

                            // use comma as separator
                            String[] splitLine = line.split(csvSplitBy);

                            if(!splitLine[0].equals("AF3")){

                                if(markerFlag == false && Double.parseDouble(splitLine[28]) > 0)
                                    markerFlag = true;
                                else if (markerFlag == true && Double.parseDouble(splitLine[28]) > 0)
                                    markerFlag = false;
                                else if (markerFlag == false)
                                    continue;
                                boolean skipLine = false;
                                for(int i=14; i<28; i++){
                                    if(Double.parseDouble(splitLine[i]) < 4.0){
                                        skipLine = true;
                                        break;
                                    }
                                }
                                if(!skipLine){
                                    allLinesSkipped = false;
                                    for(int i=0; i<14; i++)
                                        dataArrayList.get(i).add(Double.parseDouble(splitLine[i]));
                                }
                            }
                        }

                        if(!allLinesSkipped) {

                            StringBuilder sb = new StringBuilder();
                            ArrayList<Double> featureVector = new ArrayList<>();
                            for (int i = 0; i < 14; i++) {
                                double[] doubleArr = new double[dataArrayList.get(i).size()];
                                for (int j = 0; j < doubleArr.length; j++)
                                    doubleArr[j] = dataArrayList.get(i).get(j).doubleValue();
                                double entropy = Entropy.calculateEntropy(doubleArr);
                                if (i == 0)
                                    sb.append(fileName + ",");
                                sb.append(entropy);
                                featureVector.add(entropy);
                                if (i != 13)
                                    sb.append(',');
                                else
                                    sb.append('\n');
                            }
                            writer.write(sb.toString());
//kNN
                            class EuclideanPair {
                                public Double euclideanDistance;
                                public String resultType;
                            }
                            class EuclideanPairComparator implements Comparator<EuclideanPair>{
                                public int compare(EuclideanPair pair1, EuclideanPair pair2){
                                    return pair1.euclideanDistance.compareTo(pair2.euclideanDistance);
                                }
                            }

                            ArrayList<EuclideanPair> euclideanDistanceVector = new ArrayList<>();
                            for(int i=0;i<trainingVectors.size();i++){
                                Double euclideanDistance = 0.0;
                                for(int j=0; j<14; j++)
                                    euclideanDistance += Math.pow(trainingVectors.get(i).get(j) - featureVector.get(j), 2);
                                EuclideanPair pair = new EuclideanPair();
                                pair.euclideanDistance = Math.sqrt(euclideanDistance);
                                pair.resultType = trainingType.get(i);
                                euclideanDistanceVector.add(pair);
                            }
                            Collections.sort(euclideanDistanceVector, new EuclideanPairComparator());

                            int[] yCounter = {0,0};
                            Double[] weight = {0.0,0.0};
                            for(int i=0;i<k;i++){
                                String label = euclideanDistanceVector.get(i).resultType;
                                Double distance = euclideanDistanceVector.get(i).euclideanDistance;
                                if(label.contains("EyesOpened")) {
                                    yCounter[0]++;
                                    weight[0] += (1/distance);
                                }
                                else if(label.contains("EyesClosed")) {
                                    yCounter[1]++;
                                    weight[1] += (1/distance);
                                }
                            }
                            if(weight[0] * yCounter[0] > weight[1] * yCounter[1]) {
                                consumer.putCommand("Execute Eyes Opened", fileName);
                            }
                            else if(weight[0] * yCounter[0] < weight[1] * yCounter[1]) {
                                consumer.putCommand("Execute Eyes Closed", fileName);
                            }
                            else
                                System.out.println("Same values!!!!");

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        consumer.end();
        writer.flush();
        writer.close();
    }
}