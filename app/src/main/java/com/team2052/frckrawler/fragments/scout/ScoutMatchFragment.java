package com.team2052.frckrawler.fragments.scout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.common.base.Optional;
import com.team2052.frckrawler.R;
import com.team2052.frckrawler.database.MetricHelper;
import com.team2052.frckrawler.database.MetricValue;
import com.team2052.frckrawler.database.consumer.OnCompletedListener;
import com.team2052.frckrawler.db.Event;
import com.team2052.frckrawler.db.MatchComment;
import com.team2052.frckrawler.db.MatchData;
import com.team2052.frckrawler.db.Metric;
import com.team2052.frckrawler.db.Robot;
import com.team2052.frckrawler.tba.JSON;
import com.team2052.frckrawler.views.metric.MetricWidget;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Adam on 11/26/2015.
 */
public class ScoutMatchFragment extends BaseScoutFragment implements OnCompletedListener {
    public static final int MATCH_GAME_TYPE = 0;
    public static final int MATCH_PRACTICE_TYPE = 1;
    private static String MATCH_TYPE = "MATCH_TYPE";

    TextInputLayout mComments, mMatchNumber;
    View mProgressBar;
    LinearLayout mMetricList;

    private int mMatchType;

    public Observable<MatchScoutData> matchScoutDataObservable(Robot robot) {
        return Observable.just(robot).map(robot1 -> {
            MatchScoutData matchScoutData = new MatchScoutData();
            final int match_num = getMatchNumber();
            final int game_type = mMatchType;

            final QueryBuilder<Metric> metricQueryBuilder
                    = dbManager.getMetricsTable().query(MetricHelper.MetricCategory.MATCH_PERF_METRICS.id, null, mEvent.getGame_id());
            List<Metric> metrics = metricQueryBuilder.list();
            for (Metric metric : metrics) {
                //Query for existing data
                QueryBuilder<MatchData> matchDataQueryBuilder
                        = dbManager.getMatchDataTable().query(robot.getId(), metric.getId(), match_num, game_type, mEvent.getId(), null);
                MatchData currentData = matchDataQueryBuilder.unique();
                //Add the metric values
                matchScoutData.values.add(new MetricValue(metric, currentData == null ? null : JSON.getAsJsonObject(currentData.getData())));
            }

            final QueryBuilder<MatchComment> matchCommentQueryBuilder
                    = dbManager.getMatchComments().query(match_num, game_type, robot.getId(), mEvent.getId());
            MatchComment mMatchComment = matchCommentQueryBuilder.unique();
            if (mMatchComment != null)
                matchScoutData.comments = mMatchComment.getComment();

            //For ux to show the user that data is being loaded and changed
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return matchScoutData;
        });
    }

    public Subscriber<MatchScoutData> matchScoutDataObserver() {
        return new Subscriber<MatchScoutData>() {
            @Override
            public void onCompleted() {
                mProgressBar.setVisibility(View.GONE);
                mMetricList.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStart() {
                mProgressBar.setVisibility(View.VISIBLE);
                mMetricList.setVisibility(View.GONE);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(MatchScoutData matchScoutData) {
                if (mComments.getEditText() != null)
                    mComments.getEditText().setText(matchScoutData.comments);
                setMetrics(matchScoutData.values);
                onCompleted();
            }
        };
    }


    public static class MatchScoutData {
        public String comments = "";
        public List<MetricValue> values = new ArrayList<>();
    }

    public static ScoutMatchFragment newInstance(Event event, int type) {
        ScoutMatchFragment scoutMatchFragment = new ScoutMatchFragment();
        Bundle args = new Bundle();
        args.putInt(MATCH_TYPE, type);
        args.putLong(EVENT_ID, event.getId());
        scoutMatchFragment.setArguments(args);
        return scoutMatchFragment;
    }

    @Override
    public void inject() {
        mComponent.inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMatchType = getArguments().getInt(MATCH_TYPE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scouting_match, null, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mComments = (TextInputLayout) view.findViewById(R.id.comments);
        mMatchNumber = (TextInputLayout) view.findViewById(R.id.match_number_input);
        mMetricList = (LinearLayout) view.findViewById(R.id.metric_widget_list);
        mProgressBar = view.findViewById(R.id.scout_loading_progress);
        binder.mSpinner = (Spinner) view.findViewById(R.id.robot);

        binder.mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public boolean init;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!init) {
                    init = true;
                    return;
                }
                updateMetricList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (mMatchNumber.getEditText() != null)
            mMatchNumber.getEditText().addTextChangedListener(new TextWatcher() {
                boolean init = false;
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(!init) {
                        init = true;
                        return;
                    }
                    try {
                        int value = Integer.parseInt(s.toString());
                        if (value == 0) {
                            mMatchNumber.setErrorEnabled(true);
                            mMatchNumber.setError("You must be an engineer!");
                        } else if (value == 2052) {
                            mMatchNumber.setErrorEnabled(true);
                            mMatchNumber.setError("That's a good tem");
                        } else if (value > 9000) {
                            mMatchNumber.setErrorEnabled(true);
                            mMatchNumber.setError("It's over 9000!");
                        } else {
                            mMatchNumber.setError("");
                            mMatchNumber.setErrorEnabled(false);
                        }
                        updateMetricList();
                    } catch (NumberFormatException e) {
                        mMatchNumber.setErrorEnabled(true);
                        mMatchNumber.setError("Invalid Number");
                    }

                }
            });

        binder.onCompletedListener = this;
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCompleted() {
        updateMetricList();
    }

    public void updateMetricList() {
        if (isSelectedRobotValid() && isMatchNumberValid())
            matchScoutDataObservable(getRobot())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(matchScoutDataObserver());
    }

    private int getMatchNumber() {
        try {
            return Integer.parseInt(mMatchNumber.getEditText().getText().toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isMatchNumberValid() {
        try {
            Integer.parseInt(mMatchNumber.getEditText().getText().toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setMetrics(List<MetricValue> metricValues) {
        mMetricList.removeAllViews();
        for (int i = 0; i < metricValues.size(); i++) {
            Optional<MetricWidget> widget = MetricWidget.createWidget(getActivity(), metricValues.get(i));
            if (widget.isPresent())
                mMetricList.addView(widget.get());
        }
    }
}