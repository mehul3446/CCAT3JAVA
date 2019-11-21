package com.example.ccat3java;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class DatabaseInterface {

    private DataBaseManager dbManager;

    private static final int SERVERPORT = 5000;
    //private static final String SERVER_IP = "10.0.2.2";
    private static final String SERVER_IP = "192.168.1.20";

    private boolean requestSuccess = false;

    private class DataBaseCreator implements Callable {
        private static final String SQL_CREATE_BREAKBEAM = "CREATE TABLE break_beam_sensor_data (" +
                "occurrence_time DATETIME PRIMARY KEY," +
                "num_of_occurrences TEXT," +
                "day_of_week TEXT);";
        private static final String SQL_CREATE_FEEDING_SCHEDULE = "CREATE TABLE feeding_schedule (" +
                "feeding_time DATETIME PRIMARY KEY," +
                "weight_change TEXT," +
                "day_of_week TEXT," +
                "synchronized INT);";
        private static final String SQL_DROP_BREAKBEAM = "DROP TABLE IF EXISTS break_beam_sensor_data;";
        private static final String SQL_DROP_FEEDING_SCHEDULE = "DROP TABLE IF EXISTS feeding_schedule;";
        private static final String DB_NAME = "feeder_db";
        SQLiteDatabase db;
        private Context ctxt;
        private Boolean newDB;

        DataBaseCreator(Context ctxt, Boolean newDB) {
            super();
            this.ctxt = ctxt;
            this.newDB = newDB;
        }

        void createDataBase() {
            Log.d("mytag", "Create Database");
            this.db = ctxt.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
            Log.d("mytag", "Before sql");
            this.db.execSQL(SQL_DROP_BREAKBEAM);
            this.db.execSQL(SQL_DROP_FEEDING_SCHEDULE);
            this.db.execSQL((SQL_CREATE_FEEDING_SCHEDULE));
            this.db.execSQL(SQL_CREATE_BREAKBEAM);
            Log.d("mytag", "Post Database Creation");
        }

        @Override
        public SQLiteDatabase call() {
            Log.d("mytag", "Database constructor");
            File databasePath = ctxt.getDatabasePath(DB_NAME);
            Log.d("mytag", "db path: " + databasePath);
            if (databasePath.exists() && !this.newDB) {
                Log.d("mytag", "exists");
                this.db = ctxt.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
            } else {
                Log.d("mytag", "doesn't exist");
                createDataBase();
            }
            Log.d("mytag", "Post if statement");
            return this.db;
        }
    }

    private class DataBaseManager {

        private SQLiteDatabase db;

        DataBaseManager(SQLiteDatabase db) {
            this.db = db;
        }

        void insertFeedingTime(String timeAndDate, String day) {
            this.db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("feeding_time", timeAndDate);
            values.put("day_of_week", day);
            values.put("synchronized", 0);
            this.db.insert("feeding_schedule", null, values);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        public boolean queryFeedingTime(String timeAndDate) {
            //Check whether a particular time is already in the database
            String[] selectionArgs = {timeAndDate};
            Log.d("mytag", "Begin query");
            Cursor cursor = this.db.rawQuery("SELECT * FROM feeding_schedule WHERE feeding_time = ?", selectionArgs);
            Log.d("mytag", "Post query");
            int count = cursor.getCount(); //Find out if there are any rows with that timeAndDate
            Log.d("mytag", "get count " + count);
            cursor.close();
            if (count == 0) {
                return false;
            } else {
                return true;
            }
        }

        void deleteFeedingRecord(String timeAndDate) {
            this.db.beginTransaction();
            String[] selectionArgs = {timeAndDate};
            this.db.execSQL("DELETE FROM feeding_schedule WHERE feeding_time = ?", selectionArgs);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        void deleteAllFeedingRecords() {
            this.db.beginTransaction();
            this.db.execSQL("DELETE FROM feeding_schedule;");
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        void deleteAllBreakBeamRecords() {
            this.db.beginTransaction();
            this.db.execSQL("DELETE FROM break_beam_sensor_data");
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        void syncFeedingTime(String timeAndDate) {
            // updates feeding time record to give synchronized column a 1, or true value
            this.db.beginTransaction();
            String[] selectionArgs = {timeAndDate};
            String query = "UPDATE feeding_schedule SET synchronized = "
                    + 1 + " WHERE feeding_time = ?";
            Cursor cursor = this.db.rawQuery(query, selectionArgs);
            cursor.moveToFirst();
            cursor.close();
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        ArrayList<String[]> queryUnsyncedFeedingTimes() {
            // queries feeding times which have not been synced and returns an array of the feeding
            // times and days
            Log.d("mytag", "get unsynced times");
            ArrayList<String[]> output = new ArrayList<>();
            String[] selectionArgs = {"0"};
            Cursor cursor = this.db.rawQuery("SELECT feeding_time, day_of_week FROM feeding_schedule " +
                    "WHERE synchronized = ?", selectionArgs);
            Log.d("mytag", String.valueOf(cursor.getCount()));
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    String time = cursor.getString(cursor.getColumnIndex("feeding_time"));
                    String day = cursor.getString(cursor.getColumnIndex("day_of_week"));
                    output.add(new String[]{time, day});
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return output;
        }

        void updateWeightChange(String timeAndDate, String weightChange) {
            Log.d("fetchWeightChanges", "Update Method, Weight Change: " + weightChange);
            if (!weightChange.equals("null")) {
                Log.d("fetchWeightChanges", "Update Method, Weight Change not null");
                this.db.beginTransaction();
                ContentValues values = new ContentValues();
                values.put("weight_change", weightChange);
                String whereClause = "feeding_time = ?";
                String[] whereArgs = {timeAndDate};
                this.db.update("feeding_schedule", values, whereClause, whereArgs);
                this.db.setTransactionSuccessful();
                this.db.endTransaction();
            }
        }

        void insertBreakBeamRecord(String occurrenceTime, String dayOfWeek, String numOfOccurrences ) {
            this.db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("occurrence_time", occurrenceTime);
            values.put("num_of_occurrences", numOfOccurrences);
            values.put("day_of_week", dayOfWeek);
            this.db.insert("break_beam_sensor_data", null, values);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        void updateBreakBeamRecord(String timeAndDate, String numOfOccurrences) {
            this.db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("num_of_occurrences", numOfOccurrences);
            String whereClause = "occurrence_time = ?";
            String[] whereArgs = {timeAndDate};
            this.db.update("break_beam_sensor_data", values, whereClause, whereArgs);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
        }

        ArrayList<String> queryBreakBeamOccurrence(String timeAndDate) {
            String[] selectionArgs = {timeAndDate};
            Cursor cursor = this.db.rawQuery("SELECT * FROM break_beam_sensor_data " +
                    "WHERE occurrence_time = ?", selectionArgs);
            ArrayList<String> outputArray = new ArrayList<>();
            if (cursor.moveToFirst()) {
                outputArray.add(cursor.getString(cursor.getColumnIndex("occurrence_time")));
                outputArray.add(cursor.getString(cursor.getColumnIndex("num_of_occurrences")));
                outputArray.add(cursor.getString(cursor.getColumnIndex("day_of_week")));
            }
            cursor.close();

            return outputArray;
        }

        ArrayList<String> queryPotentialWeightChangeUpdates (String currentTimeAndDate) {
            ArrayList<String> output = new ArrayList<>();
            String[] whereArgs = {currentTimeAndDate};
            Log.d("fetchWeightChanges", "Current Time: " + currentTimeAndDate);
            Cursor cursor = this.db.rawQuery("SELECT feeding_time FROM feeding_schedule " +
                    "WHERE weight_change IS null AND feeding_time < ?", whereArgs);
            if (cursor.moveToFirst()) {
                Log.d("fetchWeightChanges", "Record exists");
                while (!cursor.isAfterLast()) {
                    output.add(cursor.getString(cursor.getColumnIndex("feeding_time")));
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return output;
        }

        String queryLatestBreakBeamTime() {
            Log.d("fetchBreakBeamRecords", "Query lastest time");
            Cursor cursor = this.db.rawQuery("SELECT occurrence_time FROM break_beam_sensor_data " +
                            "ORDER BY occurrence_time DESC LIMIT 1", null);
            if (cursor.moveToFirst()) {
                Log.d("fetchBreakBeamRecords", "Records exist");
                String latestTime = cursor.getString(cursor.getColumnIndex("occurrence_time"));
                cursor.close();
                return latestTime;

            } else {
                Log.d("fetchBreakBeamRecords", "No records");
                cursor.close();
                return "";
            }
        }

        ArrayList<String[]> queryFutureFeedingTimes(String currentTimeAndDate) {
            Log.d("loadCurrentFeedingTime", "query Start");
            ArrayList<String[]> output = new ArrayList<>();
            String[] whereArgs = {currentTimeAndDate};
            Cursor cursor = this.db.rawQuery("SELECT feeding_time, day_of_week, synchronized " +
                    "FROM feeding_schedule WHERE feeding_time > ?", whereArgs);
            if (cursor.moveToFirst()) {
                Log.d("loadCurrentFeedingTime", "Query, record exists");
                while(!cursor.isAfterLast()) {
                    String[] record = {cursor.getString(cursor.getColumnIndex("feeding_time")),
                            cursor.getString(cursor.getColumnIndex("day_of_week")),
                            cursor.getString(cursor.getColumnIndex("synchronized"))};
                    output.add(record);
                    Log.d("loadCurrentFeedingTime", "Record: " +
                            cursor.getString(cursor.getColumnIndex("feeding_time")));
                    cursor.moveToNext();
                }
            }
            cursor.close();
            Log.d("loadCurrentFeedingTime", "OUTPUT: " + output.toString());
            return output;
        }

        ArrayList<String[]> queryWeightChangeData () {
            ArrayList<String[]> output = new ArrayList<>();
            Cursor cursor = this.db.rawQuery("SELECT feeding_time, weight_change " +
                    "FROM feeding_schedule WHERE weight_change IS NOT NULL", null);
            if (cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
                    String[] record = {cursor.getString(cursor.getColumnIndex("feeding_time")),
                            cursor.getString(cursor.getColumnIndex("weight_change"))};
                    output.add(record);
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return output;
        }

        ArrayList<String[]> queryBreakBeamData () {
            ArrayList<String[]> output = new ArrayList<>();
            Cursor cursor = this.db.rawQuery("SELECT occurrence_time, num_of_occurrences, day_of_week " +
                    "FROM break_beam_sensor_data", null);
            if (cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
                    String[] record = {cursor.getString(cursor.getColumnIndex("occurrence_time")),
                            cursor.getString(cursor.getColumnIndex("num_of_occurrences")),
                            cursor.getString(cursor.getColumnIndex("day_of_week"))};
                    output.add(record);
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return output;
        }
    }

    private class ClientThread implements Runnable {

        private int request;
        private String[] payload;
        private PrintWriter out;
        private BufferedReader in;

        ClientThread(int request, String[] payload) {
            super();
            Log.d("mytag", "thread constructor");
            this.request = request;
            this.payload = payload;
        }

        @Override
        public void run() {
            Log.d("mytag", "running");

            try {
                //InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByName(SERVER_IP), SERVERPORT),
                        1000);
                socket.setSoTimeout(100);

                Log.d("mytag", "post socket creation");
                out = new PrintWriter(socket.getOutputStream(), false);
                in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                Log.d("mytag", "after output");
                try {
                    boolean success = requestHandler(this.request, this.payload);
                    requestSuccess = success;
                    Log.d("mytag", "success " + success);
                } finally {
                    out.close();
                    in.close();
                    Log.d("mytag", "post close");
                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                Log.d("mytag", e1.toString());
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.d("mytag", e1.toString());
            }
            Log.d("mytag", "????");

        }

        boolean requestHandler(int request, String[] payload) {
            /*
             * request types:
             * 1 insert_new_feeding_time
             * 2 query_weight_change
             * 3 query_break_beam_data_past_date
             * 4 delete_feeding_time
             * 5 delete all feeding times
             * 6 delete all break beam records
             * 7 activate feeder now
             * */
            if (request == 1) {
                Log.d("mytag", "Insert new feeding time");
                insertNewFeedingTimeRequest(payload);
                return readSuccessMessage();
            } else if (request == 2) {
                return queryWeightChangeRequest(payload);
            } else if (request == 3) {
                return queryBreakBeamDataRequest(payload);
            } else if (request == 4){
                deleteFeedingTimeRequest(payload);
                return readSuccessMessage();
            } else if (request == 5){
                deleteAllFeedingTimesRequest();
                return readSuccessMessage();
            } else if (request == 6) {
                deleteAllBreakBeamRecords();
                return readSuccessMessage();
            } else {
                activateFeederRequest();
                return readSuccessMessage();
            }
        }

        boolean readSuccessMessage() {
            char[] cbuf = new char[2];
            try {
                in.read(cbuf, 0, 2);
                return String.valueOf(cbuf).equals("00");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("mytag", e.toString());
                return false;
            }
        }

        void insertNewFeedingTimeRequest(String[] payload) {
            // Sends a new feeding time to microcontroller
            ArrayList<String> keys = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            keys.add("feeding_time");
            keys.add("day_of_week");

            for (String item: payload) {
                values.add(item);
            }
            String contentBody = jsonDumps(keys, values);
            Log.d("mytag", contentBody);
            String jsonHeader = createJSONHeader(1, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            Log.d("mytag", lengthHeader);
            Log.d("mytag", message);
            out.print(message);
            out.flush();
        }

        boolean queryWeightChangeRequest(String[] payload) {
            // Asks microcontroller for weight change records
            // and inserts them into the database
            Log.d("fetchWeightChanges", "Query Weight Change Request");
            ArrayList<String> keys = new ArrayList<>();
            keys.add("feeding_time");
            ArrayList<String> values = new ArrayList<>();
            for (String item: payload) {
                values.add(item);
            }
            String contentBody = jsonDumps(keys, values);
            Log.d("mytag", contentBody);
            String jsonHeader = createJSONHeader(2, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            Log.d("mytag", lengthHeader);
            Log.d("mytag", message);
            out.print(message);
            out.flush();



            int returnLengthHeader = Integer.parseInt(readBytes(in, 2));
            Log.d("mytag", String.valueOf(returnLengthHeader));
            JSONObject returnJsonHeader = jsonLoads(readBytes(in, returnLengthHeader));
            Log.d("mytag", returnJsonHeader.toString());
            try {
                Log.d("mytag", returnJsonHeader.getString("content_size"));
                JSONObject returnContent = jsonLoads(readBytes(in,
                        Integer.parseInt(returnJsonHeader.getString("content_size"))));
                Log.d("mytag", returnContent.toString());
                dbManager.updateWeightChange(returnContent.getString("feeding_time"),
                        returnContent.getString("weight_change"));
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("mytag", e.toString());
                return false;
            }
        }

        boolean queryBreakBeamDataRequest(String[] payload) {
            // Asks for break beam records past a certain date
            // and inserts them into the database
            ArrayList<String> keys = new ArrayList<>();
            keys.add("occurrence_time");
            ArrayList<String> values = new ArrayList<>();
            for (String item: payload) {
                values.add(item);
            }
            String contentBody = jsonDumps(keys, values);
            Log.d("mytag", contentBody);
            String jsonHeader = createJSONHeader(3, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            Log.d("mytag", lengthHeader);
            Log.d("mytag", message);
            out.print(message);
            out.flush();


            String buff = readBytes(in, 2);
            Log.d("fetchBreakBeamRecords", "Length Header String: " + buff);
            int returnLengthHeader = Integer.parseInt(buff);
            Log.d("mytag", String.valueOf(returnLengthHeader));
            while (returnLengthHeader > 0) {
                try {
                    JSONObject returnJsonHeader = jsonLoads(readBytes(in, returnLengthHeader));
                    Log.d("mytag", returnJsonHeader.toString());
                    Log.d("mytag", returnJsonHeader.getString("content_size"));
                    JSONObject returnContent = jsonLoads(readBytes(in,
                            Integer.parseInt(returnJsonHeader.getString("content_size"))));
                    Log.d("mytag", returnContent.toString());

                    ArrayList<String> occurenceInTable = dbManager.queryBreakBeamOccurrence(
                            returnContent.getString("occurrence_time"));
                    if (occurenceInTable.size() > 0) {
                        dbManager.updateBreakBeamRecord(returnContent.getString("occurrence_time"),
                                returnContent.getString("num_of_occurrences"));
                    } else {
                        dbManager.insertBreakBeamRecord(returnContent.getString("occurrence_time"),
                                returnContent.getString("day_of_week"),
                                returnContent.getString("num_of_occurrences"));
                    }

                    returnLengthHeader = Integer.parseInt(readBytes(in, 2));
                    Log.d("mytag", String.valueOf(returnLengthHeader));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("mytag", e.toString());
                    return false;
                }
            }
            return true;

        }

        void deleteFeedingTimeRequest (String[] payload) {
            ArrayList<String> keys = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            keys.add("feeding_time");
            values.add(payload[0]);
            String contentBody = jsonDumps(keys, values);
            String jsonHeader = createJSONHeader(4, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            out.print(message);
            out.flush();
        }

        void deleteAllFeedingTimesRequest () {
            ArrayList<String> keys = new ArrayList<>(Arrays.asList(""));
            ArrayList<String> values = new ArrayList<>(Arrays.asList(""));
            String contentBody = jsonDumps(keys, values);
            String jsonHeader = createJSONHeader(5, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            out.print(message);
            out.flush();
        }

        public void deleteAllBreakBeamRecords () {
            ArrayList<String> keys = new ArrayList<>(Arrays.asList(""));
            ArrayList<String> values = new ArrayList<>(Arrays.asList(""));
            String contentBody = jsonDumps(keys, values);
            String jsonHeader = createJSONHeader(6, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            out.print(message);
            out.flush();
        }

        public void activateFeederRequest() {
            // Sends a message to feeder to activate now
            ArrayList<String> keys = new ArrayList<>(Arrays.asList(""));
            ArrayList<String> values = new ArrayList<>(Arrays.asList(""));
            String contentBody = jsonDumps(keys, values);
            String jsonHeader = createJSONHeader(7, contentBody);
            String lengthHeader = createLengthHeader(jsonHeader);
            String message = lengthHeader + jsonHeader + contentBody;
            out.print(message);
            out.flush();
        }

        public String readBytes(BufferedReader reader, int numOfBytes) {
            char[] cbuf = new char[numOfBytes];
            try {
                reader.read(cbuf, 0, numOfBytes);
                return String.valueOf(cbuf);
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }

        public String createJSONHeader(int request, String data) {
            String dataSize = getMessageLength(data);
            String request_str = Integer.toString(request);
            ArrayList<String> keys = new ArrayList<>(Arrays.asList("content_size", "request"));
            ArrayList<String> values = new ArrayList<>(Arrays.asList(dataSize, request_str));
            String jsonHeader = jsonDumps(keys, values);
            return jsonHeader;
        }

        public String createLengthHeader(String jsonHeader) {
            return getMessageLength(jsonHeader);
        }

        public String getMessageLength(String msg) {
            try {
                byte[] sizeBytes = msg.getBytes("UTF-8");
                int size = sizeBytes.length;
                String strSize = Integer.toString(size);
                String finalStrSize;
                if (strSize.length() < 2) {
                    finalStrSize = '0' + strSize;
                } else {
                    finalStrSize = strSize;
                }
                return finalStrSize;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return "";

        }

        public String jsonDumps(ArrayList<String> keys, ArrayList<String> values) {
            JSONObject obj = new JSONObject();
            try {
                for (int i = 0; i < keys.size(); i++) {
                    obj.put(keys.get(i), values.get(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj.toString();

        }

        public JSONObject jsonLoads(String jsonString) {
            try {
                JSONObject obj = new JSONObject(jsonString);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;

        }
    }

    public DatabaseInterface(Context ctxt) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<SQLiteDatabase> future = executor.submit(new DataBaseCreator(ctxt.getApplicationContext(),
                false));
        try {
            SQLiteDatabase db = future.get();
            this.dbManager = new DataBaseManager(db);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public boolean insertFeedingTime (String newFeedingTime, String dayOfWeek) {
        this.dbManager.insertFeedingTime(newFeedingTime, dayOfWeek);
        String[] payload = {newFeedingTime, dayOfWeek};
        Thread request = new Thread(new ClientThread(1, payload));
        request.start();
        try {
            request.join();
            if (requestSuccess) {
                requestSuccess = false;
                this.dbManager.syncFeedingTime(newFeedingTime);
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean updateFeedingTime (String feedingTime, String newFeedingTime, String dayOfWeek) {
        boolean success = deleteFeedingTime(feedingTime);
        if (success) {
            success = insertFeedingTime(newFeedingTime, dayOfWeek);
            if (success) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteFeedingTime (String feedingTime) {
        String[] payload = {feedingTime};
        Thread request = new Thread(new ClientThread(4, payload));
        request.start();
        try {
            request.join();
            if (requestSuccess) {
                requestSuccess = false;
                this.dbManager.deleteFeedingRecord(feedingTime);
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean syncFeedingTimes () {
        ArrayList<String[]> unsyncedFeedingTimes = this.dbManager.queryUnsyncedFeedingTimes();
        Log.d("syncFeedingTimes", "Number of records to sync: " + unsyncedFeedingTimes.size());
        for (String[] item: unsyncedFeedingTimes) {
            String[] payload = {item[0], item[1]};
            Thread request = new Thread(new ClientThread(1, payload));
            request.start();
            try {
                request.join();
                this.dbManager.syncFeedingTime(item[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }

        }
        return true;
    }

    public boolean fetchWeightChanges () {
        Log.d("fetchWeightChanges", "Fetch Weight Changes Method Call");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        ArrayList<String> potentialWeightChangeUpdates = this.dbManager.queryPotentialWeightChangeUpdates(dtf.format(now));
        Log.d("fetchWeightChanges", "Potential Weight Change Records: " + potentialWeightChangeUpdates.toString());
        boolean success = true;
        for (String item: potentialWeightChangeUpdates) {
            String[] payload = {item};
            Thread request = new Thread(new ClientThread(2, payload));
            request.start();
            try {
                request.join();
                if (requestSuccess) {
                    success = true;
                    requestSuccess = false;
                } else {
                    return false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return success;
    }

    public boolean fetchBreakBeamRecords () {
        // Queries the microcontroller for new break beam records
        String latestBreakBeam = this.dbManager.queryLatestBreakBeamTime();
        Log.d("fetchBreakBeamRecords", "Latest Break Beam: " + latestBreakBeam);
        String[] payload = {latestBreakBeam};
        Thread request = new Thread(new ClientThread(3, payload));
        request.start();
        try {
            request.join();
            if (requestSuccess) {
                requestSuccess = false;
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<String[]> loadCurrentFeedingTimes () {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        Log.d("loadCurrentFeedingTime", "Current Time: " + dtf.format(now));
        return this.dbManager.queryFutureFeedingTimes(dtf.format(now));
    }

    public ArrayList<String[]> loadWeightData () {
        return this.dbManager.queryWeightChangeData();
    }

    public ArrayList<String[]> loadBreakBeamData () {
        return this.dbManager.queryBreakBeamData();
    }

    public boolean deleteAllBreakBeamRecords () {
        String[] payload = {""};
        Thread request = new Thread(new ClientThread(6, payload));
        request.start();
        try {
            request.join();
            if (requestSuccess) {
                requestSuccess = false;
                this.dbManager.deleteAllBreakBeamRecords();
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean deleteAllFeedingRecords () {
        String[] payload = {""};
        Thread request = new Thread(new ClientThread(5, payload));
        request.start();
        try {
            request.join();
            if (requestSuccess) {
                requestSuccess = false;
                this.dbManager.deleteAllFeedingRecords();
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
