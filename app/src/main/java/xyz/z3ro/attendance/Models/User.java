package xyz.z3ro.attendance.Models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    public String first_name;
    public String last_name;
    public String roll;
    public String phone_number;
    public String serial;
    public String zimei;
    public int No_of_days_present;
    public User(){
    }
    public User (String first_name,String last_name,String roll,String phone_number,String serial,String zimei, int No_of_days_present){
        this.first_name = first_name;
        this.last_name = last_name;
        this.roll = roll;
        this.phone_number = phone_number;
        this.serial = serial;
        this.zimei = zimei;
        this.No_of_days_present = No_of_days_present;
    }
}
