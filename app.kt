# ElderlyReminderApp (Kotlin Android) - Project files

---

// README: This is a compact, working skeleton for an Android reminder app tailored to older users.
// It includes: voice input (SpeechRecognizer), text-to-speech (TTS), Room database for reminders/items,
// AlarmManager-based scheduled notifications, and a simple voice command parser.
// Copy each file into an Android Studio project (package: com.example.elderlyreminder).

---

// File: settings.gradle
rootProject.name = "ElderlyReminderApp"
include ':app'

---

// File: build.gradle (Project-level)
// (Use default Android project-level with Gradle plugin)

// File: app/build.gradle
plugins {
    id 'com.android.application'
    id 'kotlin-android'
    id 'kotlin-kapt'
}

android {
    compileSdk 34

    defaultConfig {
        applicationId "com.example.elderlyreminder"
        minSdk 23
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
                targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.11.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.0'

    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'

    // Lifecycle & Coroutines
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // Optional for notification compat
    implementation 'androidx.core:core:1.11.0'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}

---

// File: app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
package="com.example.elderlyreminder">

<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<application
android:allowBackup="true"
android:label="Elderly Reminder"
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
android:supportsRtl="true"
android:theme="@style/Theme.ElderlyReminder">

<activity android:name=".MainActivity">
<intent-filter>
<action android:name="android.intent.action.MAIN" />
<category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
</activity>

<receiver android:name=".AlarmReceiver" android:exported="true" />

</application>
</manifest>

---

// File: app/src/main/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto"
android:layout_width="match_parent"
android:layout_height="match_parent"
android:padding="16dp">

<TextView
android:id="@+id/titleText"
android:layout_width="0dp"
android:layout_height="wrap_content"
android:text="Elderly Reminder"
android:textSize="28sp"
android:textStyle="bold"
android:gravity="center"
app:layout_constraintTop_toTopOf="parent"
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintEnd_toEndOf="parent" />

<Button
android:id="@+id/btnSpeak"
android:layout_width="0dp"
android:layout_height="72dp"
android:text="Talk / Add Reminder"
android:textSize="22sp"
app:layout_constraintTop_toBottomOf="@id/titleText"
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintEnd_toEndOf="parent"
android:layout_marginTop="24dp" />

<Button
android:id="@+id/btnList"
android:layout_width="0dp"
android:layout_height="56dp"
android:text="Show Reminders"
android:textSize="20sp"
app:layout_constraintTop_toBottomOf="@id/btnSpeak"
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintEnd_toEndOf="parent"
android:layout_marginTop="16dp" />

<TextView
android:id="@+id/statusText"
android:layout_width="0dp"
android:layout_height="wrap_content"
android:text="Welcome — press Talk and speak clearly."
android:textSize="18sp"
app:layout_constraintTop_toBottomOf="@id/btnList"
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintEnd_toEndOf="parent"
android:layout_marginTop="20dp" />

</androidx.constraintlayout.widget.ConstraintLayout>

---

// File: app/src/main/res/values/strings.xml
<resources>
<string name="app_name">Elderly Reminder</string>
</resources>

---

// File: app/src/main/java/com/example/elderlyreminder/data/Reminder.kt
package com.example.elderlyreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "task" or "item"
    val title: String,
    val details: String?,
    val timeMillis: Long? // null for simple "item location" entries
)

---

// File: app/src/main/java/com/example/elderlyreminder/data/ReminderDao.kt
package com.example.elderlyreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Query("SELECT * FROM reminders ORDER BY timeMillis IS NULL, timeMillis ASC")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE title LIKE :q LIMIT 1")
    suspend fun findByTitle(q: String): Reminder?

    @Delete
    suspend fun delete(reminder: Reminder)
}

---

// File: app/src/main/java/com/example/elderlyreminder/data/AppDatabase.kt
package com.example.elderlyreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Reminder::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "elderly_reminder_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

---

// File: app/src/main/java/com/example/elderlyreminder/AlarmReceiver.kt
package com.example.elderlyreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val details = intent.getStringExtra("details") ?: ""

        // Simple TTS announcement via service could be added; for now show notification and toast
        val nm = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(details)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        nm.notify((System.currentTimeMillis()%10000).toInt(), builder.build())

        Toast.makeText(context, "$title - $details", Toast.LENGTH_LONG).show()
    }
}

---

// File: app/src/main/java/com/example/elderlyreminder/MainActivity.kt
package com.example.elderlyreminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.elderlyreminder.data.AppDatabase
import com.example.elderlyreminder.data.Reminder
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "reminder_channel"
    }

    private lateinit var btnSpeak: Button
    private lateinit var btnList: Button
    private lateinit var statusText: TextView

    private var tts: TextToSpeech? = null
    private lateinit var db: AppDatabase

    private val requestRecordPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "Record audio permission is needed for voice features", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSpeak = findViewById(R.id.btnSpeak)
        btnList = findViewById(R.id.btnList)
        statusText = findViewById(R.id.statusText)

        db = AppDatabase.getDatabase(this)

        // Request mic permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestRecordPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        tts = TextToSpeech(this, this)

        btnSpeak.setOnClickListener { startListening() }
        btnList.setOnClickListener { showList() }

        createNotificationChannelIfNeeded()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utteranceId")
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        try {
            startActivityForResult(intent, 1001)
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting voice input: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = results?.get(0) ?: ""
            statusText.text = "Heard: $spoken"
            lifecycleScope.launch { handleSpoken(spoken) }
        }
    }

    private suspend fun handleSpoken(spoken: String) {
        // Very simple parser: check for "remind" for tasks; "kept" or "kept my" or "i kept" for item locations; "where is" or "where are" for queries
        val lower = spoken.lowercase(Locale.getDefault())

        when {
            lower.contains("remind me") || lower.contains("remind") -> {
                // Extract title and time naively
                // Examples: "Remind me to take medicine at 8 PM" or "Remind me to call son tomorrow at 10 am"
                val parsed = SimpleSpeechParser.parseReminder(lower)
                val reminder = Reminder(type = "task", title = parsed.title, details = parsed.details, timeMillis = parsed.timeMillis)
                val id = db.reminderDao().insert(reminder)
                val confirmed = "Okay. I'll remind you: ${parsed.title}" + if (parsed.timeMillis != null) " at ${Date(parsed.timeMillis)}" else ""
                speak(confirmed)

                // Schedule alarm if time present
                parsed.timeMillis?.let { scheduleAlarm(it, parsed.title, parsed.details ?: "") }
            }

            lower.contains("i kept") || lower.contains("kept my") || lower.contains("kept the") || lower.contains("placed my") -> {
                val parsed = SimpleSpeechParser.parseLocation(lower)
                val reminder = Reminder(type = "item", title = parsed.title, details = parsed.details, timeMillis = null)
                db.reminderDao().insert(reminder)
                speak("Saved location for ${parsed.title}.")
            }

            lower.contains("where is") || lower.contains("where are") || lower.startsWith("where") -> {
                // extract object
                val q = SimpleSpeechParser.extractQuery(lower)
                val found = db.reminderDao().findByTitle("%$q%")
                if (found != null) {
                    val answer = if (found.type == "item") "You said it's ${found.details}" else "Reminder: ${found.title} at ${found.details}"
                    speak(answer)
                } else {
                    speak("I couldn't find that. You can save it by saying, I kept my $q in the ...")
                }
            }

            else -> {
                speak("Sorry, I didn't understand. Try saying: Remind me to... or I kept my...")
            }
        }
    }

    private fun showList() {
        // Launch a simple list activity or show a toast summary
        lifecycleScope.launch {
            val list = db.reminderDao().getAll()
            // getAll returns Flow - we can collect the first emission by transforming via coroutine - but Room Flow needs collection
            // For brevity, show a simple text via Toast; in full app you'd build a RecyclerView.
            // Here we'll query by a simple query: not available directly - so this is simplified.
            Toast.makeText(this@MainActivity, "Open Reminders list feature (implement RecyclerView)", Toast.LENGTH_LONG).show()
        }
    }

    private fun scheduleAlarm(timeMillis: Long, title: String, details: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("details", details)
        }
        val pending = PendingIntent.getBroadcast(this, (timeMillis % Int.MAX_VALUE).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Use setExactAndAllowWhileIdle for critical reminders
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pending)

        speak("Reminder scheduled.")
    }

    private fun createNotificationChannelIfNeeded() {
        // Create notification channel for API 26+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Reminders"
            val desc = "Reminder notifications"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply { description = desc }
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}

---

// File: app/src/main/java/com/example/elderlyreminder/SimpleSpeechParser.kt
package com.example.elderlyreminder

import java.text.SimpleDateFormat
import java.util.*

object SimpleSpeechParser {
    // VERY SIMPLE parsing heuristics for demo purposes. Replace with proper NLP/Regex for production.

    data class ParsedReminder(val title: String, val details: String?, val timeMillis: Long?)
    data class ParsedLocation(val title: String, val details: String?)

    fun parseReminder(lower: String): ParsedReminder {
        // naive: split by " at " to find time
        var timeMillis: Long? = null
        var details: String? = null
        var title = lower

        if (lower.contains(" at ")) {
            val parts = lower.split(" at ")
            title = parts[0].replace("remind me to", "").replace("remind me", "").trim()
            val timePart = parts[1]
            val parsedTime = tryParseTime(timePart)
            timeMillis = parsedTime
            details = timePart
        } else if (lower.contains(" tomorrow ") || lower.contains(" tomorrow")) {
            // naive next-day at default time 9 AM
            title = lower.replace("remind me to", "").replace("remind me", "").replace("tomorrow", "").trim()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            timeMillis = cal.timeInMillis
            details = "Tomorrow 9:00"
        } else {
            title = lower.replace("remind me to", "").replace("remind me", "").trim()
            details = null
            timeMillis = null
        }

        // Capitalize first letter
        val cleanedTitle = title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return ParsedReminder(cleanedTitle, details, timeMillis)
    }

    fun parseLocation(lower: String): ParsedLocation {
        // Example: "i kept my glasses on the table" -> title: glasses, details: on the table
        var title = "item"
        var details: String? = null

        val keepVariants = listOf("i kept my", "i kept the", "kept my", "kept the", "placed my", "placed the")
        for (v in keepVariants) {
            if (lower.contains(v)) {
                val after = lower.substring(lower.indexOf(v) + v.length).trim()
                // assume format "<object> in/on/at <place>"
                val tokens = after.split(" in ", limit = 2)
                if (tokens.size == 2) {
                    title = tokens[0].trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    details = "in ${tokens[1].trim()}"
                    break
                }
                val tokens2 = after.split(" on ", limit = 2)
                if (tokens2.size == 2) {
                    title = tokens2[0].trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    details = "on ${tokens2[1].trim()}"
                    break
                }

                // fallback: everything after is details
                title = after.split(" ").firstOrNull() ?: "item"
                details = after
                break
            }
        }
        return ParsedLocation(title.replace(Regex("[^a-zA-Z0-9 ]"), ""), details)
    }

    fun extractQuery(lower: String): String {
        // "where is my keys" -> extract last words
        // naive: remove where is/where are
        var q = lower
        q = q.replace("where is", "").replace("where are", "").replace("my", "").replace("the", "").trim()
        return q
    }

    private fun tryParseTime(timeText: String): Long? {
        // Try parse formats like "8 pm", "8:30 AM", "10 am"
        val today = Calendar.getInstance()
        val formats = listOf("h a", "hh a", "h:mm a", "hh:mm a")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val d = sdf.parse(timeText.uppercase(Locale.getDefault()))
                if (d != null) {
                    val timeCal = Calendar.getInstance()
                    timeCal.time = d
                    val result = Calendar.getInstance()
                    result.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                    result.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                    result.set(Calendar.SECOND, 0)
                    if (result.timeInMillis < System.currentTimeMillis()) result.add(Calendar.DAY_OF_YEAR, 1)
                    return result.timeInMillis
                }
            } catch (e: Exception) {
                // ignore and try next
            }
        }
        return null
    }
}

---

// Notes & Next Steps:
// 1) This code is a compact demo. For a production-grade app:
//    - Improve Natural Language Parsing (use ML models or Dialogflow/Rasa).
//    - Implement RecyclerView to show reminders and allow editing/deleting.
//    - Add persistent notification & foreground service if necessary.
//    - Improve accessibility: large fonts, big buttons, haptic feedback, confirm dialogs.
//    - Add fallback offline STT (Vosk) if needed.
// 2) To build APK: Open Android Studio, paste files into appropriate project structure, sync Gradle, run on device.

---

// End of project skeleton
