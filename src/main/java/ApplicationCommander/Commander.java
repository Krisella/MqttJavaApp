package ApplicationCommander;

import java.io.IOException;

public class Commander {
    public Commander() {}

    public static void main(String[] args){
        try {
            System.out.println(args.length);
            classifyData.createEntropyFiles(args[0], args[1], args[2]);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
