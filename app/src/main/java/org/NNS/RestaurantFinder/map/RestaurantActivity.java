package org.NNS.RestaurantFinder.map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;

public class RestaurantActivity extends AppCompatActivity {

    private static final int CALL_REQUEST_CODE = 676;
    private Restaurant restaurant;
    private boolean map;
    private String name;
    private double desLng;
    private double desLat;
    private boolean start;
    private String startName;
    private double startLng;
    private double startLat;
    RatingBar ratingBar;
    ImageView imageView;
    TextView nameTV;
    TextView phoneTV;
    LinearLayout offerLL;
    RecyclerView offerRV;
    RecyclerView menuRV;
    Double rating;
    private FirebaseDatabase database;
    private DatabaseReference offerDatabase;
    private DatabaseReference menuDatabase;
    ArrayList<Offer> offers;
    ArrayList<Menu> menus;
    FirebaseRecyclerOptions<Offer> offerFirebaseRecyclerOptions;
    FirebaseRecyclerOptions<Menu> menuFirebaseRecyclerOptions;
    FirebaseRecyclerAdapter<Offer,OfferViewHolder> offerViewHolderFirebaseRecyclerAdapter;
    FirebaseRecyclerAdapter<Menu,MenuViewHolder> menuViewHolderFirebaseRecyclerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);
        Intent intent=getIntent();
        rating=intent.getDoubleExtra("rate",0);
        startLat=intent.getDoubleExtra("startLat",0);
        startLng=intent.getDoubleExtra("startLng", 0);
        startName=intent.getStringExtra("startName");
        start=intent.getBooleanExtra("start", true);
        desLat=intent.getDoubleExtra("desLat", 0);
        desLng=intent.getDoubleExtra("desLng", 0);
        name=intent.getStringExtra("name");
        map=intent.getBooleanExtra("map", true);
        restaurant= (Restaurant) intent.getSerializableExtra("res");
        imageView=findViewById(R.id.resImageIV);
        nameTV=findViewById(R.id.resNameTV);
        phoneTV=findViewById(R.id.resPhoneTV);
        offerLL=findViewById(R.id.orderLayout);
        offerRV=findViewById(R.id.offersIV);
        menuRV=findViewById(R.id.menuIV);
        ratingBar=findViewById(R.id.rating);
        if(rating!=0)
            ratingBar.setRating((float) (rating*1.000));
        else ratingBar.setVisibility(View.GONE);
        menus=new ArrayList<>();
        offers=new ArrayList<>();
        Glide.with(this /* context */)
                .load(restaurant.getPhoto())
                .optionalCenterCrop()
                .placeholder(R.drawable.cover)
                .into(imageView);
        nameTV.setText(restaurant.getRestaurantName());
        phoneTV.setText(restaurant.getPhoneNumber());
        database=FirebaseDatabase.getInstance();
        offerDatabase=database.getReference().child("offers");
        menuDatabase=database.getReference().child("menu").child(restaurant.getPlaceID());



        offerDatabase.child(restaurant.getPlaceID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null){
                    offerLL.setVisibility(View.VISIBLE);
                } else {
                    offerLL.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        offerRV.setHasFixedSize(true);
        offerRV.setLayoutManager(new LinearLayoutManager(RestaurantActivity.this));
        offerFirebaseRecyclerOptions=new FirebaseRecyclerOptions.Builder<Offer>().
                setQuery(offerDatabase.child(restaurant.getPlaceID()),Offer.class).build();
        offerViewHolderFirebaseRecyclerAdapter =new FirebaseRecyclerAdapter<Offer,
                OfferViewHolder>(offerFirebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull OfferViewHolder offerViewHolder, int i, @NonNull Offer offer) {
                offerViewHolder.offerTV.setText(offer.getOfferText());
            }

            @NonNull
            @Override
            public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new OfferViewHolder(LayoutInflater.from(RestaurantActivity.this)
                        .inflate(R.layout.offer_row,parent,false));
            }
        };
        offerRV.setAdapter(offerViewHolderFirebaseRecyclerAdapter);

        menuRV.setHasFixedSize(true);
        menuRV.setLayoutManager(new LinearLayoutManager(this));
        menuFirebaseRecyclerOptions=new FirebaseRecyclerOptions.Builder<Menu>().
                setQuery(menuDatabase,Menu.class).build();
        menuViewHolderFirebaseRecyclerAdapter =new FirebaseRecyclerAdapter<Menu, MenuViewHolder>(menuFirebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder menuViewHolder, int i, @NonNull Menu menu) {
                Glide.with(RestaurantActivity.this /* context */)
                        .load(menu.getPhoto())
                        .optionalCenterCrop()
                        .placeholder(R.drawable.cover)
                        .into(menuViewHolder.menuIV);
                menuViewHolder.menuNameTV.setText(menu.getName());
                menuViewHolder.portionTV.setText(menu.getPortion());
                menuViewHolder.priceTV.setText(menu.getPrice());
            }

            @NonNull
            @Override
            public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new MenuViewHolder(LayoutInflater.from(RestaurantActivity.this)
                        .inflate(R.layout.menu_row,parent,false));
            }
        };

        menuRV.setAdapter(menuViewHolderFirebaseRecyclerAdapter);

    }

    public void call(View view) {
        if(ContextCompat.checkSelfPermission(RestaurantActivity.this,
                Manifest.permission.CALL_PHONE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(RestaurantActivity.this,
                    new String[] {Manifest.permission.CALL_PHONE},CALL_REQUEST_CODE);
        }else {
            String dial="tel:"+restaurant.getPhoneNumber();
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }
    }

    public void getDirections(View view) {
        Intent intent=new Intent(RestaurantActivity.this,DirecctionActivity.class);

        intent.putExtra("startLat",startLat);
        intent.putExtra("startLng", startLng);
        intent.putExtra("startName",startName);
        intent.putExtra("start", start);
        intent.putExtra("desLat", desLat);
        intent.putExtra("desLng", desLng);
        intent.putExtra("name",name);
        intent.putExtra("map", map);
        intent.putExtra("map1", false);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==CALL_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                String dial="tel:"+restaurant.getPhoneNumber();
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            } else {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        offerViewHolderFirebaseRecyclerAdapter.startListening();
        menuViewHolderFirebaseRecyclerAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        offerViewHolderFirebaseRecyclerAdapter.startListening();
        menuViewHolderFirebaseRecyclerAdapter.stopListening();
    }
}
