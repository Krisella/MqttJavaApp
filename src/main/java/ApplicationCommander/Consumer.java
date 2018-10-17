package ApplicationCommander;

import org.eclipse.paho.client.mqttv3.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class ConsumerCallback implements MqttCallback{
    @Override
    public void connectionLost(Throwable cause){

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception{
        Consumer.sleepAmount.set(Integer.parseInt(message.toString()) * 1000);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}

public class Consumer extends Thread {
    private static int              MAXQUEUE;
    private ArrayList<DString>      commandQueue = new ArrayList<DString>();
    protected static AtomicInteger  sleepAmount = new AtomicInteger(1000);
    private boolean                 receiving = true;
    private static MqttClient       client;
    private static String           clientId = "pahomqttpublisher";
    private static String           serverUri = "tcp://localhost:1883";
    private int                     correct = 0;
    private int                     count = 0;

    Consumer(int max){
        MAXQUEUE = max;
        clientId = clientId + System.currentTimeMillis();
    }

    @Override
    public void run(){
        try{
            client = new MqttClient(serverUri, clientId);
            client.setCallback(new ConsumerCallback());
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            System.out.println("Connecting to broker: " + serverUri);
            client.connect(mqttConnectOptions);
            System.out.println("Connected");
            client.subscribe("mqttapp/part2/freq", 0);
        }catch (MqttException e){
            e.printStackTrace();
        }
        try{
            while(true){
                sendCommand();
                Thread.sleep(sleepAmount.get());
            }
        }catch (InterruptedException e){
            System.out.println("Thread execution completed. Exiting...");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        try{
            client.disconnect();
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    private synchronized void sendCommand() throws InterruptedException, MqttException {
        if(receiving || (commandQueue.size() != 0)) {
            while (commandQueue.size() == 0) {
                wait();
            }
            if(commandQueue.size() != 0) {
                DString cur = commandQueue.remove(0);
                notify();
                MqttMessage message = new MqttMessage();
                message.setPayload(cur.getResult().getBytes());
                message.setQos(1);
                client.publish("mqttapp/part2", message);
                if (cur.print()) {
                    correct++;
                }
                count++;
            }
        }else{
            System.out.println("Number of successful predictions: " + correct);
            System.out.println("Files: " + count);
            System.out.println("Success rate: " + ((float) correct /(float) count) * 100 + "%");
            this.interrupt();
        }
    }

    public synchronized void putCommand(String command, String result) throws InterruptedException{
        while(MAXQUEUE > 0 && commandQueue.size() == MAXQUEUE){
            wait();
        }

        commandQueue.add(new DString(command, result));
        notify();
    }

    public synchronized void end(){
        receiving = false;
        notify();
    }
}
