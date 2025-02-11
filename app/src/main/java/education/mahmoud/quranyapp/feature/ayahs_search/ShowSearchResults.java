package education.mahmoud.quranyapp.feature.ayahs_search;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.MessageFormat;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import education.mahmoud.quranyapp.R;
import education.mahmoud.quranyapp.Util.Constants;
import education.mahmoud.quranyapp.Util.Data;
import education.mahmoud.quranyapp.Util.Util;
import education.mahmoud.quranyapp.data_layer.Repository;
import education.mahmoud.quranyapp.data_layer.local.room.AyahItem;
import education.mahmoud.quranyapp.feature.listening_activity.ListenServie;
import education.mahmoud.quranyapp.feature.show_sura_ayas.ShowAyahsActivity;

public class ShowSearchResults extends AppCompatActivity {

    private static final String TAG = "ShowSearchResults";
    private static final int RC_STORAGE = 10002;
    @BindView(R.id.edSearch)
    TextInputEditText edSearch;
    @BindView(R.id.rvSearch)
    RecyclerView rvSearch;
    @BindView(R.id.tvNotFound)
    TextView tvNotFound;
    @BindView(R.id.tvSearchCount)
    TextView tvSearchCount;

    SearchResultsAdapter adapter;
    @BindView(R.id.bottomSearch)
    BottomSheetLayout bottomSearch;
    private Repository repository;
    private List<AyahItem> ayahItems;
    private String ayah;

    private MediaPlayer mediaPlayer;

    boolean isRunning = false;

    private void editWatcher() {
        edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                ayah = editable.toString();
                ayah = ayah.replaceAll(" +", " ");
                if (!TextUtils.isEmpty(ayah) && ayah.length() > 0) {
                    ayahItems = repository.getAyahByAyahText(ayah);
                    Log.d(TAG, "afterTextChanged: " + ayahItems.size());

                    int count = ayahItems.size(); // n of results
                    tvSearchCount.setText(getString(R.string.times, count));
                    if (count > 0) {
                        adapter.setAyahItemList(ayahItems, ayah);
                        foundState();
                    } else {
                        notFoundState();
                    }
                } else {
                    defaultState();
                }

            }
        });

    }

    private void setUpBottomSheet(AyahItem ayahItem) {
        // bottom sheet
        MenuSheetView menuSheetView =
                new MenuSheetView(ShowSearchResults.this, MenuSheetView.MenuType.LIST, "Options", new MenuSheetView.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (bottomSearch.isSheetShowing()) {
                            bottomSearch.dismissSheet();
                        }
                        switch (item.getItemId()) {
                            case R.id.menuOpen:
                                openPage(ayahItem.getPageNum());
                                break;
                            case R.id.menuPlaySound:
                                playAudio(ayahItem);
                                break;
                            case R.id.menuTafser:
                                showTafseer(ayahItem);
                                break;
                        }
                        return true;
                    }
                });
        menuSheetView.inflateMenu(R.menu.menu_sheet_search);
        bottomSearch.showWithSheetView(menuSheetView);
    }

    private void showTafseer(AyahItem ayahItem) {
        if (ayahItem.getTafseer() != null) {
            String title = this.
                    getString(R.string.tafserr_info, ayahItem.getAyahInSurahIndex(), ayahItem.getPageNum(), ayahItem.getJuz());
            Util.getDialog(this, ayahItem.getTafseer(), title).show();
        } else {
            Toast.makeText(this, this.getText(R.string.tafseer_not_down), Toast.LENGTH_SHORT).show();
        }
    }

    private void adapterListeners() {
        adapter.setiSearchItemClick(new SearchResultsAdapter.ISearchItemClick() {
            @Override
            public void onSearchItemClick(AyahItem item) {
                setUpBottomSheet(item);
                Log.d(TAG, "onSearchItemClick: ");
            }
        });
    }

    private void openPage(int pageNum) {
        Intent openAcivity = new Intent(ShowSearchResults.this, ShowAyahsActivity.class);
        openAcivity.putExtra(Constants.PAGE_INDEX, pageNum);
        startActivity(openAcivity);
    }

    /**
     * state with no search performed
     */
    private void defaultState() {
        tvNotFound.setVisibility(View.GONE);
    }

    private void initRv() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        rvSearch.setLayoutManager(manager);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "me_quran.ttf");
        adapter = new SearchResultsAdapter(typeface);
        rvSearch.setAdapter(adapter);
        rvSearch.setHasFixedSize(true);

    }

    private void foundState() {
        rvSearch.setVisibility(View.VISIBLE);
        tvNotFound.setVisibility(View.GONE);
    }

    private void notFoundState() {
        rvSearch.setVisibility(View.GONE);
        tvNotFound.setVisibility(View.VISIBLE);
    }

    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_search_results);
        ButterKnife.bind(this);
        repository = Repository.getInstance(getApplication());
        initRv();
        adapterListeners();
        editWatcher();

    }

    private void playAudio(AyahItem item) {
        if (item.getAudioPath() != null) {
            Log.d(TAG, "playAudio: isRunning  " + isRunning);
            if (serviceIntent != null) {
                stopService(serviceIntent);
            }
            serviceIntent = ListenServie.createService(getApplicationContext(),
                    item.getAudioPath(), getName(item));

        } else {
            showMessage(getString(R.string.not_downlod_audio));
        }

    }

    private String getName(AyahItem item) {
        String name = Data.SURA_NAMES[item.getSurahIndex() - 1]; // surah index start from 1 but arr from 0
        return MessageFormat.format("{0},{1}",
                name, item.getAyahInSurahIndex());
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }



}
