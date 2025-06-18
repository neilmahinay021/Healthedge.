package com.example.healthedge

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.Diagnosis
import com.example.healthedge.models.User
import com.example.healthedge.models.Doctor
import com.example.healthedge.models.DiagnosisList
import com.example.healthedge.models.MedicineWithDosage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DiagnosisPanelActivity : AppCompatActivity() {
    private lateinit var patientSpinner: Spinner
    private lateinit var addressInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var bloodPressureInput: EditText
    private lateinit var referredBySpinner: Spinner
    private lateinit var diagnosisSpinner: Spinner
    private lateinit var medicineNameSpinner: Spinner
    private lateinit var dosageSpinner: Spinner
    private lateinit var durationInput: EditText
    private lateinit var adviceGivenInput: EditText
    private lateinit var signatureInput: EditText
    private lateinit var nextVisitInput: EditText
    private lateinit var addDiagnosisButton: Button

    private var selectedUser: User? = null
    private var users: List<User> = emptyList()
    private var doctors: List<Doctor> = emptyList()
    private var diagnosesList: List<DiagnosisList> = emptyList()
    private var medicines: List<MedicineWithDosage> = emptyList()
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnosis_panel)

        patientSpinner = findViewById(R.id.patientSpinner)
        addressInput = findViewById(R.id.addressInput)
        weightInput = findViewById(R.id.weightInput)
        heightInput = findViewById(R.id.heightInput)
        bloodPressureInput = findViewById(R.id.bloodPressureInput)
        referredBySpinner = findViewById(R.id.referredBySpinner)
        diagnosisSpinner = findViewById(R.id.diagnosisSpinner)
        medicineNameSpinner = findViewById(R.id.medicineNameSpinner)
        dosageSpinner = findViewById(R.id.dosageSpinner)
        durationInput = findViewById(R.id.durationInput)
        adviceGivenInput = findViewById(R.id.adviceGivenInput)
        signatureInput = findViewById(R.id.signatureInput)
        nextVisitInput = findViewById(R.id.nextVisitInput)
        addDiagnosisButton = findViewById(R.id.addDiagnosisButton)

        // Date picker for next visit
        nextVisitInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                nextVisitInput.setText(dateStr)
                selectedDate = dateStr
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        // Fetch users for spinner
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getUsers()
                if (response.isSuccessful) {
                    users = response.body() ?: emptyList()
                    val names = users.mapNotNull { it.name }
                    val adapter = ArrayAdapter(this@DiagnosisPanelActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    patientSpinner.adapter = adapter
                } else {
                    Toast.makeText(this@DiagnosisPanelActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DiagnosisPanelActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch doctors for spinner
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getDoctors()
                if (response.isSuccessful) {
                    doctors = response.body() ?: emptyList()
                    val doctorItems = doctors.mapNotNull { if (it.name != null && it.specialization != null) "${it.name} (${it.specialization})" else null }
                    val adapter = ArrayAdapter(this@DiagnosisPanelActivity, android.R.layout.simple_spinner_item, doctorItems)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    referredBySpinner.adapter = adapter
                } else {
                    Toast.makeText(this@DiagnosisPanelActivity, "Failed to load doctors", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DiagnosisPanelActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch diagnoses list for spinner
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getDiagnosesList()
                if (response.isSuccessful) {
                    diagnosesList = response.body() ?: emptyList()
                    
                    // Group diagnoses by category
                    val groupedDiagnoses = diagnosesList.groupBy { it.category }
                    val categories = groupedDiagnoses.keys.sorted()
                    
                    // Create a list of items with headers
                    val items = mutableListOf<Any>()
                    categories.filterNotNull().forEach { category ->
                        items.add(category)
                        items.addAll((groupedDiagnoses[category] ?: emptyList()).filterNotNull())
                    }
                    Log.d("DiagnosisPanel", "Spinner items: $items")
                    
                    val adapter = object : ArrayAdapter<Any>(
                        this@DiagnosisPanelActivity,
                        android.R.layout.simple_spinner_item,
                        items
                    ) {
                        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                            val view = super.getView(position, convertView, parent)
                            val item = getItem(position)
                            if (item == null) {
                                (view as TextView).text = ""
                                return view
                            }
                            if (item is String) {
                                view.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                                (view as TextView).setTextColor(resources.getColor(android.R.color.white))
                                view.text = item
                            } else if (item is DiagnosisList) {
                                view.setBackgroundColor(resources.getColor(android.R.color.white))
                                (view as TextView).setTextColor(resources.getColor(android.R.color.black))
                                view.text = item.name
                            }
                            return view
                        }
                        
                        override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                            val view = super.getDropDownView(position, convertView, parent)
                            val item = getItem(position)
                            if (item == null) {
                                (view as TextView).text = ""
                                return view
                            }
                            if (item is String) {
                                view.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                                (view as TextView).setTextColor(resources.getColor(android.R.color.white))
                                view.text = item
                            } else if (item is DiagnosisList) {
                                view.setBackgroundColor(resources.getColor(android.R.color.white))
                                (view as TextView).setTextColor(resources.getColor(android.R.color.black))
                                view.text = item.name
                            }
                            return view
                        }
                        
                        override fun isEnabled(position: Int): Boolean {
                            return getItem(position) is DiagnosisList
                        }
                    }
                    
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    diagnosisSpinner.adapter = adapter
                } else {
                    Toast.makeText(this@DiagnosisPanelActivity, "Failed to load diagnoses", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DiagnosisPanelActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Update medicine spinner when diagnosis changes
        diagnosisSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position)
                Log.d("DiagnosisPanel", "Selected diagnosis spinner item: $selectedItem")
                if (selectedItem is DiagnosisList) {
                    Log.d("DiagnosisPanel", "Selected diagnosis id: ${selectedItem.id}, name: ${selectedItem.name}")
                    lifecycleScope.launch {
                        try {
                            val response = ApiClient.apiService.getMedicinesByDisease(selectedItem.id)
                            if (response.isSuccessful) {
                                medicines = response.body() ?: emptyList()
                                Log.d("DiagnosisPanel", "Fetched medicines: $medicines")
                                val medicineItems = medicines.mapNotNull { medicine ->
                                    if (medicine.name != null) {
                                        if (medicine.genericName.isNullOrEmpty()) {
                                            medicine.name
                                        } else {
                                            "${medicine.name} (${medicine.genericName})"
                                        }
                                    } else null
                                }
                                val adapter = ArrayAdapter(this@DiagnosisPanelActivity, android.R.layout.simple_spinner_item, medicineItems)
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                medicineNameSpinner.adapter = adapter
                            } else {
                                Toast.makeText(this@DiagnosisPanelActivity, "Failed to load medicines", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@DiagnosisPanelActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Update dosage spinner when medicine changes
        medicineNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedMedicine = medicines.getOrNull(position)
                Log.d("DiagnosisPanel", "Selected medicine: $selectedMedicine")
                if (selectedMedicine != null) {
                    val dosageItems = listOfNotNull(
                        selectedMedicine.dosage?.let { d -> selectedMedicine.frequency?.let { f -> "$d - $f" } },
                        selectedMedicine.duration
                    )
                    val adapter = ArrayAdapter(this@DiagnosisPanelActivity, android.R.layout.simple_spinner_item, dosageItems)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    dosageSpinner.adapter = adapter
                    durationInput.setText(selectedMedicine.duration)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        patientSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedUser = users.getOrNull(position)
                // Auto-fill address from user
                addressInput.setText(selectedUser?.address ?: "")
                // Fetch vitals and auto-fill vitals fields
                val userId = selectedUser?.id ?: return
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.apiService.getVitals(userId)
                        if (response.isSuccessful) {
                            val vitals = response.body()?.firstOrNull()
                            weightInput.setText(vitals?.weight ?: "")
                            heightInput.setText(vitals?.height ?: "")
                            bloodPressureInput.setText(vitals?.bloodPressure ?: "")
                        } else {
                            weightInput.setText("")
                            heightInput.setText("")
                            bloodPressureInput.setText("")
                        }
                    } catch (e: Exception) {
                        weightInput.setText("")
                        heightInput.setText("")
                        bloodPressureInput.setText("")
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedUser = null
                addressInput.setText("")
                weightInput.setText("")
                heightInput.setText("")
                bloodPressureInput.setText("")
            }
        }

        addDiagnosisButton.setOnClickListener {
            val user = selectedUser
            if (user == null) {
                Toast.makeText(this, "Please select a patient", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedDoctor = doctors.getOrNull(referredBySpinner.selectedItemPosition)
            val selectedItem = diagnosisSpinner.selectedItem
            if (selectedItem !is DiagnosisList) {
                Toast.makeText(this, "Please select a valid diagnosis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedMedicine = medicines.getOrNull(medicineNameSpinner.selectedItemPosition)

            if (selectedDoctor == null || selectedMedicine == null) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert selectedDate (String?) to Date?
            val nextVisitDate: Date? = selectedDate?.let {
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                } catch (e: Exception) {
                    null
                }
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val createdAtString = dateFormat.format(Date())
            val nextVisitString = selectedDate

            val diagnosis = Diagnosis(
                id = 0,
                patientCode = user.id,
                address = addressInput.text.toString(),
                weight = weightInput.text.toString().toDoubleOrNull(),
                height = heightInput.text.toString().toDoubleOrNull(),
                bloodPressure = bloodPressureInput.text.toString(),
                referredBy = "${selectedDoctor.name} (${selectedDoctor.specialization})",
                diagnosis = selectedItem.name,
                medicineName = selectedMedicine.let { medicine ->
                    if (medicine.genericName.isNullOrEmpty()) {
                        medicine.name
                    } else {
                        "${medicine.name} (${medicine.genericName})"
                    }
                },
                dosage = "${selectedMedicine.dosage} - ${selectedMedicine.frequency}",
                duration = selectedMedicine.duration,
                adviceGiven = adviceGivenInput.text.toString(),
                signature = signatureInput.text.toString(),
                nextVisit = nextVisitString,
                createdAt = createdAtString
            )

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.addDiagnosis(diagnosis)
                    if (response.isSuccessful && response.body()?.get("success") == true) {
                        Toast.makeText(this@DiagnosisPanelActivity, "Diagnosis added!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@DiagnosisPanelActivity, "Failed to add diagnosis", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@DiagnosisPanelActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}