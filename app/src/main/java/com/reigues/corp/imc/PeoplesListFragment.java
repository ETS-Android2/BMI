package com.reigues.corp.imc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PeoplesListFragment extends Fragment implements CustomExpandableListAdapter.customButtonListener {

    //private static final String KEY_FILE = "file";
    private ExpandableListView expandableListView;
    private CustomExpandableListAdapter expandableListAdapter;
    private List<String> expandableListTitle;
    private HashMap<String, List<String[]>> expandableListDetail;

    private Document fileText;

    private View LastItemListView;
    private View EditGroupListView;

    private Animation animationD;

    private AlertDialog dialog;
    private AlertDialog dialogE;

    private LayoutInflater inflater;
    private FloatingActionButton fab;
    private int addButtonPlacement;

    static PeoplesListFragment newInstance() {
        return (new PeoplesListFragment());
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.inflater = inflater;

        View layout = inflater.inflate(R.layout.fragment_peoples_list, container, false);

        LastItemListView = inflater.inflate(R.layout.list_view_dialog_add_people, container, false);
        EditGroupListView = inflater.inflate(R.layout.list_view_dialog_edit_people, container, false);

        if (savedInstanceState != null) {
            fileText = (Document) savedInstanceState.getSerializable("fileText");
        }else {

            fileText = getFileText();

        }

        fab = layout.findViewById(R.id.fab);

        addButtonPlacement =Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("key_add_button_placement", "0"));
        if (addButtonPlacement==1){
            fab.show();
        }else {
            fab.hide();
        }

        createDialog();
        createEditDialog();

        expandableListView = layout.findViewById(R.id.expandableListView);

        setDataInList();

        expandableListAdapter = new CustomExpandableListAdapter(getActivity(), expandableListTitle, expandableListDetail,addButtonPlacement,this);

        expandableListView.setAdapter(expandableListAdapter);

        animationD = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out);
        animationD.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                refreshData();
                expandableListAdapter.notifyDataSetChanged();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        fileText = getFileText();
        refreshData();

        int count =  expandableListAdapter.getGroupCount();
        for (int i = 0; i <count ; i++)
            expandableListView.collapseGroup(i);

        addButtonPlacement = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("key_add_button_placement", "0"));
        if (addButtonPlacement==1){
            fab.show();
        }else {
            fab.hide();
        }
        expandableListAdapter.setAddButtonPlacement(addButtonPlacement);

        expandableListAdapter.notifyDataSetChanged();

    }

    private void createDialog(){

        final EditText addEditNom = LastItemListView.findViewById(R.id.addEditNom);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.label_add)+" ?");
        builder.setMessage(getString(R.string.label_add_text));
        builder.setView(LastItemListView);
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.label_add), null);
        builder.setNegativeButton(getString(R.string.label_cancel), null);
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogI) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {

                        if(TextUtils.isEmpty(addEditNom.getText().toString())) {
                            addEditNom.setError(getString(R.string.label_error,getString(R.string.label_name)));
                        }
                        else{
                            Element racine = fileText.getRootElement();

                            Element nom = new Element("nom");
                            racine.addContent(nom);
                            nom.setAttribute("nom",addEditNom.getText().toString());

                            expandableListAdapter.setAddGroup(addEditNom.getText().toString());
                            refreshData();
                            //expandableListAdapter.notifyDataSetChanged();

                            writeFile(fileText);

                            addEditNom.setText("");
                            addEditNom.setError(null);

                            dialog.cancel();
                        }
                    }
                });
            }
        });

        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    }

    private void createEditDialog(){
        dialogE = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.label_edit)+" ?")
                .setView(EditGroupListView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.label_edit), null)
                .setNegativeButton(getString(R.string.label_cancel), null)
                .create();
    }

    private void setDataInList() {

        expandableListTitle = new ArrayList<>();
        expandableListDetail = new HashMap<>();

        Element racine = fileText.getRootElement();

        for (Element Enom : racine.getChildren()){

            List<String[]> Lnom = new ArrayList<>();

            for (Element date : Enom.getChildren()){

                Lnom.add(new String[]{date.getChild("IMC").getText(),date.getAttributeValue("date")});

            }

            expandableListDetail.put(Enom.getAttributeValue("nom"),Lnom);
            expandableListTitle.add(Enom.getAttributeValue("nom"));

        }
    }

    private void refreshData(){
        expandableListDetail.clear();
        expandableListTitle.clear();

        Element racine = fileText.getRootElement();

        for (Element Enom : racine.getChildren()){

            List<String[]> Lnom = new ArrayList<>();

            for (Element date : Enom.getChildren()){

                Lnom.add(new String[]{date.getChild("IMC").getText(),date.getAttributeValue("date")});

            }

            expandableListDetail.put(Enom.getAttributeValue("nom"),Lnom);
            expandableListTitle.add(Enom.getAttributeValue("nom"));

        }
    }

    private void writeFile(Document fileText) {
        try {

            XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
            sortie.output(fileText, new FileOutputStream(new File(Objects.requireNonNull(getActivity()).getFilesDir(), getString(R.string.data_file_name))));

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private Document getFileText() {

        File imcFile = new File(Objects.requireNonNull(getActivity()).getFilesDir(), getString(R.string.data_file_name));

        SAXBuilder sxb = new SAXBuilder();
        org.jdom2.Document document = null;
        try
        {
            document = sxb.build(imcFile);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return document;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("fileText",fileText);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onButtonClickListener(String action, final String nom, final View view, String date) {
        switch(action) {

            case "Child":
                View title = inflater.inflate(R.layout.list_view_dialog_imc_title, null);

                Element racine = fileText.getRootElement();

                Element eNom = null;

                for (Element e : racine.getChildren()){
                    if (e.getAttributeValue("nom").equals(nom)){
                        eNom = e;
                    }
                }

                Element eDate = null;

                if (eNom != null) {
                    for (Element e : eNom.getChildren()){
                        if (e.getAttributeValue("date").equals(date)){
                            eDate = e;
                        }
                    }
                }

                TextView Tvtitle = title.findViewById(R.id.titleText);
                try {
                    if (eDate != null) {
                        Tvtitle.setText(getString(R.string.label_in_middle,eNom.getAttributeValue("nom"),new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
                                Objects.requireNonNull(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).parse(eDate.getAttributeValue("date"))))));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                float result = 0;
                if (eDate != null) {
                    result = Float.parseFloat(eDate.getChild("IMC").getText());
                }
                String inter = "";
                if (result<=16.5){
                    inter = getString(R.string.label_result_interpretation_0);
                } else if (result>16.5&&result<=18.5){
                    inter = getString(R.string.label_result_interpretation_1);
                }else if (result>18.5&&result<=25){
                    inter = getString(R.string.label_result_interpretation_2);
                }else if (result>25&&result<=30){
                    inter = getString(R.string.label_result_interpretation_3);
                }else if (result>30&&result<=35){
                    inter = getString(R.string.label_result_interpretation_4);
                }else if (result>35&&result<=40){
                    inter = getString(R.string.label_result_interpretation_5);
                }else if (result>40){
                    inter = getString(R.string.label_result_interpretation_6);
                }

                final AlertDialog viewDialog;
                if (eDate != null) {
                    viewDialog = new AlertDialog.Builder(getActivity()).setCustomTitle(title)
                            .setMessage(getString(R.string.label_BMI)+" : "+ eDate.getChild("IMC").getText()+" ("+inter+")"+
                                    "\n"+getString(R.string.label_height_dot)+" "+eDate.getChild("taille").getText()+
                                    "\n"+getString(R.string.label_weight_dot)+" "+eDate.getChild("poids").getText())
                            .setPositiveButton("Ok", null).create();

                    viewDialog.show();

                    ImageButton titleDeleteButton = title.findViewById(R.id.titleDeleteButton);
                    final Element finalENom = eNom;
                    final Element finalEDate = eDate;
                    final String finalInter = inter;
                    titleDeleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.label_delete_text))
                                    .setMessage(getString(R.string.label_BMI)+" : "+ finalEDate.getChild("IMC").getText()+" ("+ finalInter +")"+
                                            "\n"+getString(R.string.label_height_dot)+" "+finalEDate.getChild("taille").getText()+
                                            "\n"+getString(R.string.label_weight_dot)+" "+finalEDate.getChild("poids").getText())
                                    .setNegativeButton(getString(R.string.label_cancel), null)
                                    .setPositiveButton(getString(R.string.label_delete), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finalENom.removeContent(finalEDate);
                                            viewDialog.dismiss();
                                            writeFile(fileText);
                                            view.startAnimation(animationD);
                                        }
                                    }).show();
                        }
                    });
                }
                break;

            case "Add":

                dialog.show();

                break;

            case "Delete":
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.label_delete)+"?")
                        .setMessage(getString(R.string.label_delete_text_people, nom))
                        .setPositiveButton(getString(R.string.label_delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Element racine = fileText.getRootElement();

                                Element Enom = null;

                                for (Element e : racine.getChildren()){

                                    if (Objects.equals(e.getAttributeValue("nom"), nom)){
                                        Enom = e;
                                    }

                                }

                                racine.removeContent(Enom);

                                refreshData();

                                writeFile(fileText);
                                view.startAnimation(animationD);
                            }
                        })
                        .setNegativeButton(getString(R.string.label_cancel), null)
                        .create().show();

                break;

            case "Edit":

                final EditText editEditNom = EditGroupListView.findViewById(R.id.editEditNom);

                editEditNom.setText(nom);
                editEditNom.setSelection(nom.length());

                dialogE.setMessage(getString(R.string.label_edit_text,nom));

                dialogE.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogI) {
                        dialogE.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v) {

                                if(TextUtils.isEmpty(editEditNom.getText().toString())) {
                                    editEditNom.setError(getString(R.string.label_error));
                                }
                                else{
                                    Element racine = fileText.getRootElement();

                                    Element Enom = null;

                                    for (Element e : racine.getChildren("nom")) {

                                        if (e.getAttributeValue("nom").equals(nom)){
                                            Enom = e;
                                        }

                                    }
                                    if (Enom != null) {
                                        Enom.setAttribute("nom",editEditNom.getText().toString());
                                    }

                                    refreshData();
                                    //expandableListAdapter.notifyDataSetChanged();

                                    writeFile(fileText);

                                    editEditNom.setText("");
                                    editEditNom.setError(null);

                                    dialogE.cancel();
                                }
                            }
                        });
                    }
                });

                Objects.requireNonNull(dialogE.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                dialogE.show();

                break;

        }
    }
}
