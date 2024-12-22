package com.example.mayfu11;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class Menu extends Fragment {

    private Switch notificationSwitch;
    private float userBmi = 22.0f; // Example value, should be calculated dynamically
    private int userGender = 0; // Assuming 0 = Male, 1 = Female. Should come from SharedPreferences.
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_menu, container, false);

        notificationSwitch = view.findViewById(R.id.notificationSwitch);
        setupNotificationSwitch();

        // Load gender and BMI from SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserDataPrefs", Context.MODE_PRIVATE);
        userGender = sharedPreferences.getInt("gender", -1); // Default -1 if not set
        userBmi = calculateBmi(sharedPreferences); // Assuming you have a function to calculate BMI

        // Determine BMI category and load layout
        loadBmiLayout(userBmi);

        // Refresh button action (optional)
        ImageButton refreshButton = view.findViewById(R.id.refresh);
        refreshButton.setOnClickListener(v -> Toast.makeText(getContext(), "Refreshing the menu", Toast.LENGTH_SHORT).show());

        return view;
    }

    // Method to calculate BMI, you might already have this logic somewhere
    private float calculateBmi(SharedPreferences sharedPreferences) {
        String weightStr = sharedPreferences.getString("weight", "0");
        String heightStr = sharedPreferences.getString("height", "0");
        try {
            float weight = Float.parseFloat(weightStr);
            float height = Float.parseFloat(heightStr) / 100; // Convert cm to meters
            return weight / (height * height);
        } catch (NumberFormatException e) {
            return 22.0f; // Default BMI if parsing fails
        }
    }

    // Method to determine BMI category based on gender
    private String getBmiCategory(float bmi, int gender) {
        if (gender == 0) { // Male
            if (bmi < 18.5) {
                return "Underweight";
            } else if (bmi >= 18.5 && bmi < 24.9) {
                return "Normal weight";
            } else if (bmi >= 25 && bmi < 29.9) {
                return "Overweight";
            } else {
                return "Obesity";
            }
        } else { // Female
            if (bmi < 18.5) {
                return "Underweight";
            } else if (bmi >= 18.5 && bmi < 24.4) { // Adjusted upper limit for women
                return "Normal weight";
            } else if (bmi >= 24.5 && bmi < 29.9) {
                return "Overweight";
            } else {
                return "Obesity";
            }
        }
    }

    // Load layout based on BMI category
    private void loadBmiLayout(float bmi) {
        String bmiCategory = getBmiCategory(bmi, userGender);

        int layoutId = getLayoutIdBasedOnBmiCategory(bmiCategory);
        if (layoutId != 0) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View bmiLayout = inflater.inflate(layoutId, null);

            // Add BMI layout to container
            ViewGroup bmiLayoutContainer = view.findViewById(R.id.bmiLayoutContainer);
            bmiLayoutContainer.removeAllViews(); // Remove previous layout
            bmiLayoutContainer.addView(bmiLayout);
        }
    }

    // Get layout ID based on BMI category
    private int getLayoutIdBasedOnBmiCategory(String bmiCategory) {
        switch (bmiCategory) {
            case "Underweight":
                return R.layout.activity_fp_lowbmi_female; // Replace with actual layout
            case "Normal weight":
                return R.layout.activity_fp_standardbmi_female; // Replace with actual layout
            case "Overweight":
                return R.layout.activity_fp_highbmi_female; // Replace with actual layout
            case "Obesity":
                return R.layout.activity_fp_highbmi_male; // Replace with actual layout
            default:
                return 0;
        }
    }

    // Setup notification switch
    private void setupNotificationSwitch() {
        // Load notification switch state from SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isNotificationEnabled = sharedPreferences.getBoolean("notificationEnabled", false);
        notificationSwitch.setChecked(isNotificationEnabled);

        // Enable notifications if switch is already active
        if (isNotificationEnabled) {
            scheduleAllMealNotifications(requireContext());
        }

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save switch state to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notificationEnabled", isChecked);
            editor.apply();

            if (isChecked) {
                // Schedule notifications
                scheduleAllMealNotifications(requireContext());
                Toast.makeText(getContext(), "Meal notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Cancel all notifications
                cancelAllMealNotifications(requireContext());
                Toast.makeText(getContext(), "Meal notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Schedule all meal notifications
    private void scheduleAllMealNotifications(Context context) {
        setMealNotification(context, 6, 0, "Breakfast Reminder", "It's time for breakfast!");
        setMealNotification(context, 11, 0, "Lunch Reminder", "It's time for lunch!");
        setMealNotification(context, 19, 0, "Dinner Reminder", "It's time for dinner!");
    }

    // Schedule a single meal notification
    private void setMealNotification(Context context, int hour, int minute, String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, 1);
                return;
            }
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        int requestCode = getNotificationRequestCode(hour, minute);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private int getNotificationRequestCode(int hour, int minute) {
        return hour * 100 + minute;
    }

    // Cancel all meal notifications
    private void cancelAllMealNotifications(Context context) {
        cancelMealNotification(context, 6, 0);
        cancelMealNotification(context, 11, 0);
        cancelMealNotification(context, 19, 0);
    }

    private void cancelMealNotification(Context context, int hour, int minute) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        int requestCode = getNotificationRequestCode(hour, minute);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
