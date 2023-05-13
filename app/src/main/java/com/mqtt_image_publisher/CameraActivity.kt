package com.mqtt_image_publisher

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var publishButton: Button
    private lateinit var settingsButton: Button
    private lateinit var imageView: ImageView
    private lateinit var mqttClient: MqttAndroidClient
    private lateinit var imageBitmap: Bitmap
    private lateinit var topic: String
    // LOG TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    fun logAndPrint(msg: String){
        Log.d(TAG, msg)
        Toast.makeText(applicationContext,msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        button = findViewById(R.id.button)
        imageView = findViewById(R.id.imageView)
        publishButton = findViewById(R.id.button_publish)
        settingsButton = findViewById(R.id.button_settings)

        settingsButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button.setOnClickListener{
            var i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startCamera.launch(i)
        }

        publishButton.setOnClickListener{publishImage()}

        setupMqttClient()


        var i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startCamera.launch(i)
    }

    private val startCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data?.getParcelableExtra<Bitmap>("data")
                if (bitmap != null) {
                    imageBitmap = bitmap
                    imageView.setImageBitmap(imageBitmap)
                }
            } else {
                // todo: Handle the canceled or failed result
            }
        }

    private fun setupMqttClient() {
        val brokerUrl = intent.getStringExtra("brokerUrl")
        val clientId = intent.getStringExtra("clientId")
        val topicTmp = intent.getStringExtra("topic")
        if(topicTmp != null)
            topic = topicTmp

        mqttClient = MqttAndroidClient(this, brokerUrl, clientId)
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
            }

            override fun connectionLost(cause: Throwable?) {
                logAndPrint( "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                logAndPrint("Image delivered successully!")
            }
        })
        mqttClient.connect(MqttConnectOptions(),null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                logAndPrint("Successfully connected to MQTT broker")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                logAndPrint("Failed to connect to MQTT broker: ${exception?.message}")
            }
        })
    }

    private fun generateFilename(): String {
        val currentTime = Date()
        val format = SimpleDateFormat("MM_dd_HH_mm_ss", Locale.getDefault())
        val formattedTime = format.format(currentTime)
        return "$formattedTime.png" // Replace with your desired file extension
    }

    private fun generateMqttImageMsg(): String {
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val imageData = stream.toByteArray()
        val json = JSONObject()
        json.put("name", generateFilename())
        json.put("data", Base64.getEncoder().encodeToString(imageData))
        return json.toString()
    }

    private fun publishImage(){
        try {
            val message = MqttMessage()
            message.payload = generateMqttImageMsg().toByteArray()
            message.qos = 2
            message.isRetained = false
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    logAndPrint("Published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    logAndPrint( "Failed to publish to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}