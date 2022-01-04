package com.reigues.corp.imc;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> expandableListTitle;
    private HashMap<String, List<String[]>> expandableListDetail;
    private int addButtonPlacement;

    private String addGroup = null;

    private customButtonListener customListener;

    CustomExpandableListAdapter(Context context, List<String> expandableListTitle,
                                HashMap<String, List<String[]>> expandableListDetail, int addButtonPlacement, customButtonListener listener) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.addButtonPlacement = addButtonPlacement;
        this.customListener = listener;
    }

    void setAddButtonPlacement(int addButtonPlacement) {
        this.addButtonPlacement = addButtonPlacement;
    }

    void setAddGroup(String addGroup) {
        this.addGroup = addGroup;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return new String[]{Objects.requireNonNull(this.expandableListDetail.get(this.expandableListTitle.get(listPosition)))
                .get(expandedListPosition)[0], Objects.requireNonNull(this.expandableListDetail.get(this.expandableListTitle.get(listPosition)))
                .get(expandedListPosition)[1]};
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(final int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (Objects.requireNonNull(this.expandableListDetail.get(this.expandableListTitle.get(listPosition))).size() == 0) {
            if (layoutInflater != null) {
                convertView = layoutInflater.inflate(R.layout.list_view_empty, parent, false);
            }
            convertView.setTag("Empty");
        } else {
            final String expandedListTextImc = ((String[]) getChild(listPosition, expandedListPosition))[0];
            final String expandedListTextdate = ((String[]) getChild(listPosition, expandedListPosition))[1];
            if (convertView == null || convertView.getTag() == "Empty") {
                if (layoutInflater != null) {
                    convertView = layoutInflater.inflate(R.layout.list_view_item, parent,false);
                }
                if (convertView != null) {
                    convertView.setTag(null);
                }

            }
            TextView expandedListTextViewImc = null;
            TextView expandedListTextViewDate = null;
            if (convertView != null) {
                expandedListTextViewImc = convertView.findViewById(R.id.expandedListItemImc);
                expandedListTextViewDate = convertView.findViewById(R.id.expandedListItemDate);
            }
            float result = Float.parseFloat(expandedListTextImc);
            String inter = "";
            if (result <= 16.5) {
                inter = context.getString(R.string.label_result_interpretation_0);
            } else if (result > 16.5 && result <= 18.5) {
                inter = context.getString(R.string.label_result_interpretation_1);
            } else if (result > 18.5 && result <= 25) {
                inter = context.getString(R.string.label_result_interpretation_2);
            } else if (result > 25 && result <= 30) {
                inter = context.getString(R.string.label_result_interpretation_3);
            } else if (result > 30 && result <= 35) {
                inter = context.getString(R.string.label_result_interpretation_4);
            } else if (result > 35 && result <= 40) {
                inter = context.getString(R.string.label_result_interpretation_5);
            } else if (result > 40) {
                inter = context.getString(R.string.label_result_interpretation_6);
            }
            if (expandedListTextViewImc != null) {
                expandedListTextViewImc.setText(context.getString(R.string.label_BMI_text,expandedListTextImc,inter));
            }
            try {
                if (expandedListTextViewDate != null) {
                    expandedListTextViewDate.setText(context.getResources().getString(R.string.label_in, new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
                            Objects.requireNonNull(new SimpleDateFormat("MM/yyyy",Locale.getDefault()).parse(expandedListTextdate)))));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            ImageButton expandedListItemDeleteButton;
            if (convertView != null) {
                expandedListItemDeleteButton = convertView.findViewById(R.id.expandedListItemDeleteButton);
                final View finalConvertView = convertView;
                expandedListItemDeleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (customListener != null) {
                            customListener.onButtonClickListener("Child", (String) getGroup(listPosition), finalConvertView, expandedListTextdate);
                        }
                    }
                });
            }

        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        if (listPosition == expandableListTitle.size()){
            return 0;
        }
        int size = Objects.requireNonNull(this.expandableListDetail.get(this.expandableListTitle.get(listPosition))).size();
        if (size == 0) {
            return 1;
        }
        return size;
    }

    @Override
    public Object getGroup(int listPosition) {
        return this.expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount() {
        return addButtonPlacement == 0 ? this.expandableListTitle.size() + 1 : this.expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (listPosition == getGroupCount() - 1 && addButtonPlacement == 0) // last element is a Button
        {
            if (convertView == null || convertView.getTag() != "Last") {
                if (layoutInflater != null) {
                    convertView = layoutInflater.inflate(R.layout.list_view_last_item, parent, false);
                    convertView.setTag("Last");
                    Button addPeopleButton = convertView.findViewById(R.id.AddPeopleBtn);

                    addPeopleButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (customListener != null) {
                                customListener.onButtonClickListener("Add", null, null, null);
                            }
                        }
                    });
                }
            }
        }
        /*else if (getGroup(listPosition).equals("Moi")){

            if (convertView == null || convertView.getTag()!="Moi") {
                convertView = layoutInflater.inflate(R.layout.list_view_group_me, null);
                convertView.setTag("Moi");
            }
            final String listTitle = (String) getGroup(listPosition);
            TextView listTitleTextView = (TextView) convertView.findViewById(R.id.listTitleMe);
            listTitleTextView.setTypeface(null, Typeface.BOLD);
            listTitleTextView.setText(listTitle);

        }*/
        else {
            if (convertView == null || convertView.getTag() != null) {
                if (layoutInflater != null) {
                    convertView = layoutInflater.inflate(R.layout.list_view_group, parent, false);
                    convertView.setTag(null);
                }
            }
            final String listTitle = (String) getGroup(listPosition);
            if (convertView != null) {
                TextView listTitleTextView = convertView.findViewById(R.id.listTitle);
                listTitleTextView.setTypeface(null, Typeface.BOLD);
                listTitleTextView.setText(listTitle);

                ImageButton deletePeopleButton = convertView.findViewById(R.id.listTitleDeleteButton);
                deletePeopleButton.setFocusable(false);
                final View finalConvertView = convertView;
                deletePeopleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (customListener != null) {
                            customListener.onButtonClickListener("Delete", listTitle, finalConvertView, null);
                        }
                    }
                });

                ImageButton editPeopleButton = convertView.findViewById(R.id.listTitleEditButton);
                editPeopleButton.setFocusable(false);
                editPeopleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (customListener != null) {
                            customListener.onButtonClickListener("Edit", listTitle, null, null);
                        }
                    }
                });
                if (listTitle.equals(addGroup)) {
                    convertView.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in));
                    addGroup = null;
                }
            }

        }
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }

    public interface customButtonListener {
        void onButtonClickListener(String action, String nom, View view, String date);
    }

}