package com.reigues.corp.imc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SettingsActivityOLD extends AppCompatPreferenceActivity {
    private static final String TAG = SettingsActivityOLD.class.getSimpleName();

    static Context context;
    private static AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = this;

        View dialogSendFeedback = ((LayoutInflater) Objects.requireNonNull(getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(R.layout.dialog_send_feedback, null);
        final ProgressBar progressBar = dialogSendFeedback.findViewById(R.id.progressBar);

        final EditText EditFeedback = dialogSendFeedback.findViewById(R.id.edit_feedback);



        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("upload result","start");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                        EditFeedback.setEnabled(false);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                });

                FTPClient ftp = null;

                try
                {
                    ftp = new FTPClient();
                    ftp.connect("files.000webhost.com");

                    if (ftp.login("appfilesr", "re22my01"))
                    {
                        ftp.enterLocalPassiveMode(); // important!
                        ftp.setFileType(FTP.BINARY_FILE_TYPE);

                        FTPFile[] files = ftp.listFiles("/public_html/Imc_Feedback");

                        List<Integer> filesName = new ArrayList<>();
                        for (FTPFile file : files) {
                            if (!file.getName().equals(".")&&!file.getName().equals(".."))
                                filesName.add(Integer.parseInt(file.getName().split("_")[0]));
                        }

                        InputStream in = new ByteArrayInputStream(("Feedback :\n"+EditFeedback.getText().toString()+"\n\n----------------\nDevice OS: Android" +
                                "\nDevice OS version: " + Build.VERSION.RELEASE + "\nApp Version: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName +
                                "\nDevice Brand: " + Build.BRAND + "\nDevice Model: " + Build.MODEL + "\nDevice Manufacturer: " + Build.MANUFACTURER).getBytes(StandardCharsets.UTF_8));

                        String outFileName = "/public_html/Imc_Feedback/"+(Collections.max(filesName)+1)+"_("+Build.ID+").txt";
                        OutputStream ou = ftp.storeFileStream(outFileName);

                        byte[] buffer = new byte[1024];
                        int len;

                        while((len = in.read(buffer)) != -1&&!Thread.currentThread().isInterrupted()){
                            ou.write(buffer, 0, len);
                            ou.flush();
                        }
                        //boolean result = ftp.storeFile("/public_html/Imc_Feedback/"+(Collections.max(filesName)+1)+"_("+Build.ID+").txt", in);
                        ou.close();
                        in.close();

                        if (Thread.currentThread().isInterrupted()){
                            ftp.deleteFile(outFileName);
                            ftp.logout();
                            ftp.disconnect();
                            return;
                        }

                        Log.i("upload result", "succeeded");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SettingsActivityOLD.this,"Feedback sent",Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.GONE);
                                EditFeedback.setText(null);
                                EditFeedback.setEnabled(true);
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                dialog.cancel();
                            }
                        });

                        ftp.logout();
                        ftp.disconnect();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SettingsActivityOLD.this,"Feedback sending failed! Please Send it later",Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            EditFeedback.setText(null);
                            EditFeedback.setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            dialog.cancel();
                        }
                    });
                    Log.i("upload result", "failed");
                    try {
                        if (ftp != null) {
                            ftp.logout();
                            ftp.disconnect();
                        }
                    }catch (Exception e1){
                        e1.printStackTrace();
                    }
                }
            }
        });



        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.label_send_feedback)+" ?");
        builder.setView(dialogSendFeedback);
        builder.setPositiveButton(getString(R.string.label_send), null);
        builder.setNegativeButton(getString(R.string.label_cancel), null);
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogI) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {

                        if(TextUtils.isEmpty(EditFeedback.getText().toString())) {
                            EditFeedback.setError(context.getString(R.string.label_error,context.getString(R.string.label_feedback)));
                        }
                        else{
                            thread.start();
                        }
                    }

                });
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View view) {
                        if (thread.isAlive()){
                            thread.interrupt();
                            progressBar.setVisibility(View.GONE);
                            EditFeedback.setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(SettingsActivityOLD.this,"Sending canceled!",Toast.LENGTH_LONG).show();
                        }
                        dialog.cancel();
                        EditFeedback.setText(null);


                    }
                });
            }
        });

        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);




            // gallery EditText change listener
            bindPreferenceSummaryToValue(findPreference("key_default_weight_unit"));
            bindPreferenceSummaryToValue(findPreference("key_default_height_unit"));
            bindPreferenceSummaryToValue(findPreference("key_add_button_placement"));
            bindPreferenceSummaryToValue(findPreference("key_language"));

            // notification preference change listener
            //bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_notification)));

            // feedback preference click listener
            Preference myPref = findPreference("key_send_feedback");
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    sendFeedback();
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
            final String stringValue = newValue.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                if (index>=0){
                    CharSequence entry = listPreference.getEntries()[index];
                    String title = null;

                    switch (preference.getKey()) {
                        case "key_default_weight_unit":
                             title = context.getString(R.string.pref_title_default_weight_unit);
                            break;
                        case "key_default_height_unit":
                            title = context.getString(R.string.pref_title_default_height_unit);
                            break;
                        case "key_add_button_placement":
                            title = context.getString(R.string.pref_title_add_button_placement);
                            break;
                        case "key_language":
                            title = context.getString(R.string.pref_title_language);
                            break;
                    }

                    preference.setTitle(title+" : "+entry);
                    if (preference.getKey().equals("key_language")){
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

                        String languageToLoad  = sharedPref.getString("key_language","en_En");
                        Locale locale = new Locale(languageToLoad);
                        Locale.setDefault(locale);
                        Configuration config = new Configuration();
                        config.locale = locale;
                        context.getResources().updateConfiguration(config,context.getResources().getDisplayMetrics());

                        //Intent intent = getIntent();
                        //finish();
                        //context.startActivity(intent);
                        //Intent intent = new Intent(SettingsActivityOLD.this, MainActivity.class);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //context.startActivity(intent);
                    }

                }
            }/* else {
                preference.setSummary(stringValue);
            }*/
            return true;
        }
    };

    /**
     * Email client intent to send support mail
     * Appends the necessary device information to email body
     * useful when providing support
     */
    public static void sendFeedback() {

        dialog.show();

        /*String body = null;
        try {
            body = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS: Android \n Device OS version: " +
                    Build.VERSION.RELEASE + "\n App Version: " + body + "\n Device Brand: " + Build.BRAND +
                    "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER+Build.ID;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"contact@androidhive.info"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Query from android app");
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_email_client)));*/
    }
}