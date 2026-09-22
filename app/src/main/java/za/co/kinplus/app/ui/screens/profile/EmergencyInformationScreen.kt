package za.co.kinplus.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import za.co.kinplus.app.ui.components.KinTopAppBar
import za.co.kinplus.app.ui.screens.auth.AuthDivider
import za.co.kinplus.app.ui.screens.auth.AuthMuted
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

private fun EmergencyInfo.percentFor(category: EmergencyInfoCategory): Int = when (category) {
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

@Composable
fun EmergencyInformationScreen(
    onBack: () -> Unit,
    onOpenCategory: (EmergencyInfoCategory) -> Unit,
    viewModel: EmergencyInfoListViewModel = hiltViewModel()
) {
    val info by viewModel.info.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        KinTopAppBar(title = stringResource(R.string.emergency_information), onBack = onBack)

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Column {
                    EmergencyInfoCategory.entries.forEachIndexed { index, category ->
                        CategoryRow(category, info.percentFor(category)) { onOpenCategory(category) }
                        if (index != EmergencyInfoCategory.entries.lastIndex) HorizontalDivider(color = AuthDivider)
                    }
                }
            }
        }

        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = AuthMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.data_safe_title), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.data_safe_body), color = AuthMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryRow(category: EmergencyInfoCategory, percent: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(category.titleRes), fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.percent_complete, percent), color = AuthMuted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    trackColor = AuthDivider
                )
            }
        },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AuthMuted) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ---------------------------------------------------------------------------
// Generic per-category form
// ---------------------------------------------------------------------------

private data class FieldSpec(val key: String, val labelRes: Int)

private val fieldsByCategory: Map<EmergencyInfoCategory, List<FieldSpec>> = mapOf(
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

@Composable
fun EmergencyInfoFormScreen(
    onBack: () -> Unit,
    viewModel: EmergencyInfoFormViewModel = hiltViewModel()
) {
    val fields by viewModel.fields.collectAsState()
    val specs = fieldsByCategory[viewModel.category].orEmpty()

    Column(Modifier.fillMaxSize()) {
        KinTopAppBar(
            title = stringResource(viewModel.category.titleRes),
            onBack = onBack,
            actions = { TextButton(onClick = { viewModel.save(onBack) }) { Text(stringResource(R.string.save)) } }
        )

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            specs.forEach { spec ->
                OutlinedTextField(
                    value = fields[spec.key].orEmpty(),
                    onValueChange = { viewModel.onFieldChange(spec.key, it) },
                    label = { Text(stringResource(spec.labelRes)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = spec.key != "notes"
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
