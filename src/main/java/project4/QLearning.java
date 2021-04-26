package project4;

import okhttp3.*;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.Thread.sleep;

public class QLearning {

    final int WORLD_ID = 1;

    double learningRate = 0.8;
    double discountRate = 0.8;

    double explorationRate = 0.8;

    // 0 - left 1 - right 2 - up 3 - down
    double[][][] qTable = new double[40][40][4];

    public void qLearning(int[] state) throws IOException {
        // state has x and y coordinates on the grid
        boolean done = false;

        // make it while loop it should continue till traversing ends
        while (!done) {
            double expirationRateThreshold = Math.random();
            int action;
            if (expirationRateThreshold > explorationRate) {
                // chose action based on the max qvalue of the state
                double max = 0;
                int maxIndex = 0;
                for (int k = 0; k < 4; k++) {
                    if (qTable[state[0]][state[1]][k] > max) {
                        max = qTable[state[0]][state[1]][k];
                        maxIndex = k;
                    }
                }
                action = maxIndex;
                System.out.println("Making best move");
            } else {
                // else explore other actions
                // chose random number between 0 and 3 inclusive

                ArrayList<Integer> rnd = new ArrayList<>();
                // if the number is between  world boundaries than add it to the selectable action
                if(state[1] - 1 >= 0){
                    // can we move left?
                    rnd.add(0);
                }
                if(state[1] + 1 < 40){
                    // can we move right?
                    rnd.add(1);
                }
                if(state[0] - 1 >= 0){
                    // can we move up?
                    rnd.add(2);
                }
                if(state[0] + 1 < 40){
                    // can we move down?
                    rnd.add(3);
                }
                action = rnd.get(new Random().nextInt(rnd.size()));
                System.out.println("Making random move");
            }
            System.out.println("New Action: " + action);

            // determine move from current state and action
            int[] move = state;
            switch (action) {
                case 0: // move left
                    move = new int[]{state[0], state[1] - 1};
                    break;
                case 1: // move right
                    move = new int[]{state[0], state[1] + 1};
                    break;
                case 2: // move up
                    move = new int[]{state[0] - 1, state[1]};
                    break;
                case 3: // move down
                    move = new int[]{state[0] + 1, state[1]};
                    break;
            }
            System.out.println("Current State: " + state[0] + " , " + state[1]);
            System.out.println("Move: " + move[0] + " , " + move[1]);

            // Make a move
            // API will return new state and reward
            // TODO MAKE API CALL TO MAKE MOVE - DONE
            int[] new_state = new int[2];

            OkHttpClient moveClient = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("text/plain");
            RequestBody moveBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("move", move[0] + "," + move[1])
                    .addFormDataPart("type", "move")
                    .addFormDataPart("teamId", "1258")
                    .addFormDataPart("worldId", String.valueOf(WORLD_ID))
                    .build();
            Request moveRequest = new Request.Builder()
                    .url("https://www.notexponential.com/aip2pgaming/api/rl/gw.php")
                    .method("POST", moveBody)
                    .addHeader("x-api-key", "998bc59f37ed76e14f08")
                    .addHeader("userid", "1060")
                    .build();

            Response moveResponse = moveClient.newCall(moveRequest).execute();
            JSONObject moveResponseJSON = new JSONObject(moveResponse.body().string());
//                if(responseJSON.getInt("world") == -1) done = true;
            double reward = moveResponseJSON.getDouble("reward");
            new_state[0] = moveResponseJSON.getJSONObject("newState").getInt("x");
            new_state[1] = moveResponseJSON.getJSONObject("newState").getInt("y");

            System.out.println("Reward: " + reward);
            System.out.println("New State: " + new_state[0] + " , " + new_state[1]);

            // Update qTable
            // first find the max qValue of the new state
            double new_state_max = 0;
            for (int k = 0; k < 4; k++) {
                if (qTable[new_state[0]][new_state[1]][k] > new_state_max) {
                    new_state_max = qTable[new_state[0]][new_state[1]][k];
                }
            }
            // Update qTable of the current state
            qTable[state[0]][state[1]][action] = qTable[state[0]][state[1]][action] * (1 - learningRate) +
                    learningRate * (reward + discountRate * new_state_max);

            state = new_state;

            // check if the game is over
            // TODO check with api to see if it is over - DONE
            // done = true; if it is over
            // if returned world is -1 then it is over
            OkHttpClient locationClient = new OkHttpClient().newBuilder()
                    .build();
            Request locationRequest = new Request.Builder()
                    .url("https://www.notexponential.com/aip2pgaming/api/rl/gw.php?type=location&teamId=1258")
                    .method("GET", null)
                    .addHeader("x-api-key", "998bc59f37ed76e14f08")
                    .addHeader("userid", "1060")
                    .build();
            Response locationResponse = locationClient.newCall(locationRequest).execute();
            JSONObject responseJSON = new JSONObject(locationResponse.body().string());
            int activeWorld = responseJSON.getInt("world");
            if (activeWorld == -1) done = true;
//                done = true;
            System.out.println("Active World: " + activeWorld);
            writeQTable();
            sleepSec(15);
        }

        // decay the exploration rate
//            exploration_rate -= 0.2;


    }

    void writeQTable() throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream("world" + WORLD_ID + ".ser")
        );
        out.writeObject(qTable);
        out.flush();
        out.close();
        System.out.println("Q-Table is written to world" + WORLD_ID + ".ser");
    }

    double[][][] readQTable() throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("world" + WORLD_ID + ".ser"));
        double[][][] array = (double[][][]) in.readObject();
        in.close();
        return array;
    }

    public void sleepSec(int second) {
        try {
            System.out.println("start sleeping");
            sleep(second * 1000);
            System.out.println("stop sleeping");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        QLearning qmain = new QLearning();

        // get location/state
        System.out.println("Getting Location to Start");
        int[] currentState = new int[2];
        OkHttpClient locationClient = new OkHttpClient().newBuilder()
                .build();
        Request locationRequest = new Request.Builder()
                .url("https://www.notexponential.com/aip2pgaming/api/rl/gw.php?type=location&teamId=1258")
                .method("GET", null)
                .addHeader("x-api-key", "998bc59f37ed76e14f08")
                .addHeader("userid", "1060")
                .build();
        try {
            Response locationResponse = locationClient.newCall(locationRequest).execute();
            JSONObject responseJSON = new JSONObject(locationResponse.body().string());
            String[] statesString = responseJSON.getString("state").split(":");
            currentState[0] = Integer.parseInt(statesString[0]);
            currentState[1] = Integer.parseInt(statesString[1]);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Start Location: " + currentState[0] + " , " + currentState[1]);

        // read q table
        try {
            qmain.qTable = qmain.readQTable();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not read qTable");
            e.printStackTrace();
//            return;
        }

        // run q learning. if exception happens write to a file
        try {
            qmain.qLearning(currentState);
        } catch (IOException e) {
            qmain.writeQTable();
            e.printStackTrace();
            return;
        }
        qmain.writeQTable();
    }
}
