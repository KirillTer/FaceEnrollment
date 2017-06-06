package com.intel.faceenrollment;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class FileName extends AppCompatActivity {

    private EditText mEditText;
    private File imageFile;
    private String newFileName;
    private File mImageFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_file_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        Intent intent = getIntent();
        imageFile = (File) intent.getSerializableExtra("oldFileName");
        mImageFolder = (File) intent.getSerializableExtra("imageFolder");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, CameraActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                this.finish();
                return true;
            case R.id.action_check:
                intent = new Intent(this, CameraActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                mEditText = (EditText) findViewById(R.id.editText);
                newFileName = mEditText.getText().toString();

                intent.putExtra("newFileName",newFileName);
                intent.putExtra("oldFileName",imageFile);
                intent.putExtra("imageFolder",mImageFolder);
                startActivity(intent);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.home:
//                Toast.makeText(getApplicationContext(), "Back!", Toast.LENGTH_SHORT).show();
//                return true;
//            case R.id.action_check:
//                Toast.makeText(getApplicationContext(), "Check!", Toast.LENGTH_SHORT).show();
//                return true;
//        }
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_check){
//            Toast.makeText(getApplicationContext(), "Check!", Toast.LENGTH_SHORT).show();
//        }
//        return true;
//    }
}
