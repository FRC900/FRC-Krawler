package com.team2052.frckrawler.fragments;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.team2052.frckrawler.FRCKrawler;
import com.team2052.frckrawler.R;
import com.team2052.frckrawler.activities.AddMetricActivity;
import com.team2052.frckrawler.activities.ImportMetricsActivity;
import com.team2052.frckrawler.activities.MetricInfoActivity;
import com.team2052.frckrawler.activities.SetupActivity;
import com.team2052.frckrawler.binding.ListViewNoDataParams;
import com.team2052.frckrawler.binding.RecyclerViewBinder;
import com.team2052.frckrawler.db.Metric;
import com.team2052.frckrawler.listeners.FABButtonListener;
import com.team2052.frckrawler.listitems.smart.MetricItemView;
import com.team2052.frckrawler.listitems.smart.SmartAdapterInteractions;
import com.team2052.frckrawler.subscribers.MetricListSubscriber;
import com.team2052.frckrawler.util.MetricHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import io.nlopez.smartadapters.SmartAdapter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static android.app.Activity.RESULT_OK;

/**
 * @author Adam
 * @since 10/15/2014
 */
public class MetricsFragment extends RecyclerViewFragment<List<Metric>, MetricListSubscriber, RecyclerViewBinder> implements FABButtonListener {
    private static final String CATEGORY_EXTRA = "CATEGORY_EXTRA";
    private static final String GAME_ID = "GAME_ID";
    private long mGame_id;
    private int mCategory;

    public static MetricsFragment newInstance(int category, long game_id) {
        MetricsFragment fragment = new MetricsFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(GAME_ID, game_id);
        bundle.putInt(CATEGORY_EXTRA, category);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGame_id = getArguments().getLong(GAME_ID, 0);
        mCategory = getArguments().getInt(CATEGORY_EXTRA);

        if (mCategory == MetricHelper.MATCH_PERF_METRICS) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 12345 && resultCode==RESULT_OK) {
            Uri uri = data.getData();


            File currentDB = getContext().getDatabasePath("frc-krawler-database-v3");

            if (currentDB.exists()) {
                try {

                    File backupDB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"temp_scouting_db");
                    FileOutputStream fos = new FileOutputStream(backupDB);

                    InputStream is = getContext().getContentResolver().openInputStream(uri);
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    try {
                        len = is.read(buffer);
                        while (len != -1) {
                            fos.write(buffer, 0, len);
                            len = is.read(buffer);
                        }

                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                    FileChannel dst = new FileOutputStream(currentDB).getChannel();
                    FileChannel src = new FileInputStream(backupDB).getChannel();
                    dst.truncate(0);
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();

                    AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                    alertDialog.setTitle("Import Database");
                    alertDialog.setMessage("Database Imported Successfully");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                }catch(IOException e) {

                    AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                    alertDialog.setTitle("Oh Fuck an Error");
                    alertDialog.setMessage(e.getMessage());
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();


                }
            }

            Intent mStartActivity = new Intent(getContext(), SetupActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(getContext(), mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager)getContext().getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            android.os.Process.killProcess(android.os.Process.myPid());

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.metric_import_firebase, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.import_metrics_menu) {
            startActivity(ImportMetricsActivity.newInstance(getContext(), mGame_id, mCategory));
        }
        if (item.getItemId() == R.id.import_data_menu) {
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 12345);

        }
        if(item.getItemId() == R.id.export_data_menu) {
            File backupDB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ScoutingProfile.db"); // for example "my_data_backup.db"
            File currentDB = getContext().getDatabasePath("frc-krawler-database-v3");
            if (currentDB.exists()) {
                try {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }catch(IOException e) {}
            }
            MediaScannerConnection.scanFile(getActivity(), new String[]{backupDB.getAbsolutePath()}, null, null);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(backupDB));
            shareIntent.setType("file/csv");
            startActivity(Intent.createChooser(shareIntent, "Share CSV with..."));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void inject() {
        mComponent.inject(this);
    }

    @Override
    protected Observable<? extends List<Metric>> getObservable() {
        return rxDbManager.metricsInGame(mGame_id, mCategory);
    }

    @Override
    public void onFABPressed() {
        startActivity(AddMetricActivity.newInstance(getActivity(), mGame_id, mCategory));
    }

    @Override
    protected ListViewNoDataParams getNoDataParams() {
        return new ListViewNoDataParams("No metrics found", R.drawable.ic_metric);
    }

    @Override
    public void provideAdapterCreator(SmartAdapter.MultiAdaptersCreator creator) {
        creator.map(Metric.class, MetricItemView.class);
        creator.listener((actionId, item, position, view) -> {
            if (actionId == SmartAdapterInteractions.EVENT_CLICKED && item instanceof Metric) {
                Metric metric = (Metric) item;
                startActivity(MetricInfoActivity.newInstance(getActivity(), metric.getId()));
            }
        });
    }
}
