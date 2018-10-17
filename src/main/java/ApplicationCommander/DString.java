package ApplicationCommander;

public class DString{
    private String str1;
    private String str2;

    DString(String str1, String str2){
        this.str1 = str1;
        this.str2 = str2;
    }

    public String getResult(){
        return str1;
    }

    public String getExpResult(){
        return str2;
    }

    public Boolean print(){
        System.out.print("Result: " + str1 + ", Expected Result: ");
        if(str1.contains("Opened") && str2.contains("EyesOpened")){
            System.out.println("Eyes Opened (Success!)");
            return true;
        }else if(str1.contains("Closed") && str2.contains("EyesClosed")){
            System.out.println("Eyes Closed (Success!)");
            return true;
        }else if(str1.contains("Opened")){
            System.out.println("Eyes Opened (Failure!)");
            return false;
        }else{
            System.out.println("Eyes Closed (Failure!)");
            return false;
        }
    }
}

