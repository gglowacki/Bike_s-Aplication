package bike_s.arduino.bike_s;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Context ctx;
    private String code2;

    public Session(Context ctx){
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences("Bike_S_Application", Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setLoggedIn(boolean logggedin, String userName){
        editor.putBoolean("loggedInmode",logggedin);
        editor.putString("userName",userName);
        editor.putInt("lockCode", 0);
        editor.commit();
    }

    public boolean loggedIn(){
        return prefs.getBoolean("loggedInmode", false);
    }

    public String getUserName(){
        return prefs.getString("userName",null);
    }

    public void saveLockCombination(int code2){
        editor.putInt("lockCode", code2);
        editor.commit();
    }

    public int GetLockCombination(){ return prefs.getInt("lockCode", 0);}
}