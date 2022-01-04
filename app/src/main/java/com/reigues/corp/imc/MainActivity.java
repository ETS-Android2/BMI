package com.reigues.corp.imc;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.navigation.NavigationView;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //FOR DATAS
    // 2 - Identify each fragment with a number
    private static final int FRAGMENT_CALCULATOR = 0;
    private static final int FRAGMENT_PEOPLES_LIST = 1;
    private static final int FRAGMENT_INFO = 2;
    //FOR FRAGMENTS
    // 1 - Declare fragment handled by Navigation Drawer
    private Fragment fragmentCalculator;
    private Fragment fragmentPeoplesList;
    private Fragment fragmentInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String languageToLoad = sharedPref.getString("key_language", "-1");
        if(!languageToLoad.equals("-1")){
            Log.e("language",languageToLoad);
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getResources().updateConfiguration(config,getResources().getDisplayMetrics());
        }*/

        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                View focusedView = getCurrentFocus();
                if (focusedView != null) {
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        File imcFile = new File(getFilesDir(), getString(R.string.data_file_name));

        if (!imcFile.exists()) {

            try {
                Element racine = new Element("personnes");
                org.jdom2.Document document = new Document(racine);

                /*Element Enom = new Element("nom");
                racine.addContent(Enom);
                Enom.setAttribute("nom","Moi");*/

                XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
                sortie.output(document, new FileOutputStream(imcFile));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Document fileText = getFileText();

        Element racine = fileText.getRootElement();

        for (Element e : racine.getChildren("nom")) {

            for (Element ed : e.getChildren("date")) {
                try {
                    new SimpleDateFormat("MM/yyyy", Locale.getDefault()).parse(ed.getAttributeValue("date"));

                } catch (ParseException e1) {
                    Log.e("Incorect date", e.getAttributeValue("nom"));
                    Log.e("Incorect date", "transformation...");

                    try {
                        Date date = new SimpleDateFormat("yyyy", Locale.getDefault()).parse(ed.getAttributeValue("date"));
                        if (date != null) {
                            ed.setAttribute("date", new SimpleDateFormat("MM/yyyy", Locale.FRENCH).format(date));
                        }
                    } catch (ParseException e2) {
                        Log.e("Incorect date", "no french");
                    }

                }
            }

        }

        try {

            XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
            sortie.output(fileText, new FileOutputStream(new File(getFilesDir(), getString(R.string.data_file_name))));

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

        Fragment visibleFragment = getSupportFragmentManager().findFragmentById(R.id.activity_main_frame_layout);
        if (visibleFragment == null) {

            // 1.1 - Show News Fragment
            this.showFragment(FRAGMENT_CALCULATOR);

            // 1.2 - Mark as selected the menu item corresponding to NewsFragment
            navigationView.getMenu().getItem(0).setChecked(true);
        }

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String languageToLoad = sharedPref.getString("key_language", "-1");
        //String languageToLoad = "en";
        if (!languageToLoad.equals("-1")) {
            //String language = SharedPrefUtils.getSavedLanguage(); // Helper method to get saved language from SharedPreferences
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return updateResourcesLocale(context, locale);
            }

            return updateResourcesLocaleLegacy(context, locale);

        }

        return context;

    }

    @TargetApi(Build.VERSION_CODES.N)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    public Document getFileText() {

        File imcFile = new File(getFilesDir(), getString(R.string.data_file_name));

        SAXBuilder sxb = new SAXBuilder();
        org.jdom2.Document document = null;
        try {
            document = sxb.build(imcFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }

    private void exit() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.label_quit))
                .setMessage(getString(R.string.label_quit_text))
                .setPositiveButton(getString(R.string.label_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }

                })
                .setNegativeButton(getString(R.string.label_no), null)
                .show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            exit();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_calculator) {
            showFragment(FRAGMENT_CALCULATOR);
            Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.title_fragment_imc_calculator);
        } else if (id == R.id.nav_peoples_list) {
            showFragment(FRAGMENT_PEOPLES_LIST);
            Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.title_fragment_peoples_list);
        } else if (id == R.id.nav_info) {

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_exit) {
            exit();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //show fragment

    private void showFragment(int fragmentIdentifier) {


        switch (fragmentIdentifier) {
            case FRAGMENT_CALCULATOR:
                if (this.fragmentCalculator == null)
                    this.fragmentCalculator = CalculatorFragment.newInstance();
                this.startTransactionFragment(this.fragmentCalculator);
                break;
            case FRAGMENT_PEOPLES_LIST:
                if (this.fragmentPeoplesList == null)
                    this.fragmentPeoplesList = PeoplesListFragment.newInstance();
                this.startTransactionFragment(this.fragmentPeoplesList);
                break;
            case FRAGMENT_INFO:
                /*if (this.fragmentInfo == null) this.fragmentInfo = ProfileFragment.newInstance();
                this.startTransactionFragment(this.fragmentInfo);*/
                break;
            default:
                break;
        }
    }

    // 3 - Generic method that will replace and show a fragment inside the MainActivity Frame Layout
    private void startTransactionFragment(Fragment fragment) {
        if (!fragment.isVisible()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_main_frame_layout, fragment).commit();
        }
    }
}
