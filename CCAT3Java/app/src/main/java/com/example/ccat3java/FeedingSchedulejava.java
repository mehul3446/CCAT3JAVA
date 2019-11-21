package com.example.ccat3java;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedingSchedulejava extends AppCompatActivity {
private Button enterFeedingTimeBtn;
final Calendar feedingCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date;
    TimePickerDialog.OnTimeSetListener timePickerListener;
    List<String> feedingList;
    ArrayAdapter feedingListAdapter;

    private SharedPreference sharedPreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeding_schedulejava);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        feedingList = new ArrayList<>();

        sharedPreference = new SharedPreference();

// Feeding Schedule Button
        enterFeedingTimeBtn = (Button)findViewById(R.id.enterFeedingTimeBtn);
        enterFeedingTimeBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               new DatePickerDialog(FeedingSchedulejava.this, date, feedingCalendar
                       .get(Calendar.YEAR), feedingCalendar.get(Calendar.MONTH),
                       feedingCalendar.get(Calendar.DAY_OF_MONTH)).show();

            }
        });

        date = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                feedingCalendar.set(Calendar.YEAR, year);
                feedingCalendar.set(Calendar.MONTH, monthOfYear);
                feedingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                new TimePickerDialog(FeedingSchedulejava.this, timePickerListener, feedingCalendar.get(Calendar.HOUR), feedingCalendar.get(Calendar.MINUTE), false).show();
                // updateLabel();
            }

        };

        timePickerListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minutes) {
// TODO Auto-generated method stub
                feedingCalendar.set(Calendar.HOUR, hourOfDay);
                feedingCalendar.set(Calendar.MINUTE, minutes);

                sharedPreference.addTimes(getApplicationContext(), feedingCalendar.getTime());
                updateAdapter();
            }
        };

        ListView feedingListView = (ListView) findViewById(R.id.feedingListView);
        feedingListAdapter = new ArrayAdapter<String>(this,
                R.layout.activity_listview, feedingList);
        feedingListView.setAdapter(feedingListAdapter);

        feedingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {


                AlertDialog.Builder builder = new AlertDialog.Builder(FeedingSchedulejava.this);

                builder.setTitle("Do you want to delete this feeding time ?");

                        // Set the action buttons
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK, so save the mSelectedItems results somewhere
                                // or return them to the component that opened the dialog
                                sharedPreference.removeTimes(getApplicationContext(), position);
                                updateAdapter();
                            }
                        });

                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });


                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        updateAdapter();
    }

    private void updateAdapter(){

        String myFormat = "MMM dd yy hh:mm aaa"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        List<Date> dateList = sharedPreference.getTimes(getApplicationContext());
        feedingList.clear();

        if(dateList != null) {
            for (Date date : dateList)
                feedingList.add(sdf.format(date));

            feedingListAdapter.notifyDataSetChanged();
        }
    }








}
