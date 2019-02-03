package android.example.qhacksv4;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.File;
import android.support.v4.content.FileProvider;
import android.net.Uri;
import java.text.SimpleDateFormat;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    int mPhotos;
    ArrayList<String> mPaths;
    ArrayList<String> train = new ArrayList<>();
    ArrayList<String> evaluate = new ArrayList<>();
    int mCameras;
    boolean truth;
    private FirebaseDatabase mref;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        mPaths = new ArrayList<>();
        setContentView(R.layout.activity_main);
    }

    public void add(View view) {
        mCameras = 0;
        mPhotos= 3;
        mPaths = new ArrayList<>();
        train = new ArrayList<>();
        truth = true;
        dispatch(mCameras);
        trainFile();
        setContentView(R.layout.activity_main);
    }

    private void dispatch(int count) {
        Intent intent;
        File image;
        Uri uri;
        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            image = null;
            try {
                image = create(count);
            } catch (IOException ex) {
            }
            if (image != null) {
                uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", image);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, 1);
            }
        }
    }

    private File create(int count) throws IOException {
        String time;
        String name;
        File dir;
        File file;
        time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        name = "JPEG_" + Integer.toString(count) + "_" + time + "_";
        dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        file = File.createTempFile(name, ".jpg", dir);
        mPaths.add(file.getAbsolutePath());
        return file;
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (mCameras < mPhotos - 1) {
            dispatch(++mCameras);
        } else {
            storeInGallery();
        }
    }

    private void storeInGallery() {
        String path;
        Intent intent;
        File file;
        Uri uri;
        for (int i = 0; i < mPaths.size(); i++) {
            path = mPaths.get(i);
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            file = new File(path);
            uri = Uri.fromFile(file);
            intent.setData(uri);
            this.sendBroadcast(intent);
            storeInFireBase(uri);
        }
    }

    public void storeInFireBase(Uri file) {
        FirebaseStorage storage;
        StorageReference reference;
        StorageReference image;
        String timeStamp;
        storage = FirebaseStorage.getInstance();
        reference = storage.getReference();
        timeStamp = timeStamp();
        if (truth) {
            image = reference.child("/train/" + timeStamp + ".jpg");
            train.add(timeStamp);
            trainFile();
        }
        else {
            image = reference.child("/evaluate/" + timeStamp + ".jpg");
            evaluate.add(timeStamp);
            evaluateFile();
        }
        image.putFile(file);
    }

    public void scan(View view) {
        mCameras = 0;
        mPhotos = 1;
        mPaths = new ArrayList<>();
        evaluate = new ArrayList<>();
        truth = false;
        dispatch(mCameras);
        evaluateFile();
        result();
        setContentView(R.layout.activity_main);
    }

    public void result() {
    }

    public void display(double value) {
        double value2;
        if (value > 1) {
            return;
        }
        else {
            value2 = (1 - value);
            if (value2 >= 0.65) {
                ConstraintLayout currentLayout = findViewById(R.id.background);
                currentLayout.setBackgroundColor(Color.argb(10, 0, 255, 0));
                Button button = findViewById(R.id.result);
                button.setText((value * 100) + "%");
            }
            else {
                ConstraintLayout currentLayout = findViewById(R.id.background);
                currentLayout.setBackgroundColor(Color.argb(10, 255, 0, 0));
                Button button = findViewById(R.id.result);
                button.setText((value * 100) + "%");
            }
        }
    }

    public String timeStamp() {
        Date date;
        long time;
        Timestamp stamp;
        String string;
        date = new Date();
        time = date.getTime();
        stamp = new Timestamp(time);
        string = stamp.toString();
        return string;
    }

    public void trainFile() {
        String content;
        content = new String();
        FirebaseDatabase database;
        DatabaseReference myRef;
        for (String element : train) {
            content = content + element + ",";
        }
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("csv");
        myRef.setValue(content);
    }

    public void evaluateFile() {
        String content;
        content = new String();
        FirebaseDatabase database;
        DatabaseReference myRef;
        for (String element : evaluate) {
            content = content + element + ",";
        }
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("csv");
        myRef.setValue(content);
    }
}