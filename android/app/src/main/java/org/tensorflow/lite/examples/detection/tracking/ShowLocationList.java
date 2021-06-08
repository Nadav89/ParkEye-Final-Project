package org.tensorflow.lite.examples.detection.tracking;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.detection.R;

public class ShowLocationList extends AppCompatActivity {

    ListView viewLocationList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_location_list);
        viewLocationList = findViewById(R.id.viewLocationList);

        DataBaseHelper dataBaseHelper = new DataBaseHelper(ShowLocationList.this);
        showLocationOnListView(dataBaseHelper);
    }

    private void showLocationOnListView(DataBaseHelper dataBaseHelper) {
        ArrayAdapter locationDataArrayAdapter = new ArrayAdapter<LocationData>(ShowLocationList.this, android.R.layout.simple_list_item_1, dataBaseHelper.getEveryone());
        viewLocationList.setAdapter(locationDataArrayAdapter);
    }
}
