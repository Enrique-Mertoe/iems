package com.smallvillecycle.iems

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smallvillecycle.iems.ui.theme.Green40
import com.smallvillecycle.iems.ui.theme.Green80
import com.smallvillecycle.iems.ui.theme.GreenGrey40
import com.smallvillecycle.iems.ui.theme.GreenGrey80
import com.smallvillecycle.iems.ui.theme.Lime40
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ConnectionStatus { CONNECTED, CONNECTING, OFFLINE, NOT_PAIRED }
object ReqCodes {
    const
    val REQUEST_ENABLE_BT: Int = 0

    const
    val REQUEST_DISCOVERABLE_BT: Int = 0
}

object Control {
    public var device: BluetoothDevice? = null
}

class MainActivity : ComponentActivity() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val ESP32_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.BLUETOOTH] == true &&
                    permissions[Manifest.permission.BLUETOOTH_ADMIN] == true
            if (granted) {
                discoverDevices()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            AppUI()
        }
    }

    private fun checkPermissions() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        } else {
            discoverDevices()
        }
    }

    @Composable
    fun AppUI() {
        var receivedData by remember { mutableStateOf("") }
        var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
        var isConnected by remember { mutableStateOf(false) }
        var showDeviceModal by remember { mutableStateOf(false) }
        var connectionStatus by remember { mutableStateOf(ConnectionStatus.NOT_PAIRED) }
        var lightIntensity by remember {
            mutableFloatStateOf(0f)
        }

        LaunchedEffect(Unit) {
            devices = getPairedDevices()
            if (devices.isEmpty()) showDeviceModal = true
        }

        if (showDeviceModal) {
            AlertDialog(
                onDismissRequest = { showDeviceModal = false },
                title = { Text("Select Device") },
                text = {
                    if (devices.isEmpty()) {
                        LaunchedEffect(Unit) {
                            devices = getPairedDevices()
                        }
                    }
                    LazyColumn {
                        items(devices) { device ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var loading by remember {
                                    mutableStateOf(false)
                                }
                                val coroutineScope = rememberCoroutineScope()
                                Button(
                                    onClick = {
                                        loading = true
                                        coroutineScope.launch {
                                            isConnected = connectToDevice(device)
                                            loading = false
                                            Control.device = device
                                            showDeviceModal = false
                                        }
                                        Control.device = device
                                        showDeviceModal = false

                                    }, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(device.name ?: device.address)

                                }
                                if (loading) {
                                    CircularProgressIndicator(
                                        color = GreenGrey40
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showDeviceModal = false }) {
                        Text("Cancel")
                    }
                }, dismissButton = {
                    Button(onClick = { openBluetoothSettings() }) {
                        Text("Not found")
                    }

                }
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("IEMS", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            var message = when (connectionStatus) {
                ConnectionStatus.NOT_PAIRED -> "No Paired Device"
                ConnectionStatus.OFFLINE -> "Device Offline"
                else -> ""
            }

            val buttonText = when (connectionStatus) {
                ConnectionStatus.NOT_PAIRED -> "Pair Device"
                ConnectionStatus.OFFLINE -> "Reconnect"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(message, fontSize = 20.sp, color = Green40)

                if (buttonText.isNotEmpty()) {
                    Button(onClick = { showDeviceModal = true }) {
                        Text(buttonText)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isConnected) {
                ControlScreen(
                    title = "Output A",
                    onDisconnect = { connectionStatus = ConnectionStatus.OFFLINE },
                    onSend = { data ->
                        sendData("outA$data")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ControlScreen(
                    title = "Output B",
                    onDisconnect = { connectionStatus = ConnectionStatus.OFFLINE },
                    onSend = { data ->
                        sendData("outB$data")
                    }
                )

                Text("Light Intensity: $lightIntensity lux", color = Color.Black)
            }
        }
    }

    private fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }

    private fun discoverDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.let {
            val deviceList = it.filter { device -> device.name == "IEMS-LomTechnology" }
            Log.d("Bluetooth", "Found ${deviceList.size} paired devices")
        }
    }

    private fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.filter { device -> device.name == "IEMS-LomTechnology" }
            ?: emptyList()
    }

    private fun connectToDevice(device: BluetoothDevice): Boolean {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(ESP32_UUID)
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: Exception) {
            Log.e("Bluetooth", "Connection failed", e)
            Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            Toast.makeText(this, "Data sent!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Bluetooth", "Send failed", e)
        }
    }

    private fun receiveData(): String {
        return try {
            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer)
            bytes?.let { String(buffer, 0, it) } ?: "No data received"
        } catch (e: Exception) {
            Log.e("Bluetooth", "Receive failed", e)
            "Error receiving data"
        }
    }

    private fun listenForData() {
        Thread {
            while (true) {
                val receivedData = receiveData()
                if (receivedData.isNotBlank() && receivedData != "No data received" && receivedData != "Error receiving data") {
                    runOnUiThread {
                        Log.d("Bluetooth", "📩 Data received: $receivedData")
                        handleReceivedData(receivedData)
                    }
                }
            }
        }.start()
    }

    private fun handleReceivedData(data: String) {
        val delimiterIndex = data.indexOf("--src--")
        if (delimiterIndex != -1) {
            val output = data.substring(0, delimiterIndex)
            val source = data.substring(delimiterIndex + 7)

            when (output) {
                "outA" -> {
                    when (source) {
                        "solar" -> showToast("Output A switched to Solar")
                        "grid" -> showToast("Output A switched to Grid")
                        else -> showToast("Unknown source for Output A: $source")
                    }
                }
                "outB" -> {
                    when (source) {
                        "solar" -> showToast("Output B switched to Solar")
                        "grid" -> showToast("Output B switched to Grid")
                        else -> showToast("Unknown source for Output B: $source")
                    }
                }
                else -> showToast("Unknown output: $output")
            }
        } else {
            showToast("Received raw data: $data")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}

@Composable
fun ControlScreen(
    title: String,
    onDisconnect: () -> Unit,
    onSend: (String) -> Unit
) {
    var isSolarEnabled by remember { mutableStateOf(false) }
    var power by remember { mutableFloatStateOf(0f) }
    Column(modifier = Modifier.padding(4.dp)) {
        EnergyCard(
            title = title,
            power = power,
            isSolarEnabled = isSolarEnabled,
//            connectionStatus = ConnectionStatus.CONNECTED,
            onToggle = {
                isSolarEnabled = !isSolarEnabled
                val label = if(isSolarEnabled) "solar" else "grid"
                onSend("--src--$label")
            },
//            onReconnect = onDisconnect,
//            onScanAndPair = {}
        )
    }
}

@Composable
fun EnergyCard(title: String, power: Float, isSolarEnabled: Boolean, onToggle: () -> Unit) {
    val backgroundColor = if (isSolarEnabled) Green80 else Green40
    val textColor = if (isSolarEnabled) Color.Black else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                "Power: ${power}W",
                color = textColor
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isSolarEnabled) "Solar" else "Grid",
                    color = textColor
                )
                Switch(
                    checked = isSolarEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Lime40,
                        checkedTrackColor = GreenGrey80,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = GreenGrey40
                    )
                )
            }
        }
    }
}