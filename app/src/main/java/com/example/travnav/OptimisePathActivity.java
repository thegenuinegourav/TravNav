package com.example.travnav;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travnav.cards.SliderAdapter;
import com.example.travnav.utils.DecodeBitmapTask;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.CardSnapHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.travnav.utils.Constant.DEFAULT_ZOOM;

public class OptimisePathActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int[][] dotCoords = new int[5][2];
    private final int[] pics = {R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5};

    private SliderAdapter sliderAdapter;

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

    private static final String TAG = "OptimisePathActivity";
    private ArrayList<Location> locations;
    private String[] positions, destinations;
    private int size;
    private float[][] graph;
    private int[] optimisePath, vertex;
    private List<Place> placeList;

    private String[] places, descriptions, countries, times;
    private Location[] maps;
    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimise_path);

        init();
        initRecyclerView();
        initMap();
        initCountryText();
        initSwitchers();
    }

    public void init() {
        locations = this.getIntent().getParcelableArrayListExtra("LOCATIONS");
        destinations = this.getIntent().getExtras().getStringArray("DESTINATIONS");
        size = locations.size();
        initPlaceList();
        positions = getpositions();
        Log.d(TAG, "LOCATIONS : " + locations.toString());
        buildGraph();
        float dist = findOptimisedPath(0);
        Log.d(TAG, "Optimised Dist : " + dist + " Path : " + optimisePath.toString());
        setStringArraysWithOptimisedPath();
        sliderAdapter = new SliderAdapter(pics, size, new OnCardClickListener());
    }

    public void initPlaceList() {
        placeList = new ArrayList<>();
        for (int i=0; i<locations.size(); i++) {
            Place place = new Place();
            place.setPlace(destinations[i]);
            place.setLocation(locations.get(i));
            place = initialisePlaceWithAttributes(place,locations.get(i));
            placeList.add(place);
        }
    }

    public Place initialisePlaceWithAttributes(Place place, Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        Geocoder geocoder = new Geocoder(OptimisePathActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String add = obj.getAddressLine(0);
            place.setAddressLine((obj.getAddressLine(0) != null) ? obj.getAddressLine(0) : "Can't Fetch");
            place.setAdminArea((obj.getAdminArea() != null) ? obj.getAdminArea() : "Can't Fetch");
            place.setSubAdminArea((obj.getSubAdminArea() != null) ? obj.getSubAdminArea() : "Can't Fetch");
            place.setLocality((obj.getLocality() != null) ? obj.getLocality() : "Can't Fetch");
            return place;

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return place;
    }

    public void buildGraph() {
        graph = new float[size][size];
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++) {
                float dist = locations.get(i).distanceTo(locations.get(j));
                graph[i][j]=dist;
            }
        }
        Log.d(TAG, "Graph : " + graph.toString());
    }

    float findOptimisedPath(int source)
    {
        int vertexSize = size-1;
        // store all vertex apart from source vertex
        vertex = new int[vertexSize];
        optimisePath = new int[vertexSize];
        for(int i=1;i<size;i++) {
            vertex[i-1]=i;
        }

        // store minimum weight Hamiltonian Cycle.
        float min_path = Float.MAX_VALUE;
        do {

            Log.d(TAG, "Vertex Array : " + vertex.toString());
            // store current Path weight(cost)
            float current_pathweight = 0;

            // compute current path weight
            int k = source;
            for (int i = 0; i < vertexSize; i++) {
                current_pathweight += graph[k][vertex[i]];
                k = vertex[i];
            }

            // update minimum
            if (min_path > current_pathweight) {
                min_path = current_pathweight;
                for (int i=0; i<vertexSize; i++)
                    optimisePath[i] = vertex[i];
            }


        } while (findNextPermutation());

        return min_path;
    }

    public void setStringArraysWithOptimisedPath() {
        places = new String[size];
        descriptions = new String[size];
        countries = new String[size];
        times = new String[size];
        maps = new Location[size];

        initialiseStringArraysWithPlaceAndPos(placeList.get(0), 0);
        for (int i=0;i<optimisePath.length;i++) {
            Place place = placeList.get(optimisePath[i]);
            initialiseStringArraysWithPlaceAndPos(place, i+1);
        }
    }

    public void initialiseStringArraysWithPlaceAndPos(Place place, int pos) {
        places[pos] = place.getPlace();
        descriptions[pos] = place.getAddressLine();
        countries[pos] = place.getAdminArea();
        maps[pos] = place.getLocation();
        if (!place.getSubAdminArea().equals("Can't Fetch")) {
            times[pos] = place.getSubAdminArea() + " , ";
            times[pos] += place.getLocality();
        }else {
            times[pos] = place.getLocality();
        }

    }

    public String[] getpositions() {
        String[] positions = new String[size];
        for (int i=0;i<size;i++) {
            positions[i] = String.valueOf(i+1) + "/" + size;
        }
        return positions;
    }


    //--------------------------------- Find Next Permutation -------------------------------------

    // Function to swap the data
    // present in the left and right indices
    public int[] swap(int data[], int left, int right) {

        // Swap the data
        int temp = data[left];
        data[left] = data[right];
        data[right] = temp;

        // Return the updated array
        return data;
    }

    // Function to reverse the sub-array
    // starting from left to the right
    // both inclusive
    public int[] reverse(int data[], int left, int right) {

        // Reverse the sub-array
        while (left < right) {
            int temp = data[left];
            data[left++] = data[right];
            data[right--] = temp;
        }

        // Return the updated array
        return data;
    }

    // Function to find the next permutation
    // of the given integer array
    public boolean findNextPermutation() {

        // If the given vertexset is empty
        // or contains only one element
        // next_permutation is not possible
        if (vertex.length <= 1)
            return false;

        int last = vertex.length - 2;

        // find the longest non-increasing suffix
        // and find the pivot
        while (last >= 0) {
            if (vertex[last] < vertex[last + 1]) {
                break;
            }
            last--;
        }

        // If there is no increasing pair
        // there is no higher order permutation
        if (last < 0)
            return false;

        int nextGreater = vertex.length - 1;

        // Find the rightmost successor to the pivot
        for (int i = vertex.length - 1; i > last; i--) {
            if (vertex[i] > vertex[last]) {
                nextGreater = i;
                break;
            }
        }

        // Swap the successor and the pivot
        vertex = swap(vertex, nextGreater, last);

        // Reverse the suffix
        vertex = reverse(vertex, last + 1, vertex.length - 1);

        // Return true as the next_permutation is done
        return true;
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.ts_map);
        mapFragment.getMapAsync(OptimisePathActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        moveCamera(placeList.get(0).getLocation(),
                DEFAULT_ZOOM,
                "My Location");
    }

    private void moveCamera(Location location, float zoom, String title){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title);
        mMap.addMarker(options);
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
        temperatureSwitcher.setCurrentText(positions[0]);

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
        descriptionsSwitcher.setCurrentText(descriptions[0]);
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
        temperatureSwitcher.setText(positions[pos % positions.length]);

        placeSwitcher.setInAnimation(OptimisePathActivity.this, animV[0]);
        placeSwitcher.setOutAnimation(OptimisePathActivity.this, animV[1]);
        placeSwitcher.setText(places[pos % places.length]);

        clockSwitcher.setInAnimation(OptimisePathActivity.this, animV[0]);
        clockSwitcher.setOutAnimation(OptimisePathActivity.this, animV[1]);
        clockSwitcher.setText(times[pos % times.length]);

        descriptionsSwitcher.setText(descriptions[pos % size]);

        //showMap(maps[pos % maps.length]);
        moveCamera(maps[pos % size], DEFAULT_ZOOM, places[pos % places.length]);

        ViewCompat.animate(greenDot)
                .translationX(dotCoords[pos % dotCoords.length][0])
                .translationY(dotCoords[pos % dotCoords.length][1])
                .start();

        currentPosition = pos;
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


    public void Navigate(View view) {
        String url = "https://www.google.com/maps/dir/?api=1&origin=" + maps[0].getLatitude() + "," + maps[0].getLongitude();
        url += "&destination=" + maps[size-1].getLatitude() + "," + maps[size-1].getLongitude();
        url += "&waypoints=";
        int i;
        for (i=1;i<size-2;i++) {
            url += maps[i].getLatitude() + "," + maps[i].getLongitude() + "|";
        }
        url += maps[i].getLatitude() + "," + maps[i].getLongitude();
        Uri gmmIntentUri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            try {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                startActivity(unrestrictedIntent);
            } catch (ActivityNotFoundException innerEx) {
                Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
            }
        }
    }


}
