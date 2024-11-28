package com.example.carparkingpool;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PeakTimeActivity extends AppCompatActivity {

    private Button selectDateButton;
    private BarChart barChart; // BarChart for vertical display
    private DatabaseReference parkingRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peak_time);

        // Initialize UI components
        selectDateButton = findViewById(R.id.selectDateButton);
        barChart = findViewById(R.id.barChart); // BarChart instead of HorizontalBarChart
        parkingRef = FirebaseDatabase.getInstance().getReference("parking");

        // Set an OnClickListener to the selectDateButton to show a DatePicker
        selectDateButton.setOnClickListener(v -> showDatePicker());
    }

    // Show Date Picker Dialog
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(PeakTimeActivity.this, (view, selectedYear, selectedMonth, selectedDay) -> {
            // Format the selected date
            String selectedDate = String.format(Locale.getDefault(), "%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear);
            Toast.makeText(PeakTimeActivity.this, "Selected Date: " + selectedDate, Toast.LENGTH_SHORT).show();

            // Fetch and display data based on the selected date
            fetchParkingDataForDate(selectedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    // Fetch parking data for the selected date
    private void fetchParkingDataForDate(String selectedDate) {
        parkingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Array to store the vehicle counts per hour (24 hours)
                int[] hourlyCounts = new int[24];

                // Loop through all parking records
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot parkingSnapshot : userSnapshot.getChildren()) {
                        String createdDate = parkingSnapshot.child("createdTime").getValue(String.class);
                        String startTime = parkingSnapshot.child("startTime").getValue(String.class);
                        String endTime = parkingSnapshot.child("endTime").getValue(String.class);

                        if (createdDate != null && startTime != null && endTime != null && isSameDate(createdDate, selectedDate)) {
                            try {
                                // Parse start and end times
                                int startHour = Integer.parseInt(startTime.split(":")[0]);
                                int endHour = Integer.parseInt(endTime.split(":")[0]);

                                // Handle cases where end hour is before start hour (overnight parking)
                                if (endHour < startHour) {
                                    endHour += 24;
                                }

                                // Increment hourly counts for each hour in the parking duration
                                for (int hour = startHour; hour <= endHour; hour++) {
                                    hourlyCounts[hour % 24]++;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                // Display the data in the bar chart
                displayBarChart(hourlyCounts);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(PeakTimeActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Helper method to check if the parking record is from the same date
    private boolean isSameDate(String createdTime, String selectedDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date createdDate = sdf.parse(createdTime.split(" ")[0]);
            Date selected = sdf.parse(selectedDate);
            return createdDate != null && selected != null && createdDate.equals(selected);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Method to display the bar chart vertically
    private void displayBarChart(int[] hourlyCounts) {
        ArrayList<BarEntry> entries = new ArrayList<>();

        // Populate the entries for each hour (0 to 23)
        for (int i = 0; i < hourlyCounts.length; i++) {
            entries.add(new BarEntry(i, hourlyCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Parked Vehicles");

        // Customize the colors based on the counts
        int[] colors = new int[hourlyCounts.length];
        for (int i = 0; i < hourlyCounts.length; i++) {
            if (hourlyCounts[i] > 4) {
                colors[i] = ColorTemplate.rgb("#FF0000"); // Red for high count
            } else if (hourlyCounts[i] > 2) {
                colors[i] = ColorTemplate.rgb("#FFA500"); // Orange for medium count
            } else {
                colors[i] = ColorTemplate.rgb("#ADD8E6"); // Light Blue for low count
            }
        }
        dataSet.setColors(colors);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);  // Set bar width

        barChart.setData(barData);

        // Add chart interaction features
        barChart.setScaleEnabled(true);  // Enable scaling
        barChart.setPinchZoom(true);     // Allow pinch zoom
        barChart.setDragEnabled(true);   // Enable dragging

        // Customize the X-axis (this represents time in the format hh only)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getHoursList()));  // Set the hour labels (hh only)
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);  // Hide grid lines
        xAxis.setGranularity(1f);  // Set granularity to 1 hour
        xAxis.setLabelRotationAngle(90);  // Rotate labels to 90 degrees
        xAxis.setTextSize(12f); // Set label text size
        xAxis.setLabelCount(24); // Ensure all 24 hours are displayed
        xAxis.setAxisLineWidth(1f); // Set the width of the X-axis line

        // Set the X-axis label
        xAxis.setLabelRotationAngle(90); // Rotate X-axis labels to 90 degrees

        // Customize the Y-axis (this represents the vehicle counts)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0);  // Start Y-axis from 0
        leftAxis.setDrawGridLines(false);  // Hide grid lines
        leftAxis.setGranularity(1f);  // Ensure granularity for each count
        leftAxis.setAxisLineWidth(1f); // Set the width of the Y-axis line

        // Set label for Y-axis
        leftAxis.setTextSize(12f); // Set Y-axis label text size

        // Disable the right Y-axis, as it's not needed
        barChart.getAxisRight().setEnabled(false);

        // Add axis labels for X and Y axes
        barChart.getDescription().setEnabled(false);  // Hide description
        barChart.getLegend().setEnabled(false);  // Hide legend

        // Disable the display of values on top of the bars
        dataSet.setDrawValues(false);

        // Refresh the chart to display the data
        barChart.invalidate();
    }


    // Helper method to return a list of hours in the format hh
    private ArrayList<String> getHoursList() {
        ArrayList<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format(Locale.getDefault(), "%02d", i)); // Format as hh only
        }
        return hours;
    }
}
