package za.co.kinplus.app.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.co.kinplus.app.R
import za.co.kinplus.app.data.repository.EmergencyInfoRepository
import za.co.kinplus.app.domain.EmergencyInfo
import javax.inject.Inject

enum class EmergencyInfoCategory(val route: String, val titleRes: Int) {
    ABOUT_ME("about_me", R.string.emergency_info_about_me),
    MEDICAL("medical", R.string.emergency_info_medical),
    VEHICLE("vehicle", R.string.emergency_info_vehicle),
    ADDRESSES("addresses", R.string.emergency_info_addresses),
    HOME_SECURITY("home_security", R.string.emergency_info_home_security);

    companion object {
        fun fromRoute(route: String?) = entries.firstOrNull { it.route == route } ?: ABOUT_ME
    }
}

internal fun EmergencyInfo.percentFor(category: EmergencyInfoCategory): Int = when (category) {
    EmergencyInfoCategory.ABOUT_ME -> aboutMe.percentComplete
    EmergencyInfoCategory.MEDICAL -> medical.percentComplete
    EmergencyInfoCategory.VEHICLE -> vehicle.percentComplete
    EmergencyInfoCategory.ADDRESSES -> addresses.percentComplete
    EmergencyInfoCategory.HOME_SECURITY -> homeSecurity.percentComplete
}

@HiltViewModel
class EmergencyInfoListViewModel @Inject constructor(
    repository: EmergencyInfoRepository
) : ViewModel() {
    val info: StateFlow<EmergencyInfo> =
        repository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmergencyInfo())
}

// ---------------------------------------------------------------------------
// Generic per-category form
// ---------------------------------------------------------------------------

internal data class FieldSpec(val key: String, val labelRes: Int)

internal val fieldsByCategory: Map<EmergencyInfoCategory, List<FieldSpec>> = mapOf(
    EmergencyInfoCategory.ABOUT_ME to listOf(
        FieldSpec("idNumber", R.string.field_id_number),
        FieldSpec("dateOfBirth", R.string.field_date_of_birth),
        FieldSpec("bloodType", R.string.field_blood_type),
        FieldSpec("notes", R.string.field_notes)
    ),
    EmergencyInfoCategory.MEDICAL to listOf(
        FieldSpec("conditions", R.string.field_medical_conditions),
        FieldSpec("allergies", R.string.field_medical_allergies),
        FieldSpec("medications", R.string.field_medical_medications),
        FieldSpec("doctorContact", R.string.field_doctor_contact)
    ),
    EmergencyInfoCategory.VEHICLE to listOf(
        FieldSpec("make", R.string.field_vehicle_make),
        FieldSpec("model", R.string.field_vehicle_model),
        FieldSpec("color", R.string.field_vehicle_color),
        FieldSpec("plate", R.string.field_vehicle_plate)
    ),
    EmergencyInfoCategory.ADDRESSES to listOf(
        FieldSpec("home", R.string.field_home_address),
        FieldSpec("work", R.string.field_work_address)
    ),
    EmergencyInfoCategory.HOME_SECURITY to listOf(
        FieldSpec("alarmCompany", R.string.field_alarm_company),
        FieldSpec("gateCode", R.string.field_gate_code),
        FieldSpec("alarmCode", R.string.field_alarm_code),
        FieldSpec("notes", R.string.field_notes)
    )
)

private fun EmergencyInfo.valuesFor(category: EmergencyInfoCategory): Map<String, String> = when (category) {
    EmergencyInfoCategory.ABOUT_ME -> mapOf(
        "idNumber" to aboutMe.idNumber, "dateOfBirth" to aboutMe.dateOfBirth,
        "bloodType" to aboutMe.bloodType, "notes" to aboutMe.notes
    )
    EmergencyInfoCategory.MEDICAL -> mapOf(
        "conditions" to medical.conditions, "allergies" to medical.allergies,
        "medications" to medical.medications, "doctorContact" to medical.doctorContact
    )
    EmergencyInfoCategory.VEHICLE -> mapOf(
        "make" to vehicle.make, "model" to vehicle.model,
        "color" to vehicle.color, "plate" to vehicle.plate
    )
    EmergencyInfoCategory.ADDRESSES -> mapOf("home" to addresses.home, "work" to addresses.work)
    EmergencyInfoCategory.HOME_SECURITY -> mapOf(
        "alarmCompany" to homeSecurity.alarmCompany, "gateCode" to homeSecurity.gateCode,
        "alarmCode" to homeSecurity.alarmCode, "notes" to homeSecurity.notes
    )
}

@HiltViewModel
class EmergencyInfoFormViewModel @Inject constructor(
    private val repository: EmergencyInfoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val category: EmergencyInfoCategory = EmergencyInfoCategory.fromRoute(savedStateHandle["category"])

    private val _fields = MutableStateFlow<Map<String, String>>(emptyMap())
    val fields: StateFlow<Map<String, String>> = _fields

    init {
        viewModelScope.launch {
            _fields.value = repository.observe().first().valuesFor(category)
        }
    }

    fun onFieldChange(key: String, value: String) {
        _fields.value = _fields.value.toMutableMap().apply { put(key, value) }
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = _fields.value
        when (category) {
            EmergencyInfoCategory.ABOUT_ME -> repository.saveAboutMe(
                f["idNumber"].orEmpty(), f["dateOfBirth"].orEmpty(), f["bloodType"].orEmpty(), f["notes"].orEmpty()
            )
            EmergencyInfoCategory.MEDICAL -> repository.saveMedical(
                f["conditions"].orEmpty(), f["allergies"].orEmpty(), f["medications"].orEmpty(), f["doctorContact"].orEmpty()
            )
            EmergencyInfoCategory.VEHICLE -> repository.saveVehicle(
                f["make"].orEmpty(), f["model"].orEmpty(), f["color"].orEmpty(), f["plate"].orEmpty()
            )
            EmergencyInfoCategory.ADDRESSES -> repository.saveAddresses(f["home"].orEmpty(), f["work"].orEmpty())
            EmergencyInfoCategory.HOME_SECURITY -> repository.saveHomeSecurity(
                f["alarmCompany"].orEmpty(), f["gateCode"].orEmpty(), f["alarmCode"].orEmpty(), f["notes"].orEmpty()
            )
        }
        onDone()
    }
}
