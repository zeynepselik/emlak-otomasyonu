package com.diegomfv.android.realestatemanager.ui.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.diegomfv.android.realestatemanager.R;
import com.diegomfv.android.realestatemanager.constants.Constants;
import com.diegomfv.android.realestatemanager.data.entities.RealEstate;
import com.diegomfv.android.realestatemanager.ui.base.BaseActivity;
import com.diegomfv.android.realestatemanager.ui.fragments.FragmentItemDescription;
import com.diegomfv.android.realestatemanager.ui.fragments.FragmentListListings;
import com.diegomfv.android.realestatemanager.util.ToastHelper;
import com.diegomfv.android.realestatemanager.util.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.diegomfv.android.realestatemanager.util.Utils.setOverflowButtonColor;


public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.toolbar_id)
    Toolbar toolbar;

    @BindView(R.id.textView_please_insert_data_id)
    TextView tvInsertData;

    @BindView(R.id.fragment1_container_id)
    FrameLayout fragment1Layout;


    private boolean editModeActive;


    private boolean mainMenu;

    private boolean deviceIsHandset;

    private int currency;

    private Unbinder unbinder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: called!");

        this.editModeActive = false;

        this.updateMainMenu();



        if (mainMenu) {
            getRepository().deleteCache();
        }

        this.currency = Utils.readCurrentCurrencyShPref(this);


        ifTabletObligateLandscape();


        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        deviceIsHandset = updateDeviceIsHandset();

        this.configureToolBar();

        this.loadFragmentOrFragments();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: called!");
        unbinder.unbind();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: called!");


        if (mainMenu) {
            getMenuInflater().inflate(R.menu.main_menu, menu);

        } else {
            getMenuInflater().inflate(R.menu.currency_menu, menu);
        }

        Utils.updateCurrencyIconWhenMenuCreated(this, currency, menu, R.id.menu_change_currency_button);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: called!");

        switch (item.getItemId()) {

            case R.id.menu_add_listing_button: {
                Utils.launchActivity(this, CreateNewListingActivity.class);
            }
            break;

            case R.id.menu_change_currency_button: {
                changeCurrency();
                Utils.updateCurrencyIcon(this, currency, item);

            }
            break;

            case R.id.menu_edit_listing_button: {
                updateMode();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called!");

        switch (requestCode) {

            case Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE: {

                if (grantResults.length > 0 && grantResults[0] != -1) {
                    createDirectories();
                }
            }
            break;
        }
    }


    public boolean getMainMenu() {
        Log.d(TAG, "getMainMenu: called!");
        return mainMenu;
    }
    public boolean getDeviceIsHandset() {
        Log.d(TAG, "getDeviceIsHandset: called!");
        return deviceIsHandset;
    }
    private void updateMainMenu() {
        Log.d(TAG, "updateMainMenu: called!");
        Log.w(TAG, "updateMainMenu: getIntent.getExtras() = " + getIntent().getExtras());
        mainMenu = getIntent().getExtras() == null;
    }


    private boolean updateDeviceIsHandset() {
        Log.d(TAG, "updateDeviceIsHandset: called!");
        return findViewById(R.id.fragment2_container_id) == null;
    }


    private void configureToolBar() {
        Log.d(TAG, "configureToolBar: called!");
        setSupportActionBar(toolbar);
        setOverflowButtonColor(toolbar, Color.WHITE);

        setToolbarTitle();

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: called!");
                if (mainMenu) {
                    launchAreYouSureDialog();
                } else {
                    Utils.launchActivity(MainActivity.this, SearchEngineActivity.class);
                }
            }
        });
    }


    private void setToolbarTitle() {
        Log.d(TAG, "setToolbarTitle: called!");
        if (mainMenu) {
            toolbar.setTitle("Main Menu");
        } else {
            toolbar.setTitle("Found Listings");
        }
    }


    public boolean getEditModeActive() {
        Log.d(TAG, "getEditMode: called!");
        return editModeActive;
    }


    private void updateMode() {
        Log.d(TAG, "updateMode: called!");

        if (!editModeActive) {
            toolbar.setTitle("Edit mode");
            toolbar.setSubtitle("Click an element");
            editModeActive = true;
        } else {
            toolbar.setTitle("Real Estate Manager");
            toolbar.setSubtitle(null);
            editModeActive = false;
        }
    }


    private void changeCurrency() {
        Log.d(TAG, "changeCurrency: called!");

        if (this.currency == 0) {
            this.currency = 1;
        } else {
            this.currency = 0;
        }
        Utils.writeCurrentCurrencyShPref(this, currency);
        loadFragmentOrFragments();
    }


    @SuppressLint("CheckResult")
    private void loadFragmentOrFragments() {
        Log.d(TAG, "loadFragmentOrFragments: called!");

        getRepository().getAllListingsRealEstateObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<List<RealEstate>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: called!");

                    }

                    @Override
                    public void onNext(List<RealEstate> realEstates) {
                        Log.d(TAG, "onNext: " + realEstates);

                        if (!realEstates.isEmpty()) {

                            /* We hide the TextView that is displayed whent there are no listings
                             * yet in the database
                             * */
                            hideTextViewShowFragments();

                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment1_container_id, FragmentListListings.newInstance())
                                    .commit();

                            if (!deviceIsHandset) {

                                /* If the device is a tablet, we load the second fragment
                                 * */
                                getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment2_container_id, FragmentItemDescription.newInstance())
                                        .commit();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());

                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: called!");

                    }
                });
    }

    private void createDirectories() {
        Log.d(TAG, "createDirectories: called");
        if (!getInternalStorage().isDirectoryExists(getImagesDir())) {
            getInternalStorage().createDirectory(getImagesDir());
        }

        if (!getInternalStorage().isDirectoryExists(getTemporaryDir())) {
            getInternalStorage().createDirectory(getTemporaryDir());
        }
    }

    private void hideTextViewShowFragments() {
        Log.d(TAG, "hideTextViewData: called!");
        tvInsertData.setVisibility(View.GONE);
        fragment1Layout.setVisibility(View.VISIBLE);
        if (findViewById(R.id.fragment2_container_id) != null) {
            findViewById(R.id.fragment2_container_id).setVisibility(View.VISIBLE);
        }

    }

    private void launchAreYouSureDialog() {
        Log.d(TAG, "launchAreYouSureDialog: called!");
        Utils.launchSimpleDialog(this,
                "Ayrılmak istediğine emin misin?",
                "Oturum Kapatılıyor",
                "Evet",
                "Hayır",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: called!");
                        Utils.launchActivityClearStack(MainActivity.this, AuthLoginActivity.class);
                    }
                });
    }


    private void ifTabletObligateLandscape() {
        Log.d(TAG, "ifTabletObligateLandscape: called!");
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
}
