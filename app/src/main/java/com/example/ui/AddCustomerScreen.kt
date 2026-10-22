package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.TallyGaveRed
import com.example.ui.theme.TallyGotGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    viewModel: TallyKhataViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val currency = profile.currency.ifBlank { "$" }

    // Form inputs
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf("CUSTOMER") } // "CUSTOMER" or "SUPPLIER"

    // Opening balance
    var openingType by remember { mutableStateOf("NONE") } // "NONE", "GAVE", "GOT"
    var openingAmountText by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    fun submitCustomer() {
        if (name.trim().isBlank()) {
            nameError = "Customer / Party name is required"
            return
        }
        nameError = null
        isSubmitting = true

        val openingAmt = openingAmountText.toDoubleOrNull() ?: 0.0

        viewModel.addCustomer(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            type = partyType,
            openingBalance = if (openingType != "NONE") openingAmt else 0.0,
            openingType = openingType,
            onComplete = { success ->
                isSubmitting = false
                if (success) {
                    onBack()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New Party / Khata",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_customer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to customer list"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isSubmitting) submitCustomer()
                },
                expanded = true,
                icon = {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Customer"
                        )
                    }
                },
                text = {
                    Text(
                        text = if (isSubmitting) "Saving..." else "Save Customer",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("save_customer_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Digital Khata",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Record customer credit debts (Due) and supplier purchases seamlessly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Core Form Card: Name, Phone, Address
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Customer Details",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Customer Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) nameError = null
                            },
                            label = { Text("Customer / Party Name *") },
                            placeholder = { Text("e.g. Rahim Store / Kamal Hossain") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            isError = nameError != null,
                            supportingText = {
                                if (nameError != null) {
                                    Text(text = nameError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_customer_name_input")
                        )

                        // Phone Number Field
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile / Phone Number") },
                            placeholder = { Text("e.g. 01712-345678") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_customer_phone_input")
                        )

                        // Address Field
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address / Market Location") },
                            placeholder = { Text("Shop #12, Sadarghat Market Road, Dhaka") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_customer_address_input")
                        )
                    }
                }

                // Party Type Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Party Type",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = partyType == "CUSTOMER",
                                onClick = { partyType = "CUSTOMER" },
                                label = { Text("Customer (Grahok)", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("party_type_customer_chip")
                            )

                            FilterChip(
                                selected = partyType == "SUPPLIER",
                                onClick = { partyType = "SUPPLIER" },
                                label = { Text("Supplier (Vendor)", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("party_type_supplier_chip")
                            )
                        }
                    }
                }

                // Optional Opening Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Opening Balance (Optional)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "If this party already has a previous outstanding debt or advance balance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Balance Type Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = openingType == "NONE",
                                onClick = { openingType = "NONE" },
                                label = { Text("0 Balance") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = openingType == "GAVE",
                                onClick = { openingType = "GAVE" },
                                label = { Text("Due (You Gave)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TallyGaveRed.copy(alpha = 0.2f),
                                    selectedLabelColor = TallyGaveRed
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = openingType == "GOT",
                                onClick = { openingType = "GOT" },
                                label = { Text("Advance (You Got)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TallyGotGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = TallyGotGreen
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AnimatedVisibility(visible = openingType != "NONE") {
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                OutlinedTextField(
                                    value = openingAmountText,
                                    onValueChange = { openingAmountText = it },
                                    label = {
                                        Text(
                                            if (openingType == "GAVE") "Previous Due Amount ($currency) *"
                                            else "Previous Advance Amount ($currency) *"
                                        )
                                    },
                                    placeholder = { Text("e.g. 500") },
                                    prefix = { Text("$currency ", fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("opening_balance_amount_input")
                                )
                            }
                        }
                    }
                }

                // In-Form Save Button
                Button(
                    onClick = {
                        if (!isSubmitting) submitCustomer()
                    },
                    enabled = !isSubmitting && name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_customer_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Saving Party...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Save Party & Open Khata", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB visibility
            }
        }
    }
}
