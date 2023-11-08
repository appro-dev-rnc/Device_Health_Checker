package com.approdevelopers.devicehealthchecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.BuildConfig;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import com.scottyab.rootbeer.RootBeer;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class PerformCheckActivity extends AppCompatActivity {


    TextView txtCheckStatusFunc,txtCheckStatusAvailable,txtCheckStatusWorking;
    ImageView imgCurrentCheckBanner,imgLoadingAlert;

    SensorManager sensorManager;

    List<Dev_Functionality> functionalities;
    int currentPosition =0;

    List<FunctionalityCheck> dataList;

    FunctionalityCheckDataModel report;

    private static final String WORKING = "Working";
    private static final String NOT_WORKING = "Not Working";
    private static final String AVAILABLE = "Available";
    private static final String UNAVAILABLE = "Unavailable";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perform_check);



        //hooks
        txtCheckStatusFunc = findViewById(R.id.txt_current_check_functionality);
        txtCheckStatusAvailable = findViewById(R.id.txt_current_check_availability);
        txtCheckStatusWorking = findViewById(R.id.txt_current_check_working);
        imgCurrentCheckBanner = findViewById(R.id.img_current_check_banner);
        imgLoadingAlert = findViewById(R.id.img_loading_alert);

         functionalities = new ArrayList<>();
         functionalities.add(Dev_Functionality.MICROPHONE);
         functionalities.add(Dev_Functionality.ROOTED);
         functionalities.add(Dev_Functionality.BLUETOOTH);
         functionalities.add(Dev_Functionality.ACCELEROMETER);
         functionalities.add(Dev_Functionality.GYROSCOPE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        report = new FunctionalityCheckDataModel();
        dataList = new ArrayList<>();
        analyseAndPerformNextCheck();

    }


    private void analyseAndPerformNextCheck(){
        if (currentPosition<functionalities.size()){
            txtCheckStatusAvailable.setText("");
            txtCheckStatusWorking.setText("");
            Dev_Functionality functionality  = functionalities.get(currentPosition);
            performChecks(functionality);
        }else {
            Toast.makeText(this, "All Checks Performed", Toast.LENGTH_SHORT).show();
            report.setFunctionalityChecks(dataList);
            report.setTimestamp(Timestamp.now().toString());
            showReportOptions();
        }
    }

    private void showReportOptions() {

        // In your activity or fragment
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View customView = inflater.inflate(R.layout.report_options_layout, null);

        builder.setView(customView);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // You can now find and manipulate views in your custom layout
        MaterialCardView pdfCard = customView.findViewById(R.id.pdf_card);
        MaterialCardView cloudCard = customView.findViewById(R.id.cloud_card);

        pdfCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(PerformCheckActivity.this, "Generate pdf", Toast.LENGTH_SHORT).show();
                checkStoragePermissions();
                alertDialog.dismiss(); // Close the dialog
            }
        });
        cloudCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(PerformCheckActivity.this, "Send data to cloud", Toast.LENGTH_SHORT).show();
                sendDataToCloud();
                alertDialog.dismiss(); // Close the dialog
            }
        });

    }

    private void checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // You have the permission, proceed with PDF creation and saving.
            generatePdf();

        } else {
            // Request permission from the user.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1001){
            if (grantResults[0]==(PackageManager.PERMISSION_GRANTED)){
                generatePdf();
            }else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 1002) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can access the microphone

            } else {
                // Permission denied, handle this case gracefully (e.g., show a message or disable microphone-related features)
            }
        }
    }

    private void generatePdf() {

        String outputPath = Environment.getExternalStorageDirectory() + "/"+"Device_health"+report.getTimestamp()+".pdf";
        PdfDocument pdfDoc = null;
        try {
            pdfDoc = new PdfDocument(new PdfWriter(outputPath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Document doc = new Document(pdfDoc);

        Paragraph title = new Paragraph("DEVICE HEALTH CHECKER")
                .setFontSize(20); // Adjust the font size as needed
        title.setHorizontalAlignment(HorizontalAlignment.CENTER);


        doc.add(title);


        Table table = new Table(new float[] {2f, 3f, 2f}); // Example: 3 columns with relative widths
        table.setWidth(300); // Table width is 100% of the page width
        table.setHorizontalAlignment(HorizontalAlignment.CENTER);
        table.setVerticalAlignment(VerticalAlignment.MIDDLE);// Adjust alignment as needed

        Cell cellFunctionality = new Cell().add(new Paragraph("Functionality"));
        Cell cellAvailability = new Cell().add(new Paragraph("Availability"));
        Cell cellWorking = new Cell().add(new Paragraph("Working Status"));
        table.addCell(cellFunctionality);
        table.addCell(cellAvailability);
        table.addCell(cellWorking);

        Log.i("statuscheck", "generatePdf: "+ report.getFunctionalityChecks().toString());
        for (FunctionalityCheck data : report.getFunctionalityChecks()) {
            Cell cell1 = new Cell().add(new Paragraph(data.getTitle()));
            Cell cell2 = new Cell().add(new Paragraph(data.getAvailability_status()));
            Cell cell3 = new Cell().add(new Paragraph(data.getWorking_status()));

            Log.i("statuscheck", "generatePdf: "+ data.getWorking_status());

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
        }


        doc.add(table);

        doc.close();

        showDialogToSharePdf(outputPath);


    }

    private void showDialogToSharePdf(String outputPath) {
        new AlertDialog.Builder(this)
                .setTitle("Pdf Generated")
                .setMessage("Choose an action to continue")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton("View", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with view operation
                        // Assuming 'outputPath' contains the path to the generated PDF
                        File pdfFile = new File(outputPath);

                        if (pdfFile.exists()) {
                            Uri pdfUri = FileProvider.getUriForFile(PerformCheckActivity.this, getApplication().getPackageName()+".provider", pdfFile);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(pdfUri, "application/pdf");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Ensure read permission
                            startActivity(intent);
                        }

                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton("Share", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File pdfFile = new File(outputPath);

                        if (pdfFile.exists()) {
                            Uri pdfUri = FileProvider.getUriForFile(PerformCheckActivity.this, getApplication().getPackageName()+".provider", pdfFile);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
                            startActivity(Intent.createChooser(intent, "Share PDF"));
                        }

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }



    private void sendDataToCloud() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Device Health Reports")
                .add(report)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(PerformCheckActivity.this, "Report uploaded", Toast.LENGTH_SHORT).show();
                            
                        }else {
                            Toast.makeText(PerformCheckActivity.this, "Failed to upload report", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void performChecks(Dev_Functionality functionality) {

        switch (functionality){
            case BLUETOOTH:

                new AlertDialog.Builder(PerformCheckActivity.this)
                        .setTitle("Bluetooth Functionality")
                        .setMessage("Turn on Bluetooth and press Continue")
                        .setCancelable(false)
                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton("Continue", (dialog, which) -> {
                            // Continue with delete operation
                            imgLoadingAlert.setImageResource(R.drawable.loading);
                            txtCheckStatusFunc.setText("Checking Bluetooth");
                            performBluetoothCheck();
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton("Deny", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

                break;

            case ACCELEROMETER:
                imgLoadingAlert.setImageResource(R.drawable.loading);
                txtCheckStatusFunc.setText("Checking Accelerometer");
                performAccelerometerCheck();

                break;
            case GYROSCOPE:
                imgLoadingAlert.setImageResource(R.drawable.loading);
                txtCheckStatusFunc.setText("Checking Gyroscope");
                performGyroScopeCheck();
                break;
            case MICROPHONE:
                imgLoadingAlert.setImageResource(R.drawable.loading);
                txtCheckStatusFunc.setText("Checking Microphone");
                performMicrophoneCheck();
                break;
            case ROOTED:
                imgLoadingAlert.setImageResource(R.drawable.loading);
                txtCheckStatusFunc.setText("Checking Root");
                performRootCheck();
                break;
        }

    }

    private void performRootCheck() {
        FunctionalityCheck rootCheck = new FunctionalityCheck();
        rootCheck.setTitle("Rooted");

        RootBeer rootBeer = new RootBeer(this);

        if (rootBeer.isRooted()) {
            imgLoadingAlert.setImageResource(R.drawable.checked);
            txtCheckStatusAvailable.setText("Rooted");

            rootCheck.setAvailability_status("Rooted");
            rootCheck.setWorking_status("-");
        } else {
            imgLoadingAlert.setImageResource(R.drawable.warning);
            txtCheckStatusAvailable.setText("Unrooted");
            rootCheck.setAvailability_status("Unrooted");
            rootCheck.setWorking_status("-");
        }
        dataList.add(rootCheck);
        currentPosition++;
        analyseAndPerformNextCheck();


    }

    private void performMicrophoneCheck() {
        FunctionalityCheck microphoneCheck = new FunctionalityCheck();
        microphoneCheck.setTitle("Microphone");

        if (getPackageManager().hasSystemFeature( PackageManager.FEATURE_MICROPHONE)) {
            //Microphone is present on the device
            txtCheckStatusAvailable.setText("Microphone Available");
            microphoneCheck.setAvailability_status(AVAILABLE);

            checkAudioPermission(microphoneCheck);


        }else {
            imgLoadingAlert.setImageResource(R.drawable.warning);
            txtCheckStatusAvailable.setText("Microphone Unavailable");
            microphoneCheck.setAvailability_status(UNAVAILABLE);
            microphoneCheck.setWorking_status("-");
            dataList.add(microphoneCheck);
            currentPosition++;
            analyseAndPerformNextCheck();
        }

    }

    private void checkMic(FunctionalityCheck mic){
        String outputPath = Environment.getExternalStorageDirectory() + "/"+"Test_audio_"+report.getTimestamp()+".mp3";

        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(outputPath); // Optional
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any exceptions that may occur during preparation and recording
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaRecorder != null) {
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    mediaRecorder.release();
                }
                File audioFile = new File(outputPath);
                boolean isMicrophoneWorking = audioFile.exists() && audioFile.length() > 0;
                if (isMicrophoneWorking){

                    txtCheckStatusWorking.setText(WORKING);
                    imgLoadingAlert.setImageResource(R.drawable.checked);
                    mic.setWorking_status(WORKING);
                    dataList.add(mic);
                    currentPosition++;
                    analyseAndPerformNextCheck();
                }else {

                    imgLoadingAlert.setImageResource(R.drawable.warning);
                    txtCheckStatusWorking.setText(NOT_WORKING);
                    dataList.add(mic);
                    currentPosition++;
                    analyseAndPerformNextCheck();
                }
            }
        }, 2000);





    }

    private void checkAudioPermission(FunctionalityCheck mic) {
        // Check if permissions are granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Request permission to access the microphone
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 1002);
        }else {
            checkMic(mic);

        }

    }

    private void performGyroScopeCheck() {

        FunctionalityCheck gyroCheck = new FunctionalityCheck();
        gyroCheck.setTitle("Gyroscope");

        imgCurrentCheckBanner.setImageResource(R.drawable.gyroscope);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope!=null){
            txtCheckStatusAvailable.setText("Gyroscope Available");
            gyroCheck.setAvailability_status(AVAILABLE);
            Toast.makeText(this, "Move your phone to check accelerometer working", Toast.LENGTH_SHORT).show();
            final boolean[] isSensorWorking = {false};
            SensorEventListener eventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.values!=null){
                        isSensorWorking[0] = true;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            };
            
            sensorManager.registerListener(eventListener,gyroscope,SensorManager.SENSOR_DELAY_NORMAL);
            new Handler().postDelayed(() -> {
                if (isSensorWorking[0]){
                    txtCheckStatusWorking.setText("GyroScope working");
                    imgLoadingAlert.setImageResource(R.drawable.checked);
                    gyroCheck.setWorking_status(WORKING);
                }else {
                    txtCheckStatusWorking.setText("GyroScope not working");
                    imgLoadingAlert.setImageResource(R.drawable.warning);
                    gyroCheck.setWorking_status(NOT_WORKING);
                }

                sensorManager.unregisterListener(eventListener);
                dataList.add(gyroCheck);
                currentPosition++;
                analyseAndPerformNextCheck();
            },5000);
        }else {
            imgLoadingAlert.setImageResource(R.drawable.warning);
            txtCheckStatusAvailable.setText("Gyroscope not available");
            gyroCheck.setAvailability_status(UNAVAILABLE);
            gyroCheck.setWorking_status("-");
            dataList.add(gyroCheck);
            currentPosition++;
            analyseAndPerformNextCheck();
        }


    }

    private void performAccelerometerCheck() {

        FunctionalityCheck accelerometerCheck = new FunctionalityCheck();
        accelerometerCheck.setTitle("Accelerometer");
        imgCurrentCheckBanner.setImageResource(R.drawable.accelerometer_sensor);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer!=null){
            txtCheckStatusAvailable.setText("Accelerometer Available");
            accelerometerCheck.setAvailability_status(AVAILABLE);
            Toast.makeText(this, "Move your phone to check accelerometer working", Toast.LENGTH_SHORT).show();
            final boolean[] isSensorWorking = {false};
            SensorEventListener eventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.values!=null){
                        isSensorWorking[0] = true;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            };

            sensorManager.registerListener(eventListener,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
            new Handler().postDelayed(() -> {
                if (isSensorWorking[0]){
                    txtCheckStatusWorking.setText("Accelerometer working");
                    imgLoadingAlert.setImageResource(R.drawable.checked);
                    accelerometerCheck.setWorking_status(WORKING);
                }else {
                    txtCheckStatusWorking.setText("Accelerometer not working");
                    imgLoadingAlert.setImageResource(R.drawable.warning);
                    accelerometerCheck.setWorking_status(NOT_WORKING);
                }

                sensorManager.unregisterListener(eventListener);
                dataList.add(accelerometerCheck);
                currentPosition++;
                analyseAndPerformNextCheck();
            },5000);
        }else {
            imgLoadingAlert.setImageResource(R.drawable.warning);
            txtCheckStatusAvailable.setText("Accelerometer not available");
            accelerometerCheck.setAvailability_status(UNAVAILABLE);
            accelerometerCheck.setWorking_status("-");
            dataList.add(accelerometerCheck);
            currentPosition++;
            analyseAndPerformNextCheck();
        }

    }

    private void performBluetoothCheck() {
        FunctionalityCheck bluetoothFunc = new FunctionalityCheck();
        bluetoothFunc.setTitle("Bluetooth");

        imgCurrentCheckBanner.setImageResource(R.drawable.bluetooth);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            txtCheckStatusAvailable.setText("Bluetooth Available");
            bluetoothFunc.setAvailability_status(AVAILABLE);

            if (bluetoothAdapter.isEnabled()) {
                //Bluetooth is available and enabled.
                txtCheckStatusWorking.setText("Bluetooth Working");
                imgLoadingAlert.setImageResource(R.drawable.checked);
                bluetoothFunc.setWorking_status(WORKING);

            } else {
                //Bluetooth is available but not enabled.
                imgLoadingAlert.setImageResource(R.drawable.warning);
                txtCheckStatusWorking.setText("Bluetooth Not Working");
                bluetoothFunc.setWorking_status(NOT_WORKING);
            }
            dataList.add(bluetoothFunc);

        } else {
            imgLoadingAlert.setImageResource(R.drawable.warning);
            txtCheckStatusAvailable.setText("Bluetooth Unavailable");
            bluetoothFunc.setAvailability_status(UNAVAILABLE);
            bluetoothFunc.setWorking_status("-");
            dataList.add(bluetoothFunc);
        }
        
        currentPosition++;
        analyseAndPerformNextCheck();
    }
}