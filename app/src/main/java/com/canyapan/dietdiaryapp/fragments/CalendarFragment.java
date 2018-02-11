package com.canyapan.dietdiaryapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.canyapan.dietdiaryapp.CreateEditEventActivity;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.models.Event;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.text.MessageFormat;

public class CalendarFragment extends Fragment implements ViewPager.OnPageChangeListener {
    public static final String TAG = "CalendarFragment";

    private static final String KEY_DATE_SERIALIZABLE = "DATE";
    private LocalDate mZeroDate;

    private OnEventFragmentInteractionListener mListener;
    private ViewPager mViewPager;
    private CalendarPagerAdapter mViewPagerAdapter = null;

    public static CalendarFragment newInstance(LocalDate date) {
        Log.d(TAG, "newInstance " + date);
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DATE_SERIALIZABLE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (null != savedInstanceState) {
            mZeroDate = (LocalDate) savedInstanceState.getSerializable(KEY_DATE_SERIALIZABLE);
        } else if (getArguments() != null) {
            mZeroDate = (LocalDate) getArguments().getSerializable(KEY_DATE_SERIALIZABLE);
        }

        Log.d(TAG, "onCreate " + mZeroDate);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState " + mZeroDate);
        outState.putSerializable(KEY_DATE_SERIALIZABLE, mZeroDate);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView " + mZeroDate);

        mViewPager = (ViewPager) inflater.inflate(R.layout.fragment_calendar_viewpager, container, false);
        mViewPager.addOnPageChangeListener(this);
        initViewPagerAdapter(mZeroDate);

        return mViewPager;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_calendar_fragment, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_calendar_today:
                goToDate(LocalDate.now());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEventFragmentInteractionListener) {
            mListener = (OnEventFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mListener.onDateChanged(mViewPagerAdapter.getDateForPosition(position));
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private void initViewPagerAdapter(final LocalDate date) {
        Log.d(TAG, "Setup viewpager content with initial date " + date);
        mViewPagerAdapter = new CalendarPagerAdapter(getChildFragmentManager(), date);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setCurrentItem(mViewPagerAdapter.getMiddlePosition(), false);
    }

    public void goToDate(final LocalDate date) {
        if (mViewPagerAdapter.getDateForPosition(mViewPager.getCurrentItem()).isEqual(date)) {
            Log.d(TAG, "Already showing date " + date);
            return;
        } else if (Math.abs(mViewPager.getCurrentItem() - mViewPagerAdapter.getPositionForDate(date)) <= 30) {
            Log.d(TAG, "Moving to near date " + date);
            mViewPager.setCurrentItem(mViewPagerAdapter.getPositionForDate(date), true);
            return;
        }

        goToDateForced(date);
    }

    public void goToDateForced(final LocalDate date) {
        mZeroDate = date;
        initViewPagerAdapter(date);
    }

    public void handleCreateEditEvent(final int code, final Intent data) {
        if (null == data) {
            return;
        }

        final Event event = data.getParcelableExtra(CreateEditEventActivity.KEY_EVENT_PARCELABLE);
        final LocalDate orgDate = (LocalDate) data.getSerializableExtra(CreateEditEventActivity.KEY_ORG_DATE_SERIALIZABLE);
        final int position = data.getIntExtra(CreateEditEventActivity.KEY_POSITION_INT, -1);

        Log.d(TAG, MessageFormat.format("Code {0} for an event on {1}.", code, orgDate));

        final DayFragment fragment = mViewPagerAdapter.getFragment(mViewPager, event.getDate());

        switch (code) {
            case CreateEditEventActivity.RESULT_INSERTED:
                if (null != fragment) {
                    fragment.addNewEvent(event);
                }
                break;
            case CreateEditEventActivity.RESULT_UPDATED:
                if (event.getDate().isEqual(orgDate)) {
                    if (null != fragment) {
                        fragment.updateAnEventAt(event, position);
                    }
                } else {
                    if (null != fragment) {
                        fragment.addNewEvent(event);
                    }

                    DayFragment fragmentOrg = mViewPagerAdapter.getFragment(mViewPager, orgDate);
                    if (null != fragmentOrg) {
                        fragmentOrg.deleteAnEventAt(event, position);
                    }
                }
                break;
            case CreateEditEventActivity.RESULT_DELETED:
                if (fragment != null && position >= 0) {
                    fragment.deleteAnEventAt(event, position);

                    Snackbar.make(mViewPager, R.string.snack_bar_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.snack_bar_undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (EventHelper.insert(getContext(), event)) {
                                        fragment.addNewEvent(event);
                                    }
                                }
                            }).show();
                }
                break;
            case CreateEditEventActivity.RESULT_CANCELLED:
            case CreateEditEventActivity.RESULT_FAILED:
            case CreateEditEventActivity.RESULT_ERROR:
        }
    }

    public interface OnEventFragmentInteractionListener {
        void onDateChanged(LocalDate newDate);
    }

    private class CalendarPagerAdapter extends FragmentStatePagerAdapter {
        private static final String TAG = "CalendarPagerAdapter";
        private static final int COUNT = 998; // %3 gives 1
        private final LocalDate mZeroDate;

        CalendarPagerAdapter(FragmentManager fm, LocalDate date) {
            super(fm);
            mZeroDate = date;
        }

        @Override
        public Fragment getItem(int position) {
            final LocalDate date = getDateForPosition(position);
            Log.d(TAG, MessageFormat.format("Creating fragment at {0} for {1}.", position, date));

            return DayFragment.newInstance(date);
        }

        @Override
        public int getCount() {
            return COUNT;
        }

        @Nullable
        DayFragment getFragment(@NonNull ViewGroup container, @NonNull LocalDate date) {
            final int position = getPositionForDate(date);

            if (position < 0 || position >= getCount()) {
                return null;
            }

            return (DayFragment) instantiateItem(container, position);
        }

        LocalDate getDateForPosition(int position) {
            return mZeroDate.plusDays(position - getMiddlePosition());
        }

        int getPositionForDate(LocalDate date) {
            final int days = Days.daysBetween(mZeroDate, date).getDays();
            return getMiddlePosition() + days;
        }

        int getMiddlePosition() {
            return COUNT / 2;
        }
    }

}
