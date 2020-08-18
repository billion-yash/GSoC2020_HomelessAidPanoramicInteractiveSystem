package mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.R;
import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy.adapters.LgUserAdapter;

import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy.lg_navigation.POI;
import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy.lg_navigation.POIController;
import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy.tasks.GetSessionTask;
import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy.tasks.VisitPoiTask;
import mihaela.claudia.diosan.gsoc2020_homelessaidpanoramicinteractivesystem.liquidGalaxy.utils.LgUser;


public class HomelessActivity extends AppCompatActivity {

    /*Firebase*/
    private FirebaseFirestore mFirestore;

    /*SearchView*/
    private SearchView searchView;

    SharedPreferences preferences;
    SharedPreferences defaultPrefs;
    TextView city_tv, country_tv, from_tv;
    ImageView goHome;
    private Map<String,String> homelessInfo = new HashMap<>();
  //  private Session session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lg_users_list);

        initViews();
        mFirestore = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        preferences = this.getSharedPreferences("cityInfo", MODE_PRIVATE);
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        GetSessionTask getSessionTask = new GetSessionTask(this);
        getSessionTask.execute();

        setActualLocation();
        setRecyclerView();

        goHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomelessActivity.this, MainActivityLG.class));
            }
        });
    }

    private void initViews(){
        searchView =findViewById(R.id.lg_users_search);
        searchView.onActionViewExpanded();
        searchView.clearFocus();
        city_tv = findViewById(R.id.city_text_users);
        country_tv = findViewById(R.id.country_text_users);
        goHome = findViewById(R.id.go_home_iv_users);
        from_tv = findViewById(R.id.city_text_tv);

    }

    private void setActualLocation(){

        preferences = this.getSharedPreferences("cityInfo", MODE_PRIVATE);
        String city = preferences.getString("city","");
        String country = preferences.getString("country","");

        city_tv.setText(city);
        country_tv.setText(country);
        from_tv.setText(getString(R.string.homeless_from));
    }

    private void setRecyclerView(){
        RecyclerView.LayoutManager mLayoutManager;
        final RecyclerView recyclerView = findViewById(R.id.recycler_view_users_lg);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            mLayoutManager = new GridLayoutManager(this, 2);
        }else {
            mLayoutManager = new GridLayoutManager(this, 4);
        }

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(mLayoutManager);

        final List<LgUser> users = new ArrayList<>();

        String city = preferences.getString("city","");


        mFirestore.collection("homeless").whereEqualTo("city", city)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                final String username = document.getString("homelessUsername");
                                final String latitude = document.getString("homelessLatitude");
                                final String longitude = document.getString("homelessLongitude");
                                final String birthday = document.getString("homelessBirthday");
                                final String location = document.getString("homelessAddress");
                                final String schedule = document.getString("homelessSchedule");
                                final String need = document.getString("homelessNeed");
                                final String lifeHistory = document.getString("homelessLifeHistory");
                                final String personallyDonations = document.getString("personallyDonations");
                                final String throughVolunteerDonations = document.getString("throughVolunteerDonations");
                                final String image = document.getString("image");
                                final double tilt = 70.0d;
                                final double range = 200.0d;

                                final int color = getColor(R.color.white);

                                final LgUser user = new LgUser(username,color, latitude, longitude,image, birthday, location, schedule, need, lifeHistory, personallyDonations, throughVolunteerDonations);
                                users.add(user);


                                final LgUserAdapter lgUserAdapter = new LgUserAdapter(users);
                                searchText(lgUserAdapter);
                                recyclerView.setAdapter(lgUserAdapter);

                                lgUserAdapter.setOnItemClickListener(new LgUserAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(int position) throws IOException {
                                        personallyTransactions(users.get(position).getUsername());
                                        throughVolunteerTransactions(users.get(position).getUsername());

                                        String description = description(users.get(position).getBirthday(), users.get(position).getLocation(), users.get(position).getSchedule(), users.get(position).getNeed());
                                        POIController.cleanKmls();
                                        POI userPoi = createPOI(users.get(position).getUsername(), users.get(position).getLatitude(), users.get(position).getLongitude());
                                        POIController.getInstance().moveToPOI(userPoi, null);

                                       // POIController.getInstance().sendPlacemark(userPoi, null, defaultPrefs.getString("SSH-IP", "192.168.1.76"), "balloons/homeless");
                                        POIController.downloadProfilePhoto(userPoi.getName(), users.get(position).getImage());
                                        POIController.getInstance().showPlacemark(userPoi,null, "https://i.ibb.co/1nsNbxr/homeless-icon.png", "placemarks/homeless");
                                        POIController.getInstance().showBalloon(userPoi, null, description, users.get(position).getUsername(), "balloons/basic/homeless");
                                        POIController.getInstance().sendBalloon(userPoi, null, "balloons/basic/homeless");
                                    }

                                    @Override
                                    public void onBioClick(int position) {
                                        personallyTransactions(users.get(position).getUsername());
                                        throughVolunteerTransactions(users.get(position).getUsername());

                                        POIController.cleanKmls();
                                        POI userPoi = createPOI(users.get(position).getUsername(), users.get(position).getLatitude(), users.get(position).getLongitude());
                                        POIController.getInstance().moveToPOI(userPoi, null);

                                      //  POIController.getInstance().sendPlacemark(userPoi, null, defaultPrefs.getString("SSH-IP", "192.168.1.76"), "balloons/homeless");

                                        POIController.getInstance().showPlacemark(userPoi,null, "https://i.ibb.co/1nsNbxr/homeless-icon.png", "placemarks/homeless");
                                        POIController.getInstance().showBalloon(userPoi, null, buildBio(users.get(position).getLifeHistory(), users.get(position).getBirthday(), users.get(position).getLocation(), users.get(position).getSchedule(), users.get(position).getNeed()), users.get(position).getUsername(), "balloons/bio/homeless");
                                        POIController.getInstance().sendBalloon(userPoi, null, "balloons/bio/homeless");

                                    }

                                    @Override
                                    public void onTransactionClick(int position) {
                                        personallyTransactions(users.get(position).getUsername());
                                        throughVolunteerTransactions(users.get(position).getUsername());


                                        POIController.cleanKmls();
                                        personallyTransactions(users.get(position).getUsername());
                                        throughVolunteerTransactions(users.get(position).getUsername());

                                        POI userPoi = createPOI(users.get(position).getUsername(), users.get(position).getLatitude(), users.get(position).getLongitude());
                                        POIController.getInstance().moveToPOI(userPoi, null);

                                      //  POIController.getInstance().sendPlacemark(userPoi, null, defaultPrefs.getString("SSH-IP", "192.168.1.76"), "balloons/homeless");

                                        POIController.getInstance().showPlacemark(userPoi,null, "https://i.ibb.co/1nsNbxr/homeless-icon.png", "placemarks/homeless");
                                        POIController.getInstance().showBalloon(userPoi, null, buildTransactions(users.get(position).getLifeHistory(),users.get(position).getBirthday(), users.get(position).getLocation(), users.get(position).getSchedule(), users.get(position).getNeed(), users.get(position).getPersonallyDonations(), users.get(position).getThroughVolunteerDonation()), users.get(position).getUsername(), "balloons/transactions/homeless");
                                        POIController.getInstance().sendBalloon(userPoi, null, "balloons/transactions/homeless");

                                    }

                                    @Override
                                    public void onOrbitClick(int position) {
                                        POI userPoi = createPOI(users.get(position).getUsername(), users.get(position).getLatitude(), users.get(position).getLongitude());
                                        String command = buildCommand(userPoi);
                                        VisitPoiTask visitPoiTask = new VisitPoiTask(command, userPoi, true,HomelessActivity.this, HomelessActivity.this);
                                        visitPoiTask.execute();
                                    }
                                });

                            }
                        }
                    }
                });
    }



    private POI createPOI(String name, String latitude, String longitude){

        POI poi = new POI()
                .setName(name)
                .setLongitude(Double.parseDouble(longitude))
                .setLatitude(Double.parseDouble(latitude))
                .setAltitude(0.0d)
                .setHeading(0.0d)
                .setTilt(70.0d)
                .setRange(200.0d)
                .setAltitudeMode("relativeToSeaFloor");

        return poi;
    }


    private void searchText(final LgUserAdapter lgUserAdapter){
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lgUserAdapter.getFilter().filter(newText);

                return false;
            }
        });
    }

    private String description(String birthday, String location, String schedule, String need){
        return  "<h2> <b> Basic Info</b></h2>\n" +
                "<p> <b> Birthday: </b> " + birthday + "</p>\n" +
                "<p> <b> Location: </b> " + location + "</p>\n" +
                "<p> <b> Schedule: </b> " + schedule + "</p>\n" +
                "<p> <b> Most important need: </b> " + need + "</p>\n" ;
    }

    private String buildBio(String lifeHistory, String birthday, String location, String schedule, String need){
        return  "<h2> <b> Basic Info</b></h2>\n" +
                "<p> <b> Birthday: </b> " + birthday + "</p>\n" +
                "<p> <b> Location: </b> " + location + "</p>\n" +
                "<p> <b> Schedule: </b> " + schedule + "</p>\n" +
                "<p> <b> Most important need: </b> " + need + "</p>\n" +
                "<h2><b> Life history </b> </h2>\n" +
                "<p> " + lifeHistory + "</p>\n";
    }

    public static String buildTransactions(String lifeHistory, String birthday, String location, String schedule, String need, String personallyDonations, String throughVolunteerDonations){

        return  "<h2> <b> Basic Info</b></h2>\n" +
                "<p> <b> Birthday: </b> " + birthday + "</p>\n" +
                "<p> <b> Location: </b> " + location + "</p>\n" +
                "<p> <b> Schedule: </b> " + schedule + "</p>\n" +
                "<p> <b> Most important need: </b> " + need + "</p>\n" +
                "<h2><b> Life history </b> </h2>\n" +
                "<p> " + lifeHistory + "</p>\n" +
                "<h2><b> Transactions </b> </h2>\n" +
                "<p><b> Personally Donations: </b> " +  personallyDonations + "</p>\n" +
                "<p><b> Through Volunteer Donations: </b> " +  throughVolunteerDonations + "</p>\n";
    }

  /*  private void personallyTransactions(String homelessUsername){

            mFirestore.collection("personallyDonations").whereEqualTo("donatesTo",homelessUsername )
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()){
                                String personallyDonations = String.valueOf(task.getResult().size());
                                homelessInfo.put("personallyDonations", personallyDonations);
                                mFirestore.collection("statistics").document(homelessUsername).set(homelessInfo, SetOptions.merge());

                            //    test_statistics.setText(String.valueOf(task.getResult().size()));
                                }
                        }
                    });
    }
*/

  /*  private void throughVolunteerTransactions(String homelessUsername){

        mFirestore.collection("throughVolunteerDonations").whereEqualTo("donatesTo",homelessUsername )
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            test_statistics.setText(String.valueOf(task.getResult().size()));
                        }
                    }
                });
    }*/

    private void personallyTransactions(String homelessUsername){

        mFirestore.collection("personallyDonations").whereEqualTo("donatesTo",homelessUsername )
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            String personallyDonations = String.valueOf(task.getResult().size());
                            homelessInfo.put("personallyDonations", personallyDonations);
                            mFirestore.collection("homeless").document(homelessUsername).set(homelessInfo, SetOptions.merge());

                        }
                    }
                });
    }


    private void throughVolunteerTransactions(String homelessUsername){

        mFirestore.collection("throughVolunteerDonations").whereEqualTo("donatesTo",homelessUsername )
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            String throughVolunteerDonations = String.valueOf(task.getResult().size());
                            homelessInfo.put("throughVolunteerDonations", throughVolunteerDonations);
                            mFirestore.collection("homeless").document(homelessUsername).set(homelessInfo, SetOptions.merge());

                        }
                    }
                });
    }




    private String buildCommand(POI poi) {
        return "echo 'flytoview=<gx:duration>3</gx:duration><gx:flyToMode>smooth</gx:flyToMode><LookAt><longitude>" + poi.getLongitude() + "</longitude>" +
                "<latitude>" + poi.getLatitude() + "</latitude>" +
                "<altitude>" + poi.getAltitude() + "</altitude>" +
                "<heading>" + poi.getHeading() + "</heading>" +
                "<tilt>" + poi.getTilt() + "</tilt>" +
                "<range>" + poi.getRange() + "</range>" +
                "<gx:altitudeMode>" + poi.getAltitudeMode() + "</gx:altitudeMode>" +
                "</LookAt>' > /tmp/query.txt";
    }

/*    private class GetSessionTask extends AsyncTask<Void, Void, Void> {
        Activity activity;

        GetSessionTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            session = LGUtils.getSession(activity);
            return null;
        }

        @Override
        protected void onPostExecute(Void success) {
            super.onPostExecute(success);
        }
    */}

/*    private class VisitPoiTask extends AsyncTask<Void, Void, String> {
        String command;
        POI currentPoi;
        boolean rotate;
        int rotationAngle = 10;
        int rotationFactor = 1;
        boolean changeVelocity = false;
        private ProgressDialog dialog;
        Activity activity;
        Context context;

        VisitPoiTask(String command, POI currentPoi, boolean rotate, Activity activity, Context context) {
            this.command = command;
            this.currentPoi = currentPoi;
            this.rotate = rotate;
            this.activity = activity;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (dialog == null) {
                dialog = new ProgressDialog(context);
                String message = context.getResources().getString(R.string.viewing) + " " + this.currentPoi.getName() + " " + context.getResources().getString(R.string.inLG);
                dialog.setMessage(message);
                dialog.setIndeterminate(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(false);


                //Buton positive => more speed
                //Button neutral => less speed
                if (this.rotate) {
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.speedx2), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing, we after define the onclick
                        }
                    });

                    dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.speeddiv2), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing, we after define the onclick
                        }
                    });
                }


                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        cancel(true);
                    }
                });
                dialog.setCanceledOnTouchOutside(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });


                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_fast_forward_black_36dp, 0, 0);
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_fast_rewind_black_36dp, 0, 0);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        changeVelocity = true;
                        rotationFactor = rotationFactor * 2;

                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(context.getResources().getString(R.string.speedx4));
                        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setText(context.getResources().getString(R.string.speeddiv2));
                        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
                        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_fast_rewind_black_36dp, 0, 0);

                        if (rotationFactor == 4) {
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                        }
                    }
                });
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        changeVelocity = true;
                        rotationFactor = rotationFactor / 2;

                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(context.getResources().getString(R.string.speedx2));
                        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setText(context.getResources().getString(R.string.speeddiv4));
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_fast_forward_black_36dp, 0, 0);

                        if (rotationFactor == 1) {
                            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
                        }
                    }
                });
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                session = LGUtils.getSession(activity);

                //We fly to the point
                LGUtils.setConnectionWithLiquidGalaxy(session, command, activity);

                //If rotation button is pressed, we start the rotation
                if (this.rotate) {

                    boolean isFirst = true;

                    while (!isCancelled()) {
                        session.sendKeepAliveMsg();

                        for (int i = 0; i <= (360 - this.currentPoi.getHeading()); i += (this.rotationAngle * this.rotationFactor)) {

                            String commandRotate = "echo 'flytoview=<gx:duration>3</gx:duration><gx:flyToMode>smooth</gx:flyToMode><LookAt>" +
                                    "<longitude>" + this.currentPoi.getLongitude() + "</longitude>" +
                                    "<latitude>" + this.currentPoi.getLatitude() + "</latitude>" +
                                    "<altitude>" + this.currentPoi.getAltitude() + "</altitude>" +
                                    "<heading>" + (this.currentPoi.getHeading() + i) + "</heading>" +
                                    "<tilt>" + this.currentPoi.getTilt() + "</tilt>" +
                                    "<range>" + this.currentPoi.getRange() + "</range>" +
                                    "<gx:altitudeMode>" + this.currentPoi.getAltitudeMode() + "</gx:altitudeMode>" +
                                    "</LookAt>' > /tmp/query.txt";


                            LGUtils.setConnectionWithLiquidGalaxy(session, commandRotate, activity);
                            session.sendKeepAliveMsg();

                            if (isFirst) {
                                isFirst = false;
                                Thread.sleep(7000);
                            } else {
                                Thread.sleep(4000);
                            }
                        }
                    }
                }

                return "";

            } catch (JSchException e) {
                this.cancel(true);
                if (dialog != null) {
                    dialog.dismiss();
                }
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(context, context.getResources().getString(R.string.error_galaxy), Toast.LENGTH_LONG).show();
                    }
                });

                return null;
            } catch (InterruptedException e) {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(context, context.getResources().getString(R.string.visualizationCanceled), Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String success) {
            super.onPostExecute(success);
            if (success != null) {
                if (dialog != null) {
                    dialog.hide();
                    dialog.dismiss();
                }
            }
        }
    }*/



