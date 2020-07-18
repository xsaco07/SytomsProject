package com.project.symptoms.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.project.symptoms.R;
import com.project.symptoms.db.controller.SelectedCategoryOptionController;
import com.project.symptoms.db.controller.SymptomCategoryController;
import com.project.symptoms.db.controller.SymptomCategoryOptionController;
import com.project.symptoms.db.controller.SymptomController;
import com.project.symptoms.db.model.SelectedCategoryOptionModel;
import com.project.symptoms.db.model.SymptomCategoryModel;
import com.project.symptoms.db.model.SymptomCategoryOptionModel;
import com.project.symptoms.db.model.SymptomModel;
import com.project.symptoms.fragment.MainMenuFragment;
import com.project.symptoms.util.DateTimeUtils;
import com.project.symptoms.view.BodyView;
import com.project.symptoms.view.SymptomOptionView;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SymptomForm extends AppCompatActivity implements MainMenuFragment.OnFragmentInteractionListener{

    private Button saveButton;
    private RadioGroup intensityRadioGroupView;
    private Switch intermittenceSwitchView;
    private EditText symptomDescriptionView, symptomMedicamentView, symptomFoodView;
    private TextView startDateView, startTimeView;
    private BodyView.Circle currentCircle;
    private String mainActivityDate, mainActivityTime;
    private int bodyState;
    private EditText symptomDurationView;
    private HashMap<String, SymptomOptionView> optionsViewHashMap;

    private SymptomController symptomController;
    private SelectedCategoryOptionController selectedCategoryOptionController;
    private SymptomCategoryOptionController symptomCategoryOptionController;

    private long symptomIdToUpdate;
    SymptomModel symptomModelToUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.symptom_form);

        optionsViewHashMap = new HashMap<>();
        setUpViews();
        loadSymptomCategories();
        setUpListeners();

        symptomController = SymptomController.getInstance(this);
        selectedCategoryOptionController = SelectedCategoryOptionController.getInstance(this);
        symptomCategoryOptionController = SymptomCategoryOptionController.getInstance(this);

        // If I receive an id that means I need to update that symptom and load the data into the form
        symptomIdToUpdate = getIntent().getLongExtra("symptom_id", -1);
        if (symptomIdToUpdate > 0) loadFormForUpdate();
        else loadDefaultForm();
    }

    private void setUpViews(){
        saveButton = findViewById(R.id.save_button);
        startDateView = findViewById(R.id.start_date);
        startTimeView = findViewById(R.id.start_time);
        intensityRadioGroupView = findViewById(R.id.symp_radio_group);
        intermittenceSwitchView = findViewById(R.id.intermittence_switch);
        symptomDescriptionView = findViewById(R.id.symp_description);
        symptomMedicamentView = findViewById(R.id.symp_medicament);
        symptomFoodView = findViewById(R.id.symp_food);
        symptomDurationView = findViewById(R.id.symp_duration);
        DateTimeUtils.getInstance().registerAsDatePicker(startDateView);
        DateTimeUtils.getInstance().registerAsTimePicker(startTimeView);
    }

    // Finish form set-up for inserting
    private void loadDefaultForm(){
        setUpBundleData();
        setStartDateTime(mainActivityDate, mainActivityTime);
    }

    // Finish form set-up for updating
    private void loadFormForUpdate(){
        saveButton.setText(R.string.update_button_text);
        symptomModelToUpdate = symptomController.findById(symptomIdToUpdate);
        symptomDescriptionView.setText(symptomModelToUpdate.getDescription());
        HashMap<String, Integer> intensityHashMap = new HashMap<>();
        intensityHashMap.put(getString(R.string.low), 0);
        intensityHashMap.put(getString(R.string.medium), 1);
        intensityHashMap.put(getString(R.string.high), 2);
        intermittenceSwitchView.setChecked(symptomModelToUpdate.getIntermittence() == 1);
        RadioButton intensityRadioButton = (RadioButton)intensityRadioGroupView.getChildAt(intensityHashMap.get(symptomModelToUpdate.getIntensity()));
        intensityRadioButton.setChecked(true);
        startDateView.setText(DateTimeUtils.getInstance().getStringDateFromLong(symptomModelToUpdate.getStartDate()));
        startTimeView.setText(DateTimeUtils.getInstance().getStringTimeFromLong(symptomModelToUpdate.getStartTime()));
        symptomDurationView.setText(String.format(Locale.getDefault(), "%d", symptomModelToUpdate.getDuration()));
        symptomMedicamentView.setText(symptomModelToUpdate.getCausingDrug());
        symptomFoodView.setText(symptomModelToUpdate.getCausingFood());
        enableCheckRespectiveSymptomOptionViews();
    }

    private void enableCheckRespectiveSymptomOptionViews(){
        for (SelectedCategoryOptionModel optionModel: selectedCategoryOptionController.getAllBySymptom(symptomIdToUpdate)) {
            String optionName = symptomCategoryOptionController.getById(optionModel.getCategoryOptionId()).getCategoryOptionName();
            SymptomOptionView currentSymptomOptionView = optionsViewHashMap.get(optionName);
            if (currentSymptomOptionView != null) currentSymptomOptionView.setChecked(true);
        }
    }

    private void setUpBundleData(){
        currentCircle = getIntent().getParcelableExtra("Circle");
        mainActivityDate = getIntent().getStringExtra("Date");
        mainActivityTime = getIntent().getStringExtra("Time");
        bodyState = getIntent().getIntExtra("State", -1);
    }

    private void setStartDateTime(String date, String time){
        startDateView.setText(date);

        // Not allow user to edit this because the date needs to be the same than MainActivity one
        // TODO: check this with client
        startDateView.setEnabled(false);
        startDateView.setClickable(false);

        startTimeView.setText(time);
    }

    private void setUpListeners(){
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = "";
                // The form will insert a new Symptom
                if(saveButton.getText().equals(getString(R.string.save_button_text))) {
                    text = insertSymptomsData() ?
                            getResources().getString(R.string.value_successfully_saved) :
                            getResources().getString(R.string.value_saving_failed);
                }
                // Thw form will update an existing symptom
                else {
                    try {
                        text = updateSymptomsData() ?
                                getResources().getString(R.string.value_successfully_saved) :
                                getResources().getString(R.string.value_saving_failed);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivityIntent);
            }
        });
    }

    private boolean insertSelectedCategoryOptions(long symptomId){
        boolean success = true;
        // Insert in selected category table
        for (Map.Entry entry: optionsViewHashMap.entrySet()) {
            SymptomOptionView currentSymptomOptionView = ((SymptomOptionView)entry.getValue());
            if(currentSymptomOptionView.isChecked()){
                Log.i("#", currentSymptomOptionView.getName());
                SymptomCategoryOptionModel currentCategoryOption = symptomCategoryOptionController.getSymptomCategoryOptionByName(currentSymptomOptionView.getName());
                success = selectedCategoryOptionController.insert(symptomId, currentCategoryOption.getCategoryOptionId()) != -1;
            }
        }
        return success;
    }

    private boolean insertSymptomsData() {
        logSelectedOptions();
        String stringDuration = symptomDurationView.getText().toString();
        int finalDuration = (!"".equals(stringDuration)) ? Integer.parseInt(stringDuration) : -1;
        int intensityCheckedViewId = intensityRadioGroupView.getCheckedRadioButtonId();
        RadioButton intensityRadioButton = findViewById(intensityCheckedViewId);
        // Insert in symptom table
        long symptomId = symptomController.insert(currentCircle.x, currentCircle.y,
                mainActivityDate, mainActivityTime, finalDuration, symptomDescriptionView.getText().toString(),
                intensityRadioButton.getText().toString(), symptomMedicamentView.getText().toString(), symptomFoodView.getText().toString(),
                intermittenceSwitchView.isChecked() ? 1 : 0, currentCircle.radius, bodyState);

        // The symptomId should be inserted correctly as well as the category selected options
        return (symptomId != -1 && insertSelectedCategoryOptions(symptomId));
    }

    private boolean updateSymptomsData() throws ParseException {
        logSelectedOptions();
        String stringDuration = symptomDurationView.getText().toString();
        int finalDuration = (!"".equals(stringDuration)) ? Integer.parseInt(stringDuration) : -1;
        int intensityCheckedViewId = intensityRadioGroupView.getCheckedRadioButtonId();
        RadioButton intensityRadioButton = findViewById(intensityCheckedViewId);
        long startDateLong = DateTimeUtils.getInstance().getDateFromString(startDateView.getText().toString()).getTime();
        long startTimeLong = DateTimeUtils.getInstance().getTimeFromString(startTimeView.getText().toString()).getTime();

        // Insert in symptom table
        // This symptom model holds the values that need are going to be updated
        SymptomModel symptomModelHolder = new SymptomModel(symptomModelToUpdate.getCirclePosX(), symptomModelToUpdate.getCirclePosY(),
                startDateLong, startTimeLong, finalDuration, symptomDescriptionView.getText().toString(),
                intensityRadioButton.getText().toString(), symptomMedicamentView.getText().toString(), symptomFoodView.getText().toString(),
                intermittenceSwitchView.isChecked() ? 1 : 0, symptomModelToUpdate.getCircleRadius(), symptomModelToUpdate.getCircleSide());

        return symptomController.updateSymptom(symptomIdToUpdate, symptomModelHolder) &&
                selectedCategoryOptionController.deleteAllBySymptom(symptomIdToUpdate) &&
                insertSelectedCategoryOptions(symptomIdToUpdate);

    }

    private void logSelectedOptions() {
        for (Map.Entry entry: optionsViewHashMap.entrySet()) {
            SymptomOptionView currentSymptomOptionView = ((SymptomOptionView)entry.getValue());
            if(currentSymptomOptionView.isChecked()){ Log.i("#","Checked " + currentSymptomOptionView.getName()); }
        }
    }

    private void loadSymptomCategories() {

        List <SymptomCategoryModel> categoryModels = SymptomCategoryController.getInstance(this).listAll();

        LinearLayout categoriesSection = findViewById(R.id.categories_section);

        for(SymptomCategoryModel categoryModel : categoryModels){
            inflateSymptomCategory(categoriesSection, categoryModel);
        }
    }

    /**
     * Should inflate each SymptomOption to the UI
     */
    private void inflateSymptomCategory(LinearLayout parentLayout, SymptomCategoryModel category) {

        // Get all the options for the given category
        List<SymptomCategoryOptionModel> options = SymptomCategoryOptionController.getInstance(this).listByCategory(category.getCategoryId());

        parentLayout.addView(inflateCategoryLabel(category.getName()));

        parentLayout.addView(inflateOptionsScrollView(options));

    }

    // Only create the scrollview that will be placed below the category label
    private HorizontalScrollView inflateOptionsScrollView(List<SymptomCategoryOptionModel> optionModels){
        LinearLayout optionsRow = new LinearLayout(this);
        optionsRow.setOrientation(LinearLayout.HORIZONTAL);
        optionsRow.setGravity(Gravity.CENTER);

        for (SymptomCategoryOptionModel option : optionModels) {
            SymptomOptionView newOption = createViewFromModel(option);
            optionsRow.addView(newOption);
            optionsViewHashMap.put(newOption.getName(), newOption);
        }

        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.addView(optionsRow);

        return scrollView;
    }

    private TextView inflateCategoryLabel(String categoryName){
        TextView categoryLabel = new TextView(this);
        categoryLabel.setText(categoryName);
        categoryLabel.setTextSize(20);
        categoryLabel.setPadding(20, 50, 20, 20);
        return categoryLabel;
    }


    private SymptomOptionView createViewFromModel(SymptomCategoryOptionModel option){
        SymptomOptionView optionView =  new SymptomOptionView(this);
        optionView.setSymptomOption(option);
        return optionView;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

}
