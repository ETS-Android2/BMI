package com.reigues.corp.imc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.fragment.app.Fragment;

import static android.graphics.ColorSpace.Model.XYZ;

public class CalculatorFragment extends Fragment implements View.OnClickListener, TextWatcher, AdapterView.OnItemSelectedListener {

    //private static final String KEY_FILE = "file";
    private EditText editPoids;
    private EditText editTaille;
    private EditText editTailleInches;
    private TextView texteResultat;
    private FloatingActionButton fab;
    private Spinner choixNom;
    private Spinner choixMois;
    private Spinner choixAnnee;
    private Document fileText;
    private float Poids;
    private float Taille;
    private float IMC;
    private LayoutInflater inflater;
    private AlertDialog dialog;
    private AlertDialog dialogNew;
    private CharSequence defaultResult;
    private CharSequence nom = null;
    private String date;
    private List<String> Lnom;
    private ArrayAdapter<String> nomAdapter;

    private int weightUnit;
    private int heightUnit;

    static CalculatorFragment newInstance() {
        return (new CalculatorFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.inflater = inflater;

        View layout = inflater.inflate(R.layout.fragment_imc_calculator, container, false);

        fileText = getFileText();

        dialogCreate(container);
        dialogNewCreate(container);

        defaultResult = getString(R.string.label_result_text_0);

        Button buttonIMC = layout.findViewById(R.id.buttonIMC);
        Button buttonRAZ = layout.findViewById(R.id.buttonRAZ);

        buttonIMC.setOnClickListener(this);
        buttonRAZ.setOnClickListener(this);

        editPoids = layout.findViewById(R.id.editPoids);
        editTaille = layout.findViewById(R.id.editTaille);
        editTailleInches = layout.findViewById(R.id.editTailleInches);

        editPoids.addTextChangedListener(this);
        editTaille.addTextChangedListener(this);
        editTailleInches.addTextChangedListener(this);

        texteResultat = layout.findViewById(R.id.texteResultat);

        fab = layout.findViewById(R.id.fab);
        fab.hide();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                if (nomAdapter.getCount() == 1) {
                    dialogNew.show();
                }
            }
        });

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        fileText = getFileText();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        weightUnit = Integer.parseInt(sharedPref.getString("key_default_weight_unit", "0"));
        heightUnit = Integer.parseInt(sharedPref.getString("key_default_height_unit", "0"));

        editPoids.setHint(getString(R.string.label_weight) + " (" + getString(R.string.label_in, getResources().getStringArray(R.array.pref_default_weight_unit_entries)[weightUnit]) + ")");
        editTaille.setHint(getString(R.string.label_height) + " (" + getString(R.string.label_in, getResources().getStringArray(R.array.pref_default_height_unit_entries)[heightUnit]) + ")");

        if (heightUnit == 2) {
            editTailleInches.setVisibility(View.VISIBLE);
        } else {
            editTailleInches.setVisibility(View.GONE);
        }
    }

    private void dialogCreate(ViewGroup container) {

        View layout = inflater.inflate(R.layout.dialog_save, container, false);

        choixNom = layout.findViewById(R.id.spinnerNom); // mettre layout car objet fais pas partie de activité principale
        choixMois = layout.findViewById(R.id.spinnerDateMois); // pareil
        choixAnnee = layout.findViewById(R.id.spinnerDateAnnee); // pareil

        choixNom.setOnItemSelectedListener(this);

        List<String> mois = new ArrayList<>(Arrays.asList(new DateFormatSymbols(Locale.getDefault()).getMonths()));

        List<String> annee = new ArrayList<>();

        int year = Calendar.getInstance().get(Calendar.YEAR);

        for (int d = year; d != year - 100; d--) {

            annee.add(String.valueOf(d));

        }
        Element racine = fileText.getRootElement();

        Lnom = new ArrayList<>();

        for (Element e : racine.getChildren()) {

            Lnom.add(e.getAttributeValue("nom"));

        }

        Lnom.add(getString(R.string.label_new_people));

        ArrayAdapter<String> moisAdapter = new ArrayAdapter<>(Objects.requireNonNull(getActivity()), android.R.layout.simple_spinner_item, mois);
        moisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> anneeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, annee);
        anneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        nomAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Lnom);
        nomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        choixMois.setAdapter(moisAdapter);
        choixAnnee.setAdapter(anneeAdapter);
        choixNom.setAdapter(nomAdapter);

        choixNom.setSelection(0);
        choixMois.setSelection(0);
        choixAnnee.setSelection(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.label_save) + "?");
        builder.setMessage(getString(R.string.label_save_text));
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.label_save), null);
        builder.setNegativeButton(getString(R.string.label_cancel), null);
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        nom = choixNom.getSelectedItem().toString();

                        date = (choixMois.getSelectedItemPosition() + 1) + "/" + choixAnnee.getSelectedItem().toString();

                        //date = choixMois.getSelectedItem().toString() + " " + choixAnnee.getSelectedItem().toString();

                        Element racine = fileText.getRootElement();

                        Element eNom = null;

                        for (Element e : racine.getChildren("nom")) {

                            if (e.getAttributeValue("nom").contentEquals(nom)) {
                                eNom = e;
                            }

                        }

                        Element eDate = new Element("date");
                        if (eNom != null) {
                            eNom.addContent(eDate);
                        }
                        eDate.setAttribute("date", date);

                        Element eIMC = new Element("IMC");
                        eIMC.setText(String.valueOf(IMC));
                        eDate.addContent(eIMC);

                        Element ePoids = new Element("poids");
                        ePoids.setText(String.valueOf(Poids));
                        eDate.addContent(ePoids);

                        Element eTaille = new Element("taille");
                        eTaille.setText(String.valueOf(Taille));
                        eDate.addContent(eTaille);

                        writeFile(fileText);

                        dialog.cancel(); // sert a quelque chose
                        fab.hide();
                    }
                });

            }
        });

    }

    private void dialogNewCreate(ViewGroup container) {
        View layout = inflater.inflate(R.layout.list_view_dialog_add_people, container, false);

        final EditText addEditNom = layout.findViewById(R.id.addEditNom);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.label_add) + "?");
        builder.setMessage(getString(R.string.label_add_text));
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.label_add), null);
        builder.setNegativeButton(getString(R.string.label_cancel), null);
        dialogNew = builder.create();
        dialogNew.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogI) {
                dialogNew.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        if (TextUtils.isEmpty(addEditNom.getText().toString())) {
                            addEditNom.setError(getString(R.string.label_error, getString(R.string.label_name)));
                        } else {
                            Element racine = fileText.getRootElement();

                            Element nom = new Element("nom");
                            racine.addContent(nom);
                            nom.setAttribute("nom", addEditNom.getText().toString());

                            writeFile(fileText);

                            Lnom.add(Lnom.size() - 1, addEditNom.getText().toString());
                            nomAdapter.notifyDataSetChanged();

                            addEditNom.setText("");
                            addEditNom.setError(null);

                            dialogNew.cancel();
                        }
                    }
                });
                dialogNew.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (nomAdapter.getCount() == 1)
                            dialog.cancel();
                        dialogNew.cancel();
                        choixNom.setSelection(nomAdapter.getCount() - 2);
                    }
                });
            }
        });

        Objects.requireNonNull(dialogNew.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    //android:checkedButton="@+id/radioBcm"
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.buttonIMC:

                try {

                    Poids = Float.parseFloat(editPoids.getText().toString());
                    Taille = Float.parseFloat(editTaille.getText().toString());

                    if (weightUnit == 1) {
                        Poids = (float) (Poids * 0.45359237);
                    }

                    if (Poids > 0) {

                        if (Taille > 0) {

                            if (heightUnit == 1) {

                                Taille = Taille / 100;

                            }

                            if (heightUnit == 2) {
                                float TailleInches = Float.parseFloat(editTailleInches.getText().toString());
                                Taille = (float) ((Taille + (TailleInches / 12)) * 0.3048);

                            }

                            fab.show();
                            IMC = Poids / (Taille * Taille);
                            texteResultat.setText(Result(IMC));

                            editPoids.setError(null);
                            editTaille.setError(null);
                        } else {
                            editTaille.setError(getString(R.string.label_error_0_h));
                        }
                    } else {

                        editPoids.setError(getString(R.string.label_error_0_w));

                    }
                } catch (NumberFormatException e) {
                    showMessage(getString(R.string.label_error_empty_label));
                }
                break;

            case R.id.buttonRAZ:

                fab.hide();
                editPoids.setText("");
                editTaille.setText("");
                texteResultat.setText(defaultResult);

                break;

        }

    }

    private CharSequence Result(Float result) {

        CharSequence text = getString(R.string.label_result_text_1, result);

        if (result <= 16.5) {
            text = text + getString(R.string.label_result_interpretation_0);
        } else if (result > 16.5 && result <= 18.5) {
            text = text + getString(R.string.label_result_interpretation_1);
        } else if (result > 18.5 && result <= 25) {
            text = text + getString(R.string.label_result_interpretation_2);
        } else if (result > 25 && result <= 30) {
            text = text + getString(R.string.label_result_interpretation_3);
        } else if (result > 30 && result <= 35) {
            text = text + getString(R.string.label_result_interpretation_4);
        } else if (result > 35 && result <= 40) {
            text = text + getString(R.string.label_result_interpretation_5);
        } else if (result > 40) {
            text = text + getString(R.string.label_result_interpretation_6);
        }

        return text;


    }

    private void writeFile(Document fileText) {
        try {

            XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
            sortie.output(fileText, new FileOutputStream(new File(Objects.requireNonNull(getActivity()).getFilesDir(), getString(R.string.data_file_name))));

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private Document getFileText() {

        File imcFile = new File(Objects.requireNonNull(getActivity()).getFilesDir(), getString(R.string.data_file_name));

        SAXBuilder sxb = new SAXBuilder();
        org.jdom2.Document document = null;
        try {
            document = sxb.build(imcFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }

    private void showMessage(String s) {

        Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), s, Toast.LENGTH_LONG).show();

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        texteResultat.setText(defaultResult);
        fab.hide();

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        if (pos == adapterView.getCount() - 1) {
            dialogNew.show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
