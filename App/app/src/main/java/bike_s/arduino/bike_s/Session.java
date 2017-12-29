package bike_s.arduino.bike_s;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Context ctx;

    public Session(Context ctx){
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences("Bike_S_Application", Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setLoggedIn(boolean logggedin, String userName){
        editor.putBoolean("loggedInmode",logggedin);
        editor.putString("userName",userName);
        editor.commit();
    }

    public boolean loggedIn(){
        return prefs.getBoolean("loggedInmode", false);
    }

    public String getUserName(){
        return prefs.getString("userName",null);
    }
}