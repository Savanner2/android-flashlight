package com.example.flashlight

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.flashlight.ui.theme.FlashlightTheme
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlashlightTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlashlightLayout()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightLayout(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val flashlightManager by remember {
        mutableStateOf(FlashlightManager(context))
    }

    val flashAvailable = flashlightManager.cameraManager.getCameraCharacteristics(flashlightManager.cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

    var sliderPosition by rememberSaveable {
        mutableFloatStateOf(0f)
    }
    var torchState by rememberSaveable {
        mutableStateOf(false)
    }
    var handlerRunning by rememberSaveable {
        mutableStateOf(false)
    }

    val sliderInt = sliderPosition.roundToInt()
    val sliderMax = 9

    val mRunnable = MyRunnable(
        context as Activity,
        ((sliderMax - sliderInt)*40).toLong(),
        flashlightManager
    )

    LaunchedEffect(Unit) {
        if(flashAvailable) {
            try {
                flashlightManager.turnOff()
            } catch (e: Exception) {
                e.printStackTrace()
                shortToast(context, e.message)
            }
        }
    }

    Scaffold(
        topBar = {
            FlashlightTopBar()
        }
    ) { padding ->
        LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp)
        ) {
            if(flashAvailable){
                SliderWithNumbers(
                    sliderPosition = sliderPosition,
                    sliderMax = sliderMax,
                    onSliderValueChange = {sliderPosition = it},
                    onSliderValueChangeFinished = {
                        if(handlerRunning) {
                            mHandler.removeCallbacksAndMessages(null)
                            mHandler.post(mRunnable)
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    OnOffButton(
                        torchState = torchState,
                        onTorchToggle = {
                            if(sliderInt == 0) {
                                if(!torchState) {
                                    flashlightManager.turnOn()
                                } else {
                                    flashlightManager.turnOff()
                                }
                            } else {
                                if(!torchState) {
                                    mHandler.post(mRunnable)
                                    handlerRunning = true
                                } else {
                                    mHandler.removeCallbacksAndMessages(null)
                                    flashlightManager.turnOff()
                                    handlerRunning = false
                                }
                            }
                            torchState = it

                        },
                        size = 300,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )

                }
            } else {
                Text(
                    text = "No flash available"
                )
            }
        }
    }
}

@Composable
fun SliderWithNumbers(modifier: Modifier = Modifier, sliderPosition: Float, onSliderValueChange: (Float) -> (Unit), onSliderValueChangeFinished: () -> (Unit), sliderMax: Int) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .padding(horizontal = 5.dp)
            .fillMaxWidth()
    ) {
        for(i in 0..sliderMax) {
            Text(
                text = "$i"
            )
        }
    }
    Slider(
        value = sliderPosition,
        onValueChange = onSliderValueChange,
        onValueChangeFinished = onSliderValueChangeFinished,
        steps = sliderMax-1,
        valueRange = 0f..sliderMax.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            activeTickColor = MaterialTheme.colorScheme.onSurface,
            inactiveTickColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
    )
}

@Composable
fun OnOffButton(modifier: Modifier = Modifier, size: Int = 50, torchState: Boolean, onTorchToggle: (Boolean)->(Unit)) {
    FilledTonalIconToggleButton(
        checked = torchState,
        onCheckedChange = onTorchToggle,
        modifier = modifier
            .size(size.dp)
    ) {
        val tint by animateColorAsState(if (torchState) Color.Green else Color.Red)
        val desc = if(torchState) "off" else "on"
        Icon(
            imageVector = Icons.Filled.PowerSettingsNew,
            contentDescription = "Turn flashlight $desc",
            modifier = Modifier
                .size(size.dp),
            tint = tint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightTopBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier

            ){
                Image(
                    painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(60.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "Flashlight",
                    style = MaterialTheme.typography.headlineLarge
                )
            }


        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = modifier
    )
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FlashlightTheme {
        FlashlightLayout()
    }
}

fun shortToast(context: Context, message: String?) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private val mHandler = Handler(Looper.getMainLooper())

class MyRunnable(
    activity: Activity,
    private var timeout: Long,
    private var flManager: FlashlightManager
) : Runnable {
    private val mActivity: WeakReference<Activity>

    init {
        mActivity = WeakReference(activity)
    }

    override fun run() {
        val activity = mActivity.get()
        if (activity != null) {
            flManager.epilepsy()
            mHandler.postDelayed(this, timeout)
        }
    }
}