package com.team2052.frckrawler.activities;

import android.os.Bundle;

import com.team2052.frckrawler.FRCKrawler;
import com.team2052.frckrawler.database.DBManager;
import com.team2052.frckrawler.di.DaggerFragmentComponent;
import com.team2052.frckrawler.di.FragmentComponent;

import javax.inject.Inject;

/**
 * Created by Acorp on 11/17/2015.
 */
public abstract class DatabaseActivity extends BaseActivity implements HasComponent {
    FragmentComponent mComponent;
    @Inject DBManager dbManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject();
    }

    @Override
    public FragmentComponent getComponent() {
        if (mComponent == null) {
            FRCKrawler app = (FRCKrawler) getApplication();
            mComponent = DaggerFragmentComponent
                    .builder()
                    .fRCKrawlerModule(app.getModule())
                    .applicationComponent(app.getComponent())
                    .build();
        }
        return mComponent;
    }

    public abstract void inject();
}