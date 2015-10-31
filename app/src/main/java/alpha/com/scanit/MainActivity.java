package alpha.com.scanit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alpha.ZXing.android.IntentIntegrator;
import com.alpha.ZXing.android.IntentResult;

import android.util.Log;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity {
    /**
     * Private Strings
     */
    private static final String PREFS_NAME = "Scan_IT";
    private static final String TAG = "MAKE_BARCODES";
    private String ScanFromFedEXG;
    private String ScanFromFedEXGManual;
    private String ScanFromFedEXGO;
    private String ScanFromFedEX;
    private String ScanFromFedEXManual;
    private String ScanFromUPS;
    private String ScanFromManualUPS;
    private String Replace;
    private String[] log = new String[100];
    private String Output;
    private String TypeG = "";
    private String TypeE = "";
    private Boolean Error = false;
    private TextView CounterTxt;
    private Integer Counter = 0;
    private Integer size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /**
         * For hiding Window Title
         */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        CreateListView();
        LoadData();

//Todo: Manual button transparent

        Button scanBtn = (Button) findViewById(R.id.scan_button);
        Button clrBtn = (Button) findViewById(R.id.clr_button);
        Button mnlBtn = (Button) findViewById(R.id.manual_button);

        CounterTxt = (TextView) findViewById(R.id.textView2);

        scanBtn.setOnClickListener(new ScanButton());
        mnlBtn.setOnClickListener(new ManButton());
        clrBtn.setOnClickListener(new ClearButton());
    }

    private void ClearButtonData() {
        SQLite db = new SQLite(this);
        db.deleteBarcodes();
        CreateListView();
        db.close();
        CounterTxt.setText("0");
    }

    private void ScanDataEmpty() {
        Toast toast = Toast.makeText(getApplicationContext(),
                "No scan data received!", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void InputManual() {

        LayoutInflater Manual = LayoutInflater.from(this);

        //Todo: Remove Null
        @SuppressLint("AndroidLintInflateParams")
        final View textEntryView = Manual.inflate(R.layout.manual_entry, null);
        final EditText infoTrack = (EditText) textEntryView.findViewById(R.id.InfoTrack);
        final EditText infoData = (EditText) textEntryView.findViewById(R.id.InfoData);

        final SQLite db = new SQLite(this);
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setIcon(R.drawable.ic_dialog_alert_holo_light).setTitle("Manual Entry").setView(textEntryView).setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {

                        // Log.i("AlertDialog","Entered "+infoData.getText().toString());
                        // Log.i("AlertDialog", "Entered " + infoTrack.getText().toString());

                        Editable value = infoData.getText();
                        String Result = infoTrack.getText().toString();

                        if (Result.length() == 0) { ScanDataEmpty(); }

                        if (Result.length() == 12 && Result.length() != 21) {
                                TypeE = "E";
                            }
                        if (Result.length() == 21) {
                            String buffer = "MJ";
                            final String ManualScanFedExG = buffer + Result.substring(Result.length() - 21, Result.length());

                            db.addBarcodes(new Barcodes(ManualScanFedExG, value.toString()));
                            Counter++;
                            db.close();

                            CreateListView();
                            //Todo: Fix Boxing
                            CounterTxt.setText(Integer.valueOf(Counter).toString());

                        }
                        else if (Result.length() != 0) {
                            db.addBarcodes(new Barcodes(Result, value.toString()));
                            Counter++;
                            db.close();

                            CreateListView();

                            CounterTxt.setText(Integer.valueOf(Counter).toString());
                        }
                    }
                }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        ScanDataEmpty();
                    }
                });
        alert.show();
    }

    private void FormatEmail() {

        /**
         * Display all bar codes
         * Log.d(TAG, "Reading - Display Results");
         * */
        SQLite db = new SQLite(this);
        List<Barcodes> Barcodes = db.getBarCodes();
        db.close();
        int i = 0;
        for (Barcodes cn : Barcodes) {
            String bar = cn.getBarcode();
            /**
             * Fedex Ground OLD - 22 Characters add MJ to display on screen
             */
            if (bar.length() == 24) {
                Output = FedEXGO(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
            /**
             * UPS Manual - 23 Characters
             */
            if (bar.length() == 23 && bar.contains("Z")) {
                Output = ManualUPS(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
            /**
             * Fedex Ground Manual - 23 Characters
             */
            if (bar.length() == 23 && !bar.contains("Z")) {
                Output = ManualFedEXG(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
            /**
             * UPS - 18 Characters
             */
            if (bar.length() == 18) {
                Output = UPS(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
            /**
             * Fedex Express Manual - 14 Characters
             */
            if (bar.length() == 14) {
                Output = ManualFedEXE(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
            /**
             * Fedex Express - 34 Characters
             * Fedex Ground NEW - 34 Characters && TypeE.equals("E")
             */
            if (bar.length() == 12 && TypeE.equals("E")) {
                TypeG = "";
                Output = FedEXE(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
            /**
             * Fedex Ground OLD - 32 Characters
             */
            if (bar.length() == 12 && TypeG.equals("G")) {
                TypeE = "";
                Output = FedEXG(bar);
                log[i] = "\n" + Output + " / " + cn.getCompany();
                i++;
            }
        }

    }

    private void emailResults() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_SUBJECT, "Tracking Numbers");
        String[] TO = {"Receiving@cdaresort.com"};
        i.putExtra(Intent.EXTRA_EMAIL, TO);
        FormatEmail();
        String newString = Arrays.toString(log);
        String FilterA = newString.replace(", null", "");
        String FilterB = FilterA.replace("[", "");
        String FilterC = FilterB.replace("]", "");
        String FilterD = FilterC.replace(",", "");
        i.putExtra(Intent.EXTRA_TEXT, FilterD);
        Arrays.fill(log, null);
        Counter = 0;
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Filter for screen
     */
    private String Filter(String Number) {

        if (Number.length() == 34) {
            /**
             * System.out.println("Added Type Fedex Express");
             */
            final String ScanFromFedEXE = Number.substring(Number.length() - 12, Number.length());
            TypeE = "E";
            TypeG = "";
            return ScanFromFedEXE;
        }
        if (Number.length() == 32) {
            /**
             * System.out.println("Added Type Fedex Ground");
             */
            final String ScanFromFedEXG = Number.substring(Number.length() - 16, Number.length() - 4);
            TypeG = "G";
            TypeE = "";
            return ScanFromFedEXG;
        }
        if (Number.length() == 22) {
            /**
             * System.out.println("Added Type Fedex Ground Old");
             */
            String buffer = "MJ";
            ScanFromFedEXG = buffer + Number.substring(Number.length() - 22, Number.length());

            return ScanFromFedEXG;
        }
        if (Number.length() < 11) {
            /**
             * Error
             */
            ScanDataEmpty();
        }

        return Number;
    }

    /**
     * Formatting
     */
    private String FedEXG(String Number) {
        /**
         * 32 Characters
         * Fedex Ground
         * Format: XXXX XXXX XXXX
         */
        ScanFromFedEXGO = Number.substring(0, 4) + " " + Number.substring(4, 8) + " " + Number.substring(8, 12);
        return ScanFromFedEXGO;
    }

    private String ManualFedEXG(String Number) {
        /**
         * 23 Characters
         * Fedex Ground Manual
         * Format: XXXXXXX XXXXXXX XXXXXXX
         * Replace MJ (Placeholder)
         */
        ScanFromFedEXGManual = Number.substring(0, 9) + " " + Number.substring(9, 16) + " " + Number.substring(16, 23);
        Replace = ScanFromFedEXGManual.replace("MJ", "");
        return Replace;
    }

    private String FedEXGO(String Number) {
        /**
         * 22 Characters
         * Fedex Ground Old
         * Format: XXXXXXX XXXXXXX XXXXXXX
         * Replace MJ (Placeholder)
         */
        ScanFromFedEXG = Number.substring(0, 9) + " " + Number.substring(9, 16) + " " + Number.substring(16, 24);
        Replace = ScanFromFedEXG.replace("MJ", "");
        return Replace;
    }

    private String FedEXE(String Number) {
        /**
         * 34 Characters
         * Fedex Express
         * Format: XXXX XXXX XXXX
         */
        ScanFromFedEX = Number.substring(0, 4) + " " + Number.substring(4, 8) + " " + Number.substring(8, 12);
        return ScanFromFedEX;
    }

    private String ManualFedEXE(String Number) {
        /**
         * 14 Characters
         * Fedex Express Manual
         * Format: XXXX XXXX XXXX
         */
        ScanFromFedEXManual = Number.substring(0, 4) + "" + Number.substring(4, 9) + "" + Number.substring(9, 14);
        return ScanFromFedEXManual;
    }

    private String UPS(String Number) {
        /**
         * 18 Characters
         * UPS
         * Format: XX XXX XXX XX XXXX XXXX
         */
        ScanFromUPS = Number.substring(0, 2) + " " + Number.substring(2, 5) + " " + Number.substring(5, 8) + " " + Number.substring(8, 10) + " " + Number.substring(10, 14) + " " + Number.substring(14, 18);
        return ScanFromUPS;
    }

    private String ManualUPS(String Number) {
        /**
         * 23 Characters
         * UPS Manual
         * Format: XX XXX XXX XX XXXX XXXX
         */
        ScanFromManualUPS = Number.substring(0, 2) + " " + Number.substring(3, 6) + " " + Number.substring(7, 10) + " " + Number.substring(11, 13) + " " + Number.substring(14, 18) + " " + Number.substring(19, 23);
        return ScanFromManualUPS;
    }

    private void CreateListView() {
        ListView listView;
        listView = (ListView) findViewById(R.id.listView);

        final SQLite db = new SQLite(this);
        Cursor cursor = db.getBarcodesRaw();

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.two_line_list_item,
                cursor,
                new String[]{"Barcode", "Company"},
                new int[]{android.R.id.text1, android.R.id.text2},
                0);

        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        listView.setAdapter(adapter);

        db.close();
    }

    private void loadPreferences(String L, String D) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings != null && L.equals("Y")) {
            size = settings.getInt("array_size", 0);
            log = new String[size];

            for (int i = 0; i < size; i++) {
                log[i] = settings.getString("array_" + i, null);
            }
        }
        if (settings != null && D.equals("Y")) {
            String value = settings.getString("counter", null);
            CounterTxt = (TextView) findViewById(R.id.textView2);
            CounterTxt.setText(value);
            Counter = Integer.parseInt(value);
        }
    }

    private void LoadData() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings != null && settings.contains("paused")) {
            SharedPreferences.Editor SharedEditor = settings.edit();
            loadPreferences("Y", "N");
            loadPreferences("N", "Y");
            SharedEditor.remove("paused");
            SharedEditor.apply();
            Log.i(TAG, "Loaded");
        } else {
            CounterTxt = (TextView) findViewById(R.id.textView2);
            CounterTxt.setText("0");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveLog();
        savePreferences();
    }

    //Todo: Save State
    private void savePreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor SharedEditor = settings.edit();

        String value = CounterTxt.getText().toString();
        String value2 = "Y";

        SharedEditor.putInt("array_size", log.length);
        SharedEditor.putString("counter", value);
        SharedEditor.putString("paused", value2);
        SharedEditor.apply();

    }

    private void saveLog() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor SharedEditor = settings.edit();

        for (int i = 0; i < log.length; i++) {
            SharedEditor.putString("array_" + i, log[i]);
            SharedEditor.apply();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            final String scanContent = scanningResult.getContents();

            LayoutInflater Results = LayoutInflater.from(this);

            @SuppressLint("InflateParams") final View textEntryView = Results.inflate(R.layout.scan_entry, null);
            final EditText scanData = (EditText) textEntryView.findViewById(R.id.scanData);

            final SQLite db = new SQLite(this);
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setIcon(R.drawable.perm_group_user_dictionary).setTitle("Information").setView(textEntryView).setPositiveButton("Save",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {

                            Editable value = scanData.getText();
                            String Result = Filter(scanContent);

                            db.addBarcodes(new Barcodes(Result, value.toString()));
                            Counter++;
                            db.close();
                            CreateListView();
                            //Todo: Fix Boxing
                            String setText = Integer.valueOf(Counter).toString();
                            CounterTxt.setText(setText);
                        }
                    }).setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            ScanDataEmpty();
                        }
                    });
            alert.show();
        } else if (resultCode == RESULT_CANCELED) {
            ScanDataEmpty();
        }
    }

    private class ScanButton implements View.OnClickListener {
        public void onClick(View v) {
            IntentIntegrator scanIntegrator = new IntentIntegrator(MainActivity.this);
            scanIntegrator.initiateScan();
        }
    }

    private class ManButton implements View.OnClickListener {
        public void onClick(View v) {
            InputManual();
        }
    }

    private class ClearButton implements View.OnClickListener {
        public void onClick(View v) {
            if (Counter == 0) {
                ScanDataEmpty();
            } else {
                emailResults();
                ClearButtonData();
            }
        }
    }
}