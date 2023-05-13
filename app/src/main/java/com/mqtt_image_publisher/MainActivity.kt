package com.mqtt_image_publisher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var brokerUrlText: EditText
    private lateinit var topicText: EditText
    private lateinit var clientIdText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        brokerUrlText = findViewById(R.id.edit_broker_url)
        topicText = findViewById(R.id.edit_topic)
        clientIdText = findViewById(R.id.edit_client_id)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.WAKE_LOCK), 111)
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 112)
        }
        else // todo: both must be set to enable
            button.isEnabled = true


        button.setOnClickListener{
            if(allFieldsAreFilled()) {
                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra("brokerUrl", brokerUrlText.text.toString())
                intent.putExtra("topic", topicText.text.toString())
                intent.putExtra("clientId", clientIdText.text.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please Fill all Fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allFieldsAreFilled(): Boolean {

        val timmedBrokerUrlText = brokerUrlText.text.toString().trim()
        val timmedTopicText = topicText.text.toString().trim()
        val timmedClientIdText = clientIdText.text.toString().trim()

        return timmedBrokerUrlText.isNotEmpty() && timmedTopicText.isNotEmpty() && timmedClientIdText.isNotEmpty()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 112 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            button.isEnabled = true
        }
    }
}