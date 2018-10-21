package xyz.z3ro.attendance.Models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Serial {
    public int date;
    public int isPresent;
    public Serial(){
    }
    public Serial(int date,int isPresent){
        this.date = date;
        this.isPresent = isPresent;
    }

}