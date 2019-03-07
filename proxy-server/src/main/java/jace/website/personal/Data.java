package jace.website.personal;

public class Data {
    private static Data instance = null;
    private int id = 0;

    private Data(){

    }

    public static Data getInstance(){
        if (instance == null) {
            instance = new Data();
        }
        return instance;
    }

    public int getNewId(){
        id += 1;
        return id - 1;
    }
}
