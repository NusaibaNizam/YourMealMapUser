package org.NNS.RestaurantFinder.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.annotation.NonNull;

import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.fragment.app.FragmentTransaction;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DirecctionActivity extends AppCompatActivity implements OnMapReadyCallback{

    boolean isMapCheck=false;
    public static final String BASE_URL = "https://maps.googleapis.com/maps/api/directions/";
    private DirectionService service;
    GoogleMap map;
    GoogleMapOptions options;
    private String origin;
    private String destination=null;
    double lat, lng,deslat=181, deslng;
    LatLng latLng;
    ArrayList<MyItem> clusterItems = new ArrayList<>();
    ClusterManager<MyItem> clusterManager;
    Location lastLocation;
    TextView durTV;
    TextView desTV;
    EditText fromET;
    EditText toET;
    Address desAddress,originAddress;
    boolean mapCheck;
    SupportMapFragment mapFragment;
    Intent intent;
    static final int ORIGIN_REQUEST=2;
    static final int DES_REQUEST=3;
    static final int WAY_REQUEST=4;
    int totalRoutes;
    int route=0;
    String urlString;
    Fragment currentFragment;
    LatLngBounds.Builder builder;
    private FusedLocationProviderClient client;
    Place place;
    BottomNavigationView bottomNavigationView;
    String mode="driving";
    Button nextRoute;
    FragmentTransaction ft;
    FragmentManager fragmentManager;
    ArrayList<Instruction> instructions=new ArrayList<>();
    InstructionFragment instructionFragment;
    boolean start;
    double dLat;
    double dLng;
    private boolean mapCheck1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direcction);
        durTV=findViewById(R.id.DurTV);
        desTV=findViewById(R.id.desTV);
        fromET=findViewById(R.id.formET);
        toET=findViewById(R.id.toET);
        bottomNavigationView=findViewById(R.id.navigation);
        nextRoute=findViewById(R.id.nextRoute);
        nextRoute.setEnabled(false);

        //Create retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(DirectionService.class);


        //Map fragment
        options = new GoogleMapOptions();
        options.zoomControlsEnabled(true).compassEnabled(true).mapType(GoogleMap.MAP_TYPE_TERRAIN);
        mapFragment = SupportMapFragment.newInstance(options);
        fragmentManager=getSupportFragmentManager();
        ft = fragmentManager.beginTransaction().replace(R.id.mapContainer, mapFragment);
        ft.commit();
        mapFragment.getMapAsync(this);

        //get last location on create
        client = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, MapActivity.PERMISSION_REQUEST);
            return;
        }
        client.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    lastLocation = task.getResult();
                    lat=lastLocation.getLatitude();
                    lng=lastLocation.getLongitude();
                }
            }
        });
        //get itent
        intent=getIntent();
        mapCheck=intent.getBooleanExtra("map",false);
        mapCheck1=intent.getBooleanExtra("map1",false);
        start=intent.getBooleanExtra("start",false);
    }

    private void navItemSelected(MenuItem item) {
        switch (item.getItemId()){

            case R.id.navigation_Bus:
            {
                mode="driving";
                if(currentFragment!=mapFragment){

                    ft=fragmentManager.beginTransaction().replace(R.id.mapContainer,mapFragment);
                    ft.commit();
                    mapFragment.getMapAsync(this);
                }

                fromET.setEnabled(true);
                toET.setEnabled(true);
                nextRoute.setEnabled(true);
                nextRoute.setVisibility(View.VISIBLE);
                map.clear();
                route=0;
                getDirections();
            }break;
            case R.id.navigation_walk:
            {
                mode="walking";
                if(currentFragment!=mapFragment){

                    ft=fragmentManager.beginTransaction().replace(R.id.mapContainer,mapFragment);
                    ft.commit();
                    mapFragment.getMapAsync(this);
                }


                fromET.setEnabled(true);
                toET.setEnabled(true);
                nextRoute.setEnabled(true);
                nextRoute.setVisibility(View.VISIBLE);
                map.clear();
                route=0;
                getDirections();
            }break;
            case R.id.navigation_Instruction:
            {
                instructionFragment=InstructionFragment.newInstance(instructions);
                ft=fragmentManager.beginTransaction().replace(R.id.mapContainer,instructionFragment);
                ft.commit();
                fromET.setEnabled(false);
                toET.setEnabled(false);
                nextRoute.setEnabled(false);
                nextRoute.setVisibility(View.INVISIBLE);
            }break;
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        //cluster manager
        clusterManager = new ClusterManager<MyItem>(DirecctionActivity.this, googleMap);
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);
        clusterManager.clearItems();

        //set my location enabled
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, MapActivity.PERMISSION_REQUEST);
            return;
        }
        map.setMyLocationEnabled(true);
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()), 15));
                return false;
            }
        });


        //last location on map ready
        client.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    lastLocation = task.getResult();
                    if(!isMapCheck) {
                        lat = lastLocation.getLatitude();
                        lng = lastLocation.getLongitude();
                        latLng = new LatLng(lat, lng);
                    }
                    //add marker on origin
                    Address address = getAddress(lat, lng);



                }
            }
        });


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                currentFragment = fragmentManager.findFragmentById(R.id.mapContainer);
                navItemSelected(item);
                return true;
            }
        });
        bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                currentFragment = fragmentManager.findFragmentById(R.id.mapContainer);
                navItemSelected(item);
            }
        });

        //value from intent
        deslat=intent.getDoubleExtra("desLat",0);
        deslng=intent.getDoubleExtra("desLng",0);
        if(mapCheck&&!isMapCheck){
            //string origin
            origin="" + lat + "," + lng;
            originAddress=getAddress(lat,lng);
            desAddress=getAddress(deslat,deslng);
            destination="" + deslat + "," + deslng;
                if (originAddress != null && desAddress != null) {
                    fromET.setText(originAddress.getLocality() + ", " + originAddress.getAdminArea());
                    toET.setText(intent.getStringExtra("name") + ", " + desAddress.getAdminArea());
                } else {
                    fromET.setText("Your Location");
                    toET.setText(intent.getStringExtra("name"));
                }

        }
        if(mapCheck){

            map.clear();

            //call getDirection method
            route=0;
            getDirections();
        }

        //Disable Map Toolbar:
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.map = googleMap;


    }

    //get Address
    public Address getAddress(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            return addresses.get(0);
        } catch (Exception e){}
        return null;
    }

    //get Direction
    private void getDirections() {

        String key= getString(R.string.google_direction_api);
        urlString= String.format("json?origin=%s&destination=%s&mode=%s&alternatives=true&key=%s",
                origin,destination,mode,key);
        dLat=lat;
        dLng=lng;
        if(start){
            dLat=intent.getDoubleExtra("startLat",lat);
            dLng=intent.getDoubleExtra("startLng",lng);
            origin="" + dLat + "," + dLng;
            fromET.setText(intent.getStringExtra("startName"));
            urlString= String.format("json?origin=%s&destination=%s&mode=%s&alternatives=true&key=%s",
                    origin,destination,mode,key);
            Log.e("derUrl",BASE_URL+urlString);
        }
        //manage Markers
        if(instructions!=null){
            instructions.clear();
        }
        map.clear();
        clusterItems.clear();
        clusterManager.clearItems();
        clusterItems.add(new MyItem(dLat,dLng));
        clusterItems.add(new MyItem(deslat,deslng));
        clusterManager.addItems(clusterItems);
        clusterManager.cluster();
        builder = new LatLngBounds.Builder();
        //get Response
        Call<DirectionResponse> directionResponseCall = service.getDirections(urlString);
        directionResponseCall.enqueue(new Callback<DirectionResponse>() {
            @Override
            public void onResponse(Call<DirectionResponse> call, Response<DirectionResponse> response) {
                if(response.isSuccessful()){
                    DirectionResponse directionResponse = response.body();
                    totalRoutes = directionResponse.getRoutes().size();

                    if(directionResponse.getRoutes().size()!=0) {
                        List<DirectionResponse.Step> steps = directionResponse.getRoutes().get(route).getLegs().get(0).getSteps();
                        desTV.setText(directionResponse.getRoutes().get(route).getLegs().get(0).getDistance().getText());
                        durTV.setText(directionResponse.getRoutes().get(route).getLegs().get(0).getDuration().getText());
                        if(start){
                            instructions.add(new Instruction(intent.getStringExtra("startName"),dLat,dLng));
                        }else instructions.add(new Instruction(fromET.getText().toString(),dLat,dLng));

                        //manage Origin
                        LatLng start1 = new LatLng(dLat, dLng);
                        double endLat1 = steps.get(0).getStartLocation().getLat();
                        double endLng1 = steps.get(0).getStartLocation().getLng();
                        LatLng end1 = new LatLng(endLat1, endLng1);


                        //add polyline from origin

                        Polyline polyline1 = map.addPolyline(new PolylineOptions().add(start1).add(end1));
                        polyline1.setColor(R.color.skyBlue);
                        for (DirectionResponse.Step step : steps) {
                            //get steps start point
                            double startLat = step.getStartLocation().getLat();
                            double startLng = step.getStartLocation().getLng();
                            LatLng start = new LatLng(startLat, startLng);

                            //add points in builder
                            builder.include(start);

                            //get end points
                            double endLat = step.getEndLocation().getLat();
                            double endLng = step.getEndLocation().getLng();
                            LatLng end = new LatLng(endLat, endLng);

                            //add points in builder
                            builder.include(end);


                            //add polylines
                            Polyline polyline = map.addPolyline(new PolylineOptions().add(start).add(end));
                            polyline.setColor(R.color.skyBlue);

                            instructions.add(new Instruction(step.getHtmlInstructions(),startLat,startLng));
                        }

                        //manage Des
                        LatLng start2 = new LatLng(deslat, deslng);
                        double endLat2 = steps.get(steps.size() - 1).getEndLocation().getLat();
                        double endLng2 = steps.get(steps.size() - 1).getEndLocation().getLng();
                        LatLng end2 = new LatLng(endLat2, endLng2);
                        //add des polyline
                        Polyline polyline2 = map.addPolyline(new PolylineOptions().add(end2).add(start2));
                        polyline2.setColor(R.color.skyBlue);

                        instructions.add(new Instruction(toET.getText().toString(),deslat,deslng));

                        //include start and end point in builder

                        builder.include(new LatLng(directionResponse.getRoutes().get(route).getBounds().getNortheast().getLat() + .1
                                , directionResponse.getRoutes().get(route).getBounds().getNortheast().getLng() + .1));

                        builder.include(new LatLng(directionResponse.getRoutes().get(route).getBounds().getSouthwest().getLat() - .1
                                , directionResponse.getRoutes().get(route).getBounds().getSouthwest().getLng() - .1));




                    }

                    else {
                        Toast.makeText(DirecctionActivity.this,"No routes",Toast.LENGTH_SHORT).show();
                        desTV.setText("-");
                        durTV.setText("-");
                    }
                    builder.include(new LatLng(dLat-.1, dLng-.1));
                    builder.include(new LatLng(deslat+.1, dLng+.1));
                    builder.include(new LatLng(deslat-.1, deslng-.1));
                    builder.include(new LatLng(deslat+.1, deslng+.1));

                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),30));
                    try {
                        if (directionResponse.getRoutes().get(route).getLegs().get(0).getDistance().getValue() <= 2500) {
                            map.setMinZoomPreference(15);
                        } else map.setMinZoomPreference(0);
                    }catch (Exception e){}
                    Button waypoint=findViewById(R.id.wayPoints);
                    if(totalRoutes<=1){
                        nextRoute.setEnabled(false);
                        nextRoute.setVisibility(View.INVISIBLE);
                        waypoint.setEnabled(false);
                        waypoint.setVisibility(View.INVISIBLE);
                    }
                    else{
                        nextRoute.setEnabled(true);
                        nextRoute.setVisibility(View.VISIBLE);
                        waypoint.setEnabled(true);
                        waypoint.setVisibility(View.VISIBLE);
                    }
                    start=false;
                }
                else {

                    builder.include(new LatLng(lat-.1, lng-.1));
                    builder.include(new LatLng(lat+.1, lng+.1));
                    builder.include(new LatLng(deslat-.1, deslng-.1));
                    builder.include(new LatLng(deslat+.1, deslng+.1));
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 30));

                    Toast.makeText(DirecctionActivity.this,response.message(),Toast.LENGTH_SHORT).show();
                    desTV.setText("-");
                    durTV.setText("-");
                    start=false;
                }
            }

            @Override
            public void onFailure(Call<DirectionResponse> call, Throwable t) {
                Toast.makeText(DirecctionActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();
                builder.include(new LatLng(lat-.1, lng-.1));
                builder.include(new LatLng(lat+.1, lng+.1));
                builder.include(new LatLng(deslat-.1, deslng-.1));
                builder.include(new LatLng(deslat+.1, deslng+.1));
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 30));
                desTV.setText("-");
                durTV.setText("-");
                start=false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(mapCheck&&mapCheck1){
            Intent intent1=new Intent(DirecctionActivity.this,MapActivity.class);
            startActivity(intent1);
        }
        super.onBackPressed();
    }

    //auto complete search
    public void setDes(View view) {
        isMapCheck=true;
        int request=0;

        //check editText id
        switch (view.getId()){
            case R.id.toET:
                request=DES_REQUEST;
                break;
            case R.id.formET:
                request=ORIGIN_REQUEST;
                break;
        }

        try {
            //auto complete search
            Address address = getAddress(lat, lng);
            List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,com.google.android.libraries.places.api.model.Place.Field.LAT_LNG,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields)
                    .build(DirecctionActivity.this);
            startActivityForResult(intent,request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //get auto complete search result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){

            //change origin
            case ORIGIN_REQUEST:
                if (resultCode == RESULT_OK) {
                    place = Autocomplete.getPlaceFromIntent(data);
                    double desLat = place.getLatLng().latitude;
                    double desLng = place.getLatLng().longitude;
                    lat=desLat;
                    lng=desLng;
                    fromET.setText(place.getAddress());
                    originAddress=getAddress(desLat,desLng);
                    origin = "" + desLat + "," + desLng;
                    map.clear();
                    route=0;
                    getDirections();

                }else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(data);
                    // TODO: Handle the error.
                    Log.i("",status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
                break;

            //change des
            case DES_REQUEST:
                if (resultCode == RESULT_OK) {
                    place = Autocomplete.getPlaceFromIntent(data);
                    double desLat = place.getLatLng().latitude;
                    double desLng = place.getLatLng().longitude;
                    deslat=desLat;
                    deslng=desLng;
                    desAddress=getAddress(desLat,desLng);
                    toET.setText(place.getAddress());
                    destination = "" + desLat + "," + desLng;
                    map.clear();
                    route=0;
                    getDirections();

                }else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(data);
                    // TODO: Handle the error.
                    Log.i("",status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
                break;
            case WAY_REQUEST:
                if (resultCode == RESULT_OK) {
                    place = Autocomplete.getPlaceFromIntent(data);

                    if(mode.contains("&waypoints=")){
                        mode=mode+"|"+place.getAddress();
                    }else mode=mode+"&waypoints="+place.getAddress();
                    map.clear();
                    route=0;
                    getDirections();
                }else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(data);
                    // TODO: Handle the error.
                    Log.i("",status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
                break;


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //change route
    public void nextRoute(View view) {
        if(route<totalRoutes-1){
            route++;
        }
        else {
            route=0;
        }
        map.clear();
        getDirections();
    }

    public void addWaypoints(View view) {
        int request=WAY_REQUEST;
        try {
            //auto complete search
            Address address = getAddress(lat, lng);
            List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,com.google.android.libraries.places.api.model.Place.Field.LAT_LNG,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS);

            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields)
                    .build(DirecctionActivity.this);
            startActivityForResult(intent,request);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
