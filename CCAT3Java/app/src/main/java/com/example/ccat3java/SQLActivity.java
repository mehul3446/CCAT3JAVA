package com.example.ccat3java;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class SQLActivity extends AppCompatActivity {

    private DatabaseInterface dbInterface;
    /*
     * The DatabaseInterface class defines all the methods which interact with the local database,
     * as well as the database located on the microcontroller. Each method in this main activity is
     * meant to demonstrate how to use the interface methods with event handlers. There are several
     * important things to know about interacting with the database. First, the variables that each
     * interface method require have two general forms:
     * - String feedingTime, newFeedingTime etc: this needs to be a properly formatted date time
     *       string. A properly formatted string looks like this: "yyyy/MM/dd HH:mm:ss" or
     *       "2019/10/26 12:30:00".
     *       To get the current date and time, properly formatted use these commands:
     *       DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
     *       LocalDateTime now = LocalDateTime.now();
     *       dtf.format(now);
     *
     *       docs here: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
     *
     * - String dayOfWeek: just needs to be a simple string: Sun, Mon, Tue, Wed, Thur, Fri, Sat. This
     *       information can also be gotten from the date time formatter
     *
     * Each interface method returns either a boolean or the results of a database query. A true value
     * or an ArrayList with values in it indicate that the method was successful in its entirety.
     * An empty ArrayList or a false value indicates that the operation failed for some reason. Reasons
     * that the operations could fail are that the phone is unable to make a connection to the
     * microcontroller, the operation or connection was interrupted, or it could just mean that the
     * database doesn't have any values to return
     *
     * When using these methods, the user should be alerted about the result. If it failed, display
     * something like "Sync Failed". Some methods will be possible if the app can't make a connection
     * to the microcontroller. Things like syncing and updating records are not allowed if there is
     * no connection. The only operations allowed are to read from the local database and to enter
     * new feeding times, which will need to be manually synced later.
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sql);
        this.dbInterface = new DatabaseInterface(getApplicationContext());
        this.dbInterface.deleteFeedingTime("2000/10/10 10:10:10");
        this.dbInterface.deleteFeedingTime("2000/11/11 11:11:11");
        this.dbInterface.deleteFeedingTime("2000/12/11 11:11:11");
        this.dbInterface.insertFeedingTime("2000/10/10 10:10:10", "Sat");
        this.dbInterface.insertFeedingTime("2000/11/11 11:11:11", "Fri");
        this.dbInterface.insertFeedingTime("2000/12/11 11:11:11", "Fri");
    }

    public void clickInsertFeedingTime(View view) {
        /*
         * This method is meant to demonstrate and test inserting a new feeding time into the database
         */
        Log.d("toplevel", "clickInsertFeedingTime");
        Log.d("insertFeedingTime", "clickInsertFeedingTime");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        DateTimeFormatter day = DateTimeFormatter.ofPattern("E"); //this gets you the current day
        LocalDateTime now = LocalDateTime.now();

        String timeAndDate = dtf.format(now);
        String dayOfWeek = day.format(now);

        Log.d("insertFeedingTime", "timeAndDate: " + timeAndDate);
        Log.d("insertFeedingTime", "dayOfWeek: " + dayOfWeek);

        boolean success = this.dbInterface.insertFeedingTime(timeAndDate, dayOfWeek);

        Log.d("insertFeedingTime", "Insert Success: " + success);


    }

    public void clickUpdateFeedingTime(View view) {
        /*
         * This method is meant to update a record in the current database. The update interface
         * method is a convenience method for deleting an old record and inserting a new one.
         * This is meant to be used only on records which haven't occurred yet. Updates only work
         * if the app can connect to the database. You need to make sure that the value exists before
         * you use this method
         */
        Log.d("toplevel", "clickUpdateFeedingTime");
        Log.d("updateFeedingTime", "clickUpdateFeedingTime");

        //Ignore this, I'm just using it to make sure that there is a future date in the database
        this.dbInterface.insertFeedingTime("2020/10/20 12:00:00", "Sat");

        //This is the method you should use to get feeding times which haven't occurred yet
        ArrayList<String[]> currentTimes = this.dbInterface.loadCurrentFeedingTimes();
        for (String[] item: currentTimes) {
            String feedingTime = item[0];
            String dayOfWeek = item[1];
            String isSynchronized = item[2];
            Log.d("updateFeedingTime", "Feeding Time: " + item);
        }

        //
        boolean success = this.dbInterface.updateFeedingTime("2020/10/20 12:00:00",
                "2020/10/30 13:00:00", "Sun");
        Log.d("updateFeedingTime", "Update Success: " + success);

    }

    public void clickDeleteFeedingTime(View view) {
        /*
         * This method deletes a record using its timeAndDate, you need to make sure the value exists
         * before using this method. It is meant to be used on the current times
         */
        Log.d("toplevel", "clickDeleteFeedingTime");
        Log.d("deleteFeedingTime", "clickDeleteFeedingTime");

        //Normally you would use this to pick out a currently existing time
//        ArrayList<String[]> currentTimes = this.dbInterface.loadCurrentFeedingTimes();
//        for (String[] item: currentTimes) {
//            String feedingTime = item[0];
//            String dayOfWeek = item[1];
//            String isSynchronized = item[2];
//      }

        //I'm only doing this for testing
        boolean success = this.dbInterface.deleteFeedingTime("2020/10/20 12:00:00");
        Log.d("deleteFeedingTime", "Delete Feeding Time Success: " + success);


    }

    public void clickSyncFeedingTimes(View view) {
        /*
         * Feeding times which are entered when there is no connection are stored with a database
         * field marking them unsynced. The user should be made aware of these entries. When the
         * user presses the sync button, a connection is attempted and unsynced times are inserted
         * into the microcontroller database.
         */
        Log.d("toplevel", "clickSyncFeedingTimes");
        Log.d("syncFeedingTimes", "clickSyncFeedingTimes");
        boolean success = this.dbInterface.syncFeedingTimes();
        Log.d("syncFeedingTimes", "syncFeedingTimes success: " + success);
    }

    public void clickFetchWeightChanges(View view) {
        /*
         * This method is meant for connecting to the microcontroller and updating any weight changes
         * on feeding times which are in the past.
         */
        Log.d("toplevel", "clickFetchWeightChanges");
        Log.d("fetchWeightChanges", "clickFetchWeightChanges");

        boolean success = this.dbInterface.fetchWeightChanges();
        Log.d("fetchWeightChanges", "Fetch Weight Change Success: " + success);
    }

    public void clickFetchBreakBeamRecords(View view) {
        /*
         * This method is for connecting to the microcontroller and syncing up any new break beam
         * record entries
         */
        Log.d("toplevel", "clickFetchBreakBeamRecords");
        Log.d("fetchBreakBeamRecords", "clickFetchBreakBeamRecords");


        boolean success = this.dbInterface.fetchBreakBeamRecords();
        Log.d("fetchBreakBeamRecords", "Fetch Break Beam Success: " + success);
    }

    public void clickLoadCurrentFeedingTimes(View view) {
        /*
         * This method is to test to see if we can view the future feeding times in the db.
         * It is for filling out the schedule that the user can see of planned feeding times
         */

//        this.dbInterface.insertFeedingTime("2020/11/10 12:00:00", "Sun");
//        this.dbInterface.insertFeedingTime("2020/12/10 12:00:00", "Sun");

        Log.d("toplevel", "clickLoadCurrentFeedingTimes");
        Log.d("loadCurrentFeedingTime", "clickLoadCurrentFeedingTimes");
        ArrayList<String[]> currentTimes = this.dbInterface.loadCurrentFeedingTimes();
        for (String[] item: currentTimes) {
            for (String indiv: item) {
                Log.d("loadCurrentFeedingTime", "Feeding Time: " + indiv);
            }
        }

    }

    public void clickLoadWeightData(View view) {
        /*
         * This method returns an arraylist of all the feeding_schedule entries which have already
         * occurred and which have an entry from the weight sensors. The string[] fields are:
         * {timeAndDate, weightChange}
         */
        Log.d("toplevel", "clickLoadWeightData");
        Log.d("loadWeightData", "clickLoadWeightData");
        ArrayList<String[]> weightData = this.dbInterface.loadWeightData();
        Log.d("loadWeightData", "# of Weight Data Records: " + weightData.size());
        for (String[] item: weightData) {
            for (String indiv: item) {
                Log.d("loadWeightData", "Weight Data rows: " + indiv);
            }
        }
    }

    public void clickLoadBreakBeamData(View view) {
        /*
         * This method returns an arraylist of all the breakbeam records stored on the phone
         * The list contains a string[] whose fields are {timeAndDate, number of occurrences (accesses),
         * dayOfWeek}
         */

        Log.d("toplevel", "clickLoadBreakBeamData");
        Log.d("loadBreakBeamData", "clickLoadBreakBeamData");

        ArrayList<String[]> breakBeamData = this.dbInterface.loadBreakBeamData();
        for (String[] item: breakBeamData) {
            for (String indiv: item) {
                Log.d("loadBreakBeamData", "BB Record: " + indiv);
            }
        }

    }

    public void clickDeleteAllBreakBeamRecords(View view) {
        /*
         * Needs to be used when there is a connection to microcontroller
         */
        Log.d("toplevel", "clickDeleteAllBreakBeamRecords");
        Log.d("deleteAllBreakBeamRecords", "clickDeleteAllBreakBeamRecords");

        boolean success = this.dbInterface.deleteAllBreakBeamRecords();
        Log.d("deleteAllBreakBeamRecords", "Delete Breakbeam Success: " + success);

    }

    public void clickDeleteAllFeedingRecords(View view) {
        /*
         * Needs to be used when there is a connection to microcontroller
         */
        Log.d("toplevel", "clickDeleteAllFeedingRecords");
        Log.d("deleteAllFeedingRecords", "clickDeleteAllFeedingRecords");

        boolean success = this.dbInterface.deleteAllFeedingRecords();
        Log.d("deleteAllFeedingRecords", "Delete all Feeding Records Success: " + success);
    }
}
