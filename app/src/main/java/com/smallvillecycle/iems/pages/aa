package com.smallvillecycle.iems

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smallvillecycle.iems.ui.theme.Green40
import com.smallvillecycle.iems.ui.theme.Green80
import com.smallvillecycle.iems.ui.theme.GreenGrey40
import com.smallvillecycle.iems.ui.theme.GreenGrey80
import com.smallvillecycle.iems.ui.theme.IEMSTheme
import com.smallvillecycle.iems.ui.theme.Lime40
import java.util.UUID

enum class ConnectionStatus { CONNECTED, CONNECTING, OFFLINE, NOT_PAIRED }
object ReqCodes{
    const
    val REQUEST_ENABLE_BT: Int = 0
    const
    val REQUEST_DISCOVERABLE_BT: Int = 0
}

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,"device not supported",Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            startActivityForResult(enableBtIntent, ReqCodes.REQUEST_ENABLE_BT)
        }

        setContent {
            IEMSTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) { innerPadding ->
                    RequestBluetoothPermissions()
                    EnergyDashboard(
                        output1Power = 100f,
                        output2Power = 150f,
                        lightIntensity = 4f,
                        isSolarEnabled1 = false,
                        isSolarEnabled2 = false,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun EnergyDashboard(
    output1Power: Float,
    output2Power: Float,
    lightIntensity: Float,
    isSolarEnabled1: Boolean,
    isSolarEnabled2: Boolean,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    var connectionStatus by remember { mutableStateOf(ConnectionStatus.NOT_PAIRED) }

    var powerConsumption by remember { mutableFloatStateOf(0f) }
    var pairedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "Intelligent Energy Management", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        val message = when (connectionStatus) {
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
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, fontSize = 20.sp, color = Green40)

            if (buttonText.isNotEmpty()) {
                Button(onClick = {  }) {
                    Text(buttonText)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ControlScreen(
            title = "Output A",
            power = powerConsumption,
            onDisconnect = { connectionStatus = ConnectionStatus.OFFLINE }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ControlScreen(
            title = "Output B",
            power = powerConsumption,
            onDisconnect = { connectionStatus = ConnectionStatus.OFFLINE }
        )

        Text("Light Intensity: $lightIntensity lux", color = Color.Black)

    }
}

@Composable
fun BluetoothConnectionManager() {
    var pairedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var connectionStatus by remember { mutableStateOf(ConnectionStatus.NOT_PAIRED) }

    when (connectionStatus) {
        ConnectionStatus.NOT_PAIRED -> {
            ScanForDevices { device ->
                pairedDevice = device
                connectionStatus = ConnectionStatus.CONNECTING
            }
        }

        ConnectionStatus.CONNECTING -> {
            pairedDevice?.let { device ->
                ConnectToDevice(device) { connected ->
                    connectionStatus = if (connected) ConnectionStatus.CONNECTED else ConnectionStatus.OFFLINE
                }
            }
        }

        ConnectionStatus.CONNECTED -> {

        }

        ConnectionStatus.OFFLINE -> {
            OfflineScreen(onReconnect = { connectionStatus = ConnectionStatus.NOT_PAIRED })
        }
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

@Composable
fun ControlScreen(
    title:String,
    power: Float,
    onDisconnect: () -> Unit
) {
    var isSolarEnabled by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(4.dp)) {
        EnergyCard(
            title = title,
            power = power,
            isSolarEnabled = isSolarEnabled,
//            connectionStatus = ConnectionStatus.CONNECTED,
            onToggle = { isSolarEnabled = !isSolarEnabled },
//            onReconnect = onDisconnect,
//            onScanAndPair = {}
        )
    }
}


@Composable
fun OfflineScreen(onReconnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Device Offline", fontSize = 20.sp, color = Green40)
        Button(onClick = onReconnect) {
            Text("Reconnect")
        }
    }
}

@Composable
fun ConnectToDevice(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
    val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.randomUUID()
    val context = LocalContext.current

    // Check for BLUETOOTH_CONNECT permission
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            onResult(true) // Connection successful
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false) // Connection failed
        }
    } else {
        // Request permission if not granted
        RequestBluetoothPermissions()
    }
}

@Composable
fun ScanForDevices(onDeviceFound: (BluetoothDevice) -> Unit) {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    if (bluetoothAdapter?.isEnabled == true) {
        // Check for permissions
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val discoveryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    if (action == BluetoothDevice.ACTION_FOUND) {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let { onDeviceFound(it) }
                    }
                }
            }

            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(discoveryReceiver, filter)
            bluetoothAdapter.startDiscovery()
        } else {
            // Request the missing permission
            RequestBluetoothPermissions()
        }
    }
}


@Composable
fun RequestBluetoothPermissions() {
    val context = LocalContext.current
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        LaunchedEffect(Unit) {
            if (permissions.any {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                }) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    permissions,
                    ReqCodes.REQUEST_ENABLE_BT
                )
            }
        }

}



@RequiresApi(Build.VERSION_CODES.S)
@Preview
@Composable
fun Bh(){
    EnergyDashboard(
        output1Power = 100f,
        output2Power = 150f,
        lightIntensity = 4f,
        isSolarEnabled1 = false,
        isSolarEnabled2 = false,
    )
}