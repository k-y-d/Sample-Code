package com.yangdevatca.road.ontariocams;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;

public class WelcomeActivity extends AppCompatActivity implements DatabaseInitializer.InitializeTaskListener {
    private static final String DB_INITED = "Inited";
    private FloatingActionButton fab;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        //...... skip some unrelated code

        fab = (FloatingActionButton) findViewById(R.id.fab);

        boolean inited = isDbInitilized();

        if(inited){
            showFAB();
            return;
        }

        new DatabaseInitializer(this, this).Initialize();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void showFAB(){
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            }
        });

        fab.show();
    }

    boolean isDbInitilized(){
        pref = getPreferences(MODE_PRIVATE);
        return pref.getBoolean(DB_INITED, false);
    }

    @Override
    public void onInitializeTaskFinished(boolean success) {
        if(success) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(DB_INITED, true);
            editor.commit();

            showFAB();
//            Snackbar.make(fab, "Data initialized", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();

            return;
        }

        Snackbar.make(fab, "Initialization failed", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

    }
}
