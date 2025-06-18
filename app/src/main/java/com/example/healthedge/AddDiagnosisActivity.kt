package com.example.healthedge

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.Diagnosis
import com.example.healthedge.models.User
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddDiagnosisActivity : AppCompatActivity() {
    private lateinit var patientSpinner: Spinner
    private lateinit var addressInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var bloodPressureInput: EditText
    private lateinit var referredByInput: EditText
    private lateinit var diagnosisInput: EditText
    private lateinit var medicineNameInput: EditText
    private lateinit var dosageInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var adviceGivenInput: EditText
    private lateinit var signatureInput: EditText
    private lateinit var nextVisitInput: EditText
    private lateinit var addDiagnosisButton: Button

    private var selectedUser: User? = null
    private var users: List<User> = emptyList()
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_diagnosis)

        patientSpinner = findViewById(R.id.patientSpinner)
        addressInput = findViewById(R.id.addressInput)
        weightInput = findViewById(R.id.weightInput)
        heightInput = findViewById(R.id.heightInput)
        bloodPressureInput = findViewById(R.id.bloodPressureInput)
        referredByInput = findViewById(R.id.referredByInput)
        diagnosisInput = findViewById(R.id.diagnosisInput)
        medicineNameInput = findViewById(R.id.medicineNameInput)
        dosageInput = findViewById(R.id.dosageInput)
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
                    val names = users.map { it.name }
                    val adapter = ArrayAdapter(this@AddDiagnosisActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    patientSpinner.adapter = adapter
                } else {
                    Toast.makeText(this@AddDiagnosisActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddDiagnosisActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        patientSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedUser = users.getOrNull(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedUser = null
            }
        }

        addDiagnosisButton.setOnClickListener {
            val user = selectedUser
            if (user == null) {
                Toast.makeText(this, "Please select a patient", Toast.LENGTH_SHORT).show()
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
                referredBy = referredByInput.text.toString(),
                diagnosis = diagnosisInput.text.toString(),
                medicineName = medicineNameInput.text.toString(),
                dosage = dosageInput.text.toString(),
                duration = durationInput.text.toString(),
                adviceGiven = adviceGivenInput.text.toString(),
                signature = signatureInput.text.toString(),
                nextVisit = nextVisitString,
                createdAt = createdAtString
            )
            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.addDiagnosis(diagnosis)
                    if (response.isSuccessful && response.body()?.get("success") == true) {
                        Toast.makeText(this@AddDiagnosisActivity, "Diagnosis added!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddDiagnosisActivity, "Failed to add diagnosis", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AddDiagnosisActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 