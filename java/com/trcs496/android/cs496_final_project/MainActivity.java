package com.trcs496.android.cs496_final_project;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public final static String USER_KEY = "USER_KEY";//keys that will get passed to other intents
    public final static String IMAGE_KEY = "IMAGE_KEY";
    public final static String PERSON_KEY = "PERSON_KEY";

    //vars for using camera
    static final int REQUEST_TAKE_PHOTO = 1;//values for knowing what to do in onActivityResult()
    static final int REQUEST_FROM_GALLERY = 2;
    private String mCurrentPhotoPath;
    private String albumName = "CS496_FinalProject";
    int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
    Uri photoURI;

    //vars for photoselect and upload
    private String uploadURL;
    private String uploadPhotoPath;
    private String photoDescription;
    private File uploadFile;
    private static final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");

    TextView messageImage;//get TextViews to display messages on in layout
    TextView messagePerson;
    private String key;//user key
    private String host;//website API calls made to
    private String scheme;//scheme of API call site
//    String asd;
//    private final OkHttpClient client = new OkHttpClient();
    LinearLayout linearImages;//layout to hold people and image buttons
    LinearLayout linearPeople;

    EditText photoDescriptionField;//edit text fields for filling with data when adding a person
    EditText firstNameField;
    EditText lastNameField;
    EditText dobField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scheme = getString(R.string.scheme);
        host = getString(R.string.host);
        setContentView(R.layout.activity_main);
        messageImage = (TextView) findViewById(R.id.messageImage);
        messagePerson = (TextView) findViewById(R.id.messagePerson);
        linearImages = (LinearLayout) findViewById(R.id.linearImages);
        linearPeople = (LinearLayout) findViewById(R.id.linearPeople);
        Intent intent = getIntent();
        key = intent.getStringExtra(SignIn.KEY);

        photoDescriptionField = (EditText) findViewById(R.id.photoDescription);
        firstNameField = (EditText) findViewById(R.id.firstName);
        lastNameField = (EditText) findViewById(R.id.lastName);
        dobField = (EditText) findViewById(R.id.dob);

//        getPhotoListing(key);
    }

    @Override
    protected void onResume() {//gets called along with oncreate, need to make sure when person or photo edited we update the lists--probably will not be data friendly, should look into updating within app
        super.onResume();
        linearImages.removeAllViewsInLayout();//clear layouts of old persons and photos, to repopulate with new data
        linearPeople.removeAllViewsInLayout();
        getPhotoListing(key);//repop layouts with new person and photo data
        getPersonListing(key);
    }


    //List for click from the list of image buttons
    //listen for click from image, start new intent passing it image and user keys
    View.OnClickListener imageClickListener(final PicassoButton button){
        return new View.OnClickListener(){
            public void onClick(View v){
                //message.setText(v.getTag().toString());
                Intent intent = new Intent(getApplicationContext(),ImageEdit.class);
                intent.putExtra(USER_KEY,key);
                intent.putExtra(IMAGE_KEY,v.getTag().toString());
                startActivity(intent);
            }
        };
    }
    //listen for click from person button, passing it image and user keys
    View.OnClickListener personClickListener(final PicassoButton button){
        return new View.OnClickListener(){
            public void onClick(View v){
                //message.setText(v.getTag().toString());
                Intent intent = new Intent(getApplicationContext(),PersonEdit.class);
                intent.putExtra(USER_KEY,key);
                intent.putExtra(PERSON_KEY,v.getTag().toString());
                startActivity(intent);
            }
        };
    }


    //Retrieve list of users photos from server, passes along users key, calls on make submission,
    //passing it photo and the users key

    private void getPhotoListing(String key){
        makeSubmissionGetData("photo", key);//pass photo path/action
    }

    private void getPersonListing(String key){
        makeSubmissionGetData("person", key);//pass person path/action
    }


    //Build URL for connecting to final project, accepts users key and desired action (aka applicable
     //URL path) to build URL

    private HttpUrl buildURLGetData(String action,String key){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments(action)
                .addPathSegment(key)
                .build();
        return url;
    }



    //Builds header, currently only ensures server knows we want json objects as reply

    private okhttp3.Headers getHeader(){
        okhttp3.Headers header = new okhttp3.Headers.Builder()
                .set("Accept", "application/json")
                .build();
        return header;
    }


    //makes submission to retrieve images and persons. Accepts action (aka applicable URL path) and users
    //key (for verifying with server which user this is)
    //originally planned to have it get people too, design/time constraints went a different way

    private void makeSubmissionGetData(final String action, final String key){
        final OkHttpClient client = new OkHttpClient();
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUrl url = buildURLGetData(action, key);//build url
                okhttp3.Headers header = getHeader();//get header
                okhttp3.Request request = new okhttp3.Request.Builder()//build request
                        .url(url)
                        .headers(header)
                        .get()//the getting data requests all use GET method
                        .build();
                try{//go for resposne
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//something wrong happened
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                messageImage.setText("There was an error in receiving response from server");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {//successfully contacted server
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                try{

                                    if (action.equals("photo")){
                                        JSONArray jarray = new JSONArray(response.body().string());
                                        if (jarray.length() != 0){//if we get list of images
                                            PicassoButton[] imageButtons = new PicassoButton[jarray.length()];//create array of picassobuttons to load images into
                                            for (int i = 0; i < jarray.length(); i++){//cycle through json array
                                                JSONObject object = jarray.getJSONObject(i);
                                                imageButtons[i] = new PicassoButton(getApplicationContext());
                                                imageButtons[i].setTag(object.get("key").toString());//we'll need the key in the listener
                                                imageButtons[i].setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                                Picasso.with(getApplicationContext()).load(object.get("image").toString()).into(imageButtons[i]);//load the image serving url into the button
                                                imageButtons[i].setOnClickListener(imageClickListener(imageButtons[i]));//set the buttons listener

                                                //add photo description
                                                TextView description = new TextView(getApplicationContext());
                                                description.setText(object.get("description").toString());
                                                linearImages.addView(description);

                                                //add photo date
                                                TextView date = new TextView(getApplicationContext());
                                                date.setText(object.get("uploadDate").toString());
                                                linearImages.addView(date);

                                                //add image
                                                linearImages.addView(imageButtons[i]);
                                            }
                                        }
                                        else{
                                            messageImage.setText("You currently have no photos");
                                        }
                                    }
                                    else if (action.equals("person")){
                                        JSONObject serverReply = new JSONObject(response.body().string());//put reply into JSON object
                                        JSONArray jarray = serverReply.getJSONArray("people");//retrieve array of people from reply
                                        if (jarray.length() != 0){//we got list of people
                                            for (int i = 0; i < jarray.length(); i++){
                                                JSONObject object = jarray.getJSONObject(i);//get individual person
                                                PicassoButton button = new PicassoButton(getBaseContext());//create new button
                                                button.setText(object.get("firstName") + " " + object.get("lastName"));//set text to be their name
                                                button.setTag(object.get("key").toString());//assign key to tag so that we know what to send to new activity
                                                button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                                button.setOnClickListener(personClickListener(button));//set appropriate listener
                                                linearPeople.addView(button);//add button to proper part of layout
                                            }
                                        }
                                    }
                                    else{
                                        messagePerson.setText("Something weird happened...");//should never get here but should be aware when it does
                                    }
                                }catch (Exception e){//error getting body
                                    messageImage.setText(e.toString());
                                }

                            }
                        });
                    }
                }
                catch(final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {//changing textView must be done on UI thread
                            messageImage.setText("There was an error: " + e.toString());
                        }
                    });
                }
            }
        }).start();
    }

    public void listenForUploadPictureButton(View view) {
        photoDescription = photoDescriptionField.getText().toString();
        if(photoDescription.equals("")){//user doesn't include name, give them directions to include it
            messageImage.setText("Please include a desciption");
        }
        else{//user does include name. upload photo
            messageImage.setText("Uploading photo...");
            getUploadURL();
        }

    }
    //retrieve upload url, make call on response to upload photo
    public void getUploadURL(){
        final OkHttpClient client = new OkHttpClient();
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUrl url = buildURLGetData("photo_get_upload_url", key);//get url with upload url action
                Headers headers = getHeader();

                Request request = new Request.Builder()
                        .url(url)
                        .headers(headers)
                        .get()//getting upload url is a GET action
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, final IOException e) {//print error if we get one
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                messageImage.setText(e.toString());
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        try{
                            JSONObject object = new JSONObject(response.body().string());
                            uploadURL = object.get("uploadurl").toString();//retrieve upload url from json object reply
                            makePost();//call make post function after upload url is received
                        }
                        catch (final Exception e){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {//changing textView must be done on UI thread
                                    messageImage.setText("getting url: " + e.toString());
                                }
                            });
                        }

                    }
                });
            }
        }).start();
    }

    public void makePost(){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//start photo picking from gallery
        startActivityForResult(intent, REQUEST_FROM_GALLERY);//select from gallery, onResult will make callback to actually make upload in onActivityResult function
    }
    //have either taken a picture or selected a picture from the gallery
    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == REQUEST_FROM_GALLERY){//made request from gallery
                Uri selectedImageUri = data.getData();
                uploadPhotoPath = getRealPathFromURI(getApplicationContext(),selectedImageUri);//call on function to retrieve full image path
                uploadFile = new File(uploadPhotoPath);//make new file of the proper image
//                try{
                final OkHttpClient client = new OkHttpClient();
                new Thread(new Runnable() {
                    public void run() {//post function done in other thread to not lock UI thread

                        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JPG, uploadFile);

                        MultipartBody multipartBody = new MultipartBody.Builder()//make multipart post
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("photoDescription", photoDescription)//photodescriptoin pulled from user input
                                .addFormDataPart("image", uploadFile.getName(), requestBody)//selected image
                                .addFormDataPart("userKey",key)
                                .build();

                        okhttp3.Request request = new okhttp3.Request.Builder()//make the request
                                .url(uploadURL)
                                .post(multipartBody)
                                .build();

                        try {//provide results from request
                            final okhttp3.Response response = client.newCall(request).execute();
                            if (!response.isSuccessful()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {//changing textView must be done on UI thread
                                        try{
                                            messageImage.setText(response.body().string());
                                        }
                                        catch(Exception e){
                                            Toast.makeText(getApplicationContext(), "An error occured converting response to string", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                            else{//if an error occurs give error message
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
//                                            message.setText(response.body().string());

                                            JSONObject object = new JSONObject(response.body().string());
                                            if (object.get("result").equals("success")){

                                                messageImage.setText("From server: " + object.get("result"));

                                                PicassoButton imageButton = new PicassoButton(getApplicationContext());//if we're successful, we want to add a new button to the list
                                                imageButton.setTag(object.get("key").toString());//we'll need the key in the listener
                                                Picasso.with(getApplicationContext()).load(object.get("image").toString()).into(imageButton);//load the image serving url into the button
                                                imageButton.setOnClickListener(imageClickListener(imageButton));//set the buttons listener

                                                //add photo description
                                                TextView description = new TextView(getApplicationContext());
                                                description.setText(object.get("description").toString());
                                                linearImages.addView(description);

                                                //add photo date
                                                TextView date = new TextView(getApplicationContext());
                                                date.setText(object.get("uploadDate").toString());
                                                linearImages.addView(date);

                                                //add image
                                                linearImages.addView(imageButton);

                                            }
                                            else if (object.get("result").equals("fail")){//if we fail, print why
                                                messageImage.setText("From server: " + object.get("cause").toString());
                                            }
                                            else{//if not fail and not success, need to know what happened
                                                messageImage.setText(object.toString());
                                            }
                                        } catch (Exception e) {//catch any errors
                                            messageImage.setText(e.toString());
                                        }
                                    }
                                });
                            }
                        }
                        catch (Exception e){
                            messageImage.setText(e.toString());
                        }
                    }
                }).start();//start post function in new thread
            }

            else if (requestCode == REQUEST_TAKE_PHOTO){//used as a catch to ensure we're not doing anything we shouldn't
                //do nothing here at this time
                //later on may rebuild so that user can directly upload image from camera, without selecting from gallery after taking
            } else{
                Toast.makeText(getApplicationContext(), "Invalid Request for onActivityResult", Toast.LENGTH_SHORT).show();//in case not camera taking picture or gallery having image selected
            }
        }
    }

    //code taken from online source to properly get path from photo URI
    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    //listen for call to take picture
    public void listenForTakePictureButton(View view) {
        dispatchTakePictureIntent();

    }

    //allow user to take a photo, a lot of this code is taken from simple
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//start the camera intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try{
                photoFile = createImageFile();//function to create image that camera stores photo in
            }
            catch (IOException e){
                //toast
                Toast.makeText(getApplicationContext(), "Failed to make photoFile", Toast.LENGTH_SHORT).show();//for error testing
            }
            if (photoFile != null){//launch camera intent, giving it file to put image in
                photoURI = FileProvider.getUriForFile(this,
                     "com.trcs496.android.cs496_final_project",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    //this function creates the file that the image is stored in
    private File createImageFile() throws IOException{

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//create file name using time stamp
        String imageFileName = "CS496HMWK4_" + timeStamp;
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),albumName);
        if (!storageDir.mkdirs()){};//if directory isn't there, make it
        File image = File.createTempFile(
                imageFileName,//image name
                ".jpg",//image extension
                storageDir//directory
        );
        mCurrentPhotoPath = "file:"+image.getAbsolutePath();
        return image;//return file to store image in
    }
    //listen for button call to add person
    public void listenForAddPerson(View view){

        if (firstNameField.getText().toString().equals("") || lastNameField.getText().toString().equals("") || dobField.getText().toString().equals("")){//tell user to give all proper data
            messagePerson.setText("Please include person first name, last name, and date ob birth (dob)");
        }
        else{//if all proper data given, upload person
            uploadPerson();
        }
    }
    //build url with proper action, scheme and host
    public HttpUrl buildURLPostData(String action){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment(action)
                .build();
        return url;
    }
    //upload person call, sends to API server via POST
    private void uploadPerson(){

        final OkHttpClient client = new OkHttpClient();
        new Thread(new Runnable() {
            @Override
            public void run() {//can't make netowrk calls on main/ui thread to prevent blocking
                final HttpUrl url = buildURLPostData("person");//make call with person route/path/action
                okhttp3.Headers header = getHeader();
                FormBody personUploadBody = buildPersonUploadBody();//form used to build person data
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .headers(header)
                        .post(personUploadBody)//POST person to server
                        .build();

                try{
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//something wrong happened
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                messagePerson.setText("There was an error in receiving response from server");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                try{
                                    JSONObject object = new JSONObject(response.body().string());//get result into JSON object
                                    messagePerson.setText("Server says: " + object.get("result").toString());//print result, fail or success
                                    if (object.get("result").equals("fail")){
                                        messagePerson.setText(messagePerson.getText() + "\n" + object.get("cause").toString());//if fail print cause
                                    }
                                    else if (object.get("result").equals("success")){//got success
                                        messagePerson.setText(messagePerson.getText() + "\n" + "You uploaded:" + "\nFirst Name: " + object.get("firstName").toString() + "\nLast Name: " + object.get("lastName")
                                                + "\nDOB: " + object.get("dob").toString());//print message for user on who they uploaded
                                        PicassoButton button = new PicassoButton(getBaseContext());//add person button to list
                                        button.setText(object.get("firstName") + " " + object.get("lastName"));//give button person name
                                        button.setTag(object.get("key").toString());//set tag to their key
                                        button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        button.setOnClickListener(personClickListener(button));//set proper listener
                                        linearPeople.addView(button);//add person button to list
                                    }
                                }
                                catch (Exception e){//print exception
                                    messagePerson.setText("error converting response body to string: " + e.toString());
                                }
                            }
                        });
                    }
                }
                catch(final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {//changing textView must be done on UI thread
                            messagePerson.setText("There was an error: " + e.toString());
                        }
                    });
                }
            }
        }).start();

    }
    //builds person data from what is in EditText fields, first and last names, dob, also sends user key which is req'd by server to make calls for authority reasons
    private FormBody buildPersonUploadBody(){
        FormBody body = new FormBody.Builder()
                .add("firstName",firstNameField.getText().toString())
                .add("lastName",lastNameField.getText().toString())
                .add("dob",dobField.getText().toString())
                .add("userKey",key)
                .build();
        return body;
    }

}
