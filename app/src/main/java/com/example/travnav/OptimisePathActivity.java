package com.example.travnav;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.example.travnav.cards.SliderAdapter;
import com.example.travnav.utils.DecodeBitmapTask;
import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.CardSnapHelper;

public class OptimisePathActivity extends AppCompatActivity {

    private final int[][] dotCoords = new int[5][2];
    private final int[] pics = {R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5};
    private final int[] maps = {R.drawable.map_paris, R.drawable.map_seoul, R.drawable.map_london, R.drawable.map_beijing, R.drawable.map_greece};
    private final int[] descriptions = {R.string.text1, R.string.text2, R.string.text3, R.string.text4, R.string.text5};
    private final String[] countries = {"PARIS", "SEOUL", "LONDON", "BEIJING", "THIRA"};
    private final String[] places = {"The Louvre", "Gwanghwamun", "Tower Bridge", "Temple of Heaven", "Aegeana Sea"};
    private final String[] temperatures = {"21°C", "19°C", "17°C", "23°C", "20°C"};
    private final String[] times = {"Aug 1 - Dec 15    7:00-18:00", "Sep 5 - Nov 10    8:00-16:00", "Mar 8 - May 21    7:00-18:00"};

    private final SliderAdapter sliderAdapter = new SliderAdapter(pics, 20, new OnCardClickListener());

    private CardSliderLayoutManager layoutManger;
    private RecyclerView recyclerView;
    private ImageSwitcher mapSwitcher;
    private TextSwitcher temperatureSwitcher;
    private TextSwitcher placeSwitcher;
    private TextSwitcher clockSwitcher;
    private TextSwitcher descriptionsSwitcher;
    private View greenDot;

    private TextView country1TextView;
    private TextView country2TextView;
    private int countryOffset1;
    private int countryOffset2;
    private long countryAnimDuration;
    private int currentPosition;

    private DecodeBitmapTask decodeMapBitmapTask;
    private DecodeBitmapTask.Listener mapLoadListener;

//    private static final String TAG = "OptimisePathActivity";
//    private ArrayList<Location> locations;
//    private int size;
//    private float[][] graph;
//    private int[] optimisePath, vertex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimise_path);

        initRecyclerView();
        initCountryText();
        initSwitchers();
        initGreenDot();

        //init();
    }

    private void initRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setAdapter(sliderAdapter);
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onActiveCardChange();
                }
            }
        });

        layoutManger = (CardSliderLayoutManager) recyclerView.getLayoutManager();

        new CardSnapHelper().attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && decodeMapBitmapTask != null) {
            decodeMapBitmapTask.cancel(true);
        }
    }

    private void initSwitchers() {
        temperatureSwitcher = (TextSwitcher) findViewById(R.id.ts_temperature);
        temperatureSwitcher.setFactory(new TextViewFactory(R.style.TemperatureTextView, true));
        temperatureSwitcher.setCurrentText(temperatures[0]);

        placeSwitcher = (TextSwitcher) findViewById(R.id.ts_place);
        placeSwitcher.setFactory(new TextViewFactory(R.style.PlaceTextView, false));
        placeSwitcher.setCurrentText(places[0]);

        clockSwitcher = (TextSwitcher) findViewById(R.id.ts_clock);
        clockSwitcher.setFactory(new TextViewFactory(R.style.ClockTextView, false));
        clockSwitcher.setCurrentText(times[0]);

        descriptionsSwitcher = (TextSwitcher) findViewById(R.id.ts_description);
        descriptionsSwitcher.setInAnimation(this, android.R.anim.fade_in);
        descriptionsSwitcher.setOutAnimation(this, android.R.anim.fade_out);
        descriptionsSwitcher.setFactory(new TextViewFactory(R.style.DescriptionTextView, false));
        descriptionsSwitcher.setCurrentText(getString(descriptions[0]));

        mapSwitcher = (ImageSwitcher) findViewById(R.id.ts_map);
        mapSwitcher.setInAnimation(this, R.anim.fade_in);
        mapSwitcher.setOutAnimation(this, R.anim.fade_out);
        mapSwitcher.setFactory(new ImageViewFactory());
        mapSwitcher.setImageResource(maps[0]);

        mapLoadListener = new DecodeBitmapTask.Listener() {
            @Override
            public void onPostExecuted(Bitmap bitmap) {
                ((ImageView)mapSwitcher.getNextView()).setImageBitmap(bitmap);
                mapSwitcher.showNext();
            }
        };
    }

    private void initCountryText() {
        countryAnimDuration = getResources().getInteger(R.integer.labels_animation_duration);
        countryOffset1 = getResources().getDimensionPixelSize(R.dimen.left_offset);
        countryOffset2 = getResources().getDimensionPixelSize(R.dimen.card_width);
        country1TextView = (TextView) findViewById(R.id.tv_country_1);
        country2TextView = (TextView) findViewById(R.id.tv_country_2);

        country1TextView.setX(countryOffset1);
        country2TextView.setX(countryOffset2);
        country1TextView.setText(countries[0]);
        country2TextView.setAlpha(0f);

        country1TextView.setTypeface(Typeface.createFromAsset(getAssets(), "open-sans-extrabold.ttf"));
        country2TextView.setTypeface(Typeface.createFromAsset(getAssets(), "open-sans-extrabold.ttf"));
    }

    private void initGreenDot() {
        mapSwitcher.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mapSwitcher.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                final int viewLeft = mapSwitcher.getLeft();
                final int viewTop = mapSwitcher.getTop() + mapSwitcher.getHeight() / 3;

                final int border = 100;
                final int xRange = Math.max(1, mapSwitcher.getWidth() - border * 2);
                final int yRange = Math.max(1, (mapSwitcher.getHeight() / 3) * 2 - border * 2);

                final Random rnd = new Random();

                for (int i = 0, cnt = dotCoords.length; i < cnt; i++) {
                    dotCoords[i][0] = viewLeft + border + rnd.nextInt(xRange);
                    dotCoords[i][1] = viewTop + border + rnd.nextInt(yRange);
                }

                greenDot = findViewById(R.id.green_dot);
                greenDot.setX(dotCoords[0][0]);
                greenDot.setY(dotCoords[0][1]);
            }
        });
    }

    private void setCountryText(String text, boolean left2right) {
        final TextView invisibleText;
        final TextView visibleText;
        if (country1TextView.getAlpha() > country2TextView.getAlpha()) {
            visibleText = country1TextView;
            invisibleText = country2TextView;
        } else {
            visibleText = country2TextView;
            invisibleText = country1TextView;
        }

        final int vOffset;
        if (left2right) {
            invisibleText.setX(0);
            vOffset = countryOffset2;
        } else {
            invisibleText.setX(countryOffset2);
            vOffset = 0;
        }

        invisibleText.setText(text);

        final ObjectAnimator iAlpha = ObjectAnimator.ofFloat(invisibleText, "alpha", 1f);
        final ObjectAnimator vAlpha = ObjectAnimator.ofFloat(visibleText, "alpha", 0f);
        final ObjectAnimator iX = ObjectAnimator.ofFloat(invisibleText, "x", countryOffset1);
        final ObjectAnimator vX = ObjectAnimator.ofFloat(visibleText, "x", vOffset);

        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(iAlpha, vAlpha, iX, vX);
        animSet.setDuration(countryAnimDuration);
        animSet.start();
    }

    private void onActiveCardChange() {
        final int pos = layoutManger.getActiveCardPosition();
        if (pos == RecyclerView.NO_POSITION || pos == currentPosition) {
            return;
        }

        onActiveCardChange(pos);
    }

    private void onActiveCardChange(int pos) {
        int animH[] = new int[] {R.anim.slide_in_right, R.anim.slide_out_left};
        int animV[] = new int[] {R.anim.slide_in_top, R.anim.slide_out_bottom};

        final boolean left2right = pos < currentPosition;
        if (left2right) {
            animH[0] = R.anim.slide_in_left;
            animH[1] = R.anim.slide_out_right;

            animV[0] = R.anim.slide_in_bottom;
            animV[1] = R.anim.slide_out_top;
        }

        setCountryText(countries[pos % countries.length], left2right);

        temperatureSwitcher.setInAnimation(OptimisePathActivity.this, animH[0]);
        temperatureSwitcher.setOutAnimation(OptimisePathActivity.this, animH[1]);
        temperatureSwitcher.setText(temperatures[pos % temperatures.length]);

        placeSwitcher.setInAnimation(OptimisePathActivity.this, animV[0]);
        placeSwitcher.setOutAnimation(OptimisePathActivity.this, animV[1]);
        placeSwitcher.setText(places[pos % places.length]);

        clockSwitcher.setInAnimation(OptimisePathActivity.this, animV[0]);
        clockSwitcher.setOutAnimation(OptimisePathActivity.this, animV[1]);
        clockSwitcher.setText(times[pos % times.length]);

        descriptionsSwitcher.setText(getString(descriptions[pos % descriptions.length]));

        showMap(maps[pos % maps.length]);

        ViewCompat.animate(greenDot)
                .translationX(dotCoords[pos % dotCoords.length][0])
                .translationY(dotCoords[pos % dotCoords.length][1])
                .start();

        currentPosition = pos;
    }

    private void showMap(@DrawableRes int resId) {
        if (decodeMapBitmapTask != null) {
            decodeMapBitmapTask.cancel(true);
        }

        final int w = mapSwitcher.getWidth();
        final int h = mapSwitcher.getHeight();

        decodeMapBitmapTask = new DecodeBitmapTask(getResources(), resId, w, h, mapLoadListener);
        decodeMapBitmapTask.execute();
    }

    private class TextViewFactory implements  ViewSwitcher.ViewFactory {

        @StyleRes
        final int styleId;
        final boolean center;

        TextViewFactory(@StyleRes int styleId, boolean center) {
            this.styleId = styleId;
            this.center = center;
        }

        @SuppressWarnings("deprecation")
        @Override
        public View makeView() {
            final TextView textView = new TextView(OptimisePathActivity.this);

            if (center) {
                textView.setGravity(Gravity.CENTER);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                textView.setTextAppearance(OptimisePathActivity.this, styleId);
            } else {
                textView.setTextAppearance(styleId);
            }

            return textView;
        }

    }

    private class ImageViewFactory implements ViewSwitcher.ViewFactory {
        @Override
        public View makeView() {
            final ImageView imageView = new ImageView(OptimisePathActivity.this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            final LayoutParams lp = new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(lp);

            return imageView;
        }
    }

    private class OnCardClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            final CardSliderLayoutManager lm =  (CardSliderLayoutManager) recyclerView.getLayoutManager();

            if (lm.isSmoothScrolling()) {
                return;
            }

            final int activeCardPosition = lm.getActiveCardPosition();
            if (activeCardPosition == RecyclerView.NO_POSITION) {
                return;
            }

            final int clickedPosition = recyclerView.getChildAdapterPosition(view);
            if (clickedPosition > activeCardPosition) {
                recyclerView.smoothScrollToPosition(clickedPosition);
                onActiveCardChange(clickedPosition);
            }
        }
    }

//    public void init() {
//        locations = this.getIntent().getParcelableArrayListExtra("LOCATIONS");
//        Log.d(TAG, "LOCATIONS : " + locations.toString());
//        size = locations.size();
//        buildGraph();
//        float dist = findOptimisedPath(0);
//        Log.d(TAG, "Optimised Dist : " + dist + " Path : " + optimisePath.toString());
//
//        List<String> optimiseLocationsNames = getLocationNamesFromOptimisePath();
//        TextView textView = (TextView) findViewById(R.id.textVw);
//        String tv = "";
//        for(int i=0;i<optimisePath.length;i++) {
//            tv = tv +  optimisePath[i] + " ";
//        }
//        tv += "\n";
//        tv = tv + optimiseLocationsNames.toString();
//        textView.setText(tv);
//    }
//
//    public List<String> getLocationNamesFromOptimisePath() {
//        List<String> optimiseLocationsNames = new ArrayList<>();
//        for (int i=0;i<optimisePath.length;i++) {
//            Location location = locations.get(optimisePath[i]);
//            optimiseLocationsNames.add(getAddress(location.getLatitude(), location.getLongitude()));
//        }
//        return optimiseLocationsNames;
//    }
//
//    public String getAddress(double lat, double lng) {
//        Geocoder geocoder = new Geocoder(OptimisePathActivity.this, Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
//            Address obj = addresses.get(0);
//            String add = obj.getAddressLine(0);
////            add = add + "\n" + obj.getCountryName();
////            add = add + "\n" + obj.getCountryCode();
////            add = add + "\n" + obj.getAdminArea();
////            add = add + "\n" + obj.getPostalCode();
////            add = add + "\n" + obj.getSubAdminArea();
////            add = add + "\n" + obj.getLocality();
////            add = add + "\n" + obj.getSubThoroughfare();
//            add = add + "\n\n\n";
//            Log.v("IGA", "Address" + add);
//            return add;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//        return "Can't find address ";
//    }
//
//    public void buildGraph() {
//        graph = new float[size][size];
//        for (int i=0; i<size; i++) {
//            for (int j=0; j<size; j++) {
//                float dist = locations.get(i).distanceTo(locations.get(j));
//                graph[i][j]=dist;
//            }
//        }
//        Log.d(TAG, "Graph : " + graph.toString());
//    }
//
//    float findOptimisedPath(int source)
//    {
//        int vertexSize = size-1;
//        // store all vertex apart from source vertex
//        vertex = new int[vertexSize];
//        optimisePath = new int[vertexSize];
//        for(int i=1;i<size;i++) {
//            vertex[i-1]=i;
//        }
//
//        // store minimum weight Hamiltonian Cycle.
//        float min_path = Float.MAX_VALUE;
//        do {
//
//            Log.d(TAG, "Vertex Array : " + vertex.toString());
//            // store current Path weight(cost)
//            float current_pathweight = 0;
//
//            // compute current path weight
//            int k = source;
//            for (int i = 0; i < vertexSize; i++) {
//                current_pathweight += graph[k][vertex[i]];
//                k = vertex[i];
//            }
//
//            // update minimum
//            if (min_path > current_pathweight) {
//                min_path = current_pathweight;
//                for (int i=0; i<vertexSize; i++)
//                    optimisePath[i] = vertex[i];
//            }
//
//
//        } while (findNextPermutation());
//
//        return min_path;
//    }
//
//
//    //--------------------------------- Find Next Permutation -------------------------------------
//
//    // Function to swap the data
//    // present in the left and right indices
//    public int[] swap(int data[], int left, int right) {
//
//        // Swap the data
//        int temp = data[left];
//        data[left] = data[right];
//        data[right] = temp;
//
//        // Return the updated array
//        return data;
//    }
//
//    // Function to reverse the sub-array
//    // starting from left to the right
//    // both inclusive
//    public int[] reverse(int data[], int left, int right) {
//
//        // Reverse the sub-array
//        while (left < right) {
//            int temp = data[left];
//            data[left++] = data[right];
//            data[right--] = temp;
//        }
//
//        // Return the updated array
//        return data;
//    }
//
//    // Function to find the next permutation
//    // of the given integer array
//    public boolean findNextPermutation() {
//
//        // If the given vertexset is empty
//        // or contains only one element
//        // next_permutation is not possible
//        if (vertex.length <= 1)
//            return false;
//
//        int last = vertex.length - 2;
//
//        // find the longest non-increasing suffix
//        // and find the pivot
//        while (last >= 0) {
//            if (vertex[last] < vertex[last + 1]) {
//                break;
//            }
//            last--;
//        }
//
//        // If there is no increasing pair
//        // there is no higher order permutation
//        if (last < 0)
//            return false;
//
//        int nextGreater = vertex.length - 1;
//
//        // Find the rightmost successor to the pivot
//        for (int i = vertex.length - 1; i > last; i--) {
//            if (vertex[i] > vertex[last]) {
//                nextGreater = i;
//                break;
//            }
//        }
//
//        // Swap the successor and the pivot
//        vertex = swap(vertex, nextGreater, last);
//
//        // Reverse the suffix
//        vertex = reverse(vertex, last + 1, vertex.length - 1);
//
//        // Return true as the next_permutation is done
//        return true;
//    }
}
