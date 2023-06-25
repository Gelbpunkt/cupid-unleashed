package org.kenvyra.unleashedmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.kenvyra.unleashedmanager.ui.theme.Typography
import org.kenvyra.unleashedmanager.ui.theme.UnleashedManagerTheme
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnleashedManagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChooseModeScreen()
                }
            }
        }
    }
}

fun setMode(newMode: String) {
    try {
        val process = Runtime.getRuntime().exec("su")
        val outputStream = DataOutputStream(process.outputStream)
        outputStream.writeBytes("setprop persist.unleashed.mode ${newMode}\n")
        outputStream.flush()
        outputStream.writeBytes("exit\n")
        outputStream.flush()
        process.waitFor()
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

fun getMode(): String? {
    try {
        val process = Runtime.getRuntime().exec("su")
        val outputStream = DataOutputStream(process.outputStream)
        val inputStream = BufferedReader(InputStreamReader(process.inputStream))

        outputStream.writeBytes("getprop persist.unleashed.mode\n")
        outputStream.flush()

        var line = inputStream.readLine()

        outputStream.writeBytes("exit\n")
        outputStream.flush()

        process.waitFor()

        if (line.isEmpty()) {
            return null
        } else {
            return line
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }

    return null
}

@Composable
fun ChooseModeScreen() {
    var previousMode = getMode() ?: "advertised"
    var selectedMode by remember { mutableStateOf(previousMode) }
    var applicationContext = LocalContext.current.applicationContext

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column {
            Text(
                "CPU Mode",
                style = Typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column {
                CpuMode("Benchmark", "benchmark", selectedMode) { newMode ->
                    selectedMode = newMode
                }
                CpuMode("Advertised", "advertised", selectedMode) { newMode ->
                    selectedMode = newMode
                }
                CpuMode("Efficiency", "efficiency", selectedMode) { newMode ->
                    selectedMode = newMode
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    setMode(selectedMode)
                    Toast.makeText(
                        applicationContext,
                        "Settings will take up to 60s to apply",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .indication(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple()
                    ),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Apply Settings",
                    style = Typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@Composable
fun CpuMode(
    sourceText: String,
    modeString: String,
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(bottom = 8.dp)
            .clickable { onModeSelected(modeString) }
            .indication(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == modeString,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(unselectedColor = MaterialTheme.colorScheme.onPrimaryContainer)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    sourceText,
                    style = Typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                )
            }
        }
    }
}
