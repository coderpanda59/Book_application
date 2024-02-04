package masulkar.pandurang.bookapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;

import masulkar.pandurang.bookapplication.databinding.ActivityPdfAddBinding;

public class PdfAddActivity extends AppCompatActivity {

    //setup view binding
    private ActivityPdfAddBinding binding;


    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    //arryalist to hold pdf categories
    private ArrayList<ModelCategory> categoryArrayList;


    //url of picked pdf
    private Uri pdfUri=null;

    private static  final int PDF_PICK_CODE=1000;

    //TAG for Debugging
    private static final String TAG="ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadPdfCategories();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click to previous activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdfPickIntent();
            }
        });

        //handle click pick category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryPickDialog();
            }
        });

        //handle click upload pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate data
                validateData();

            }
        });
    }

    private String title="",description="", category="";
    private void validateData() {
        //step 1:Validate data
           Log.d(TAG,"validateData:validating data...");

        //get data
        title =binding.titleEt.getText().toString().trim();
        description= binding.descriptionEt.getText().toString().trim();
        category = binding.categoryTv.getText().toString().trim();

        //validate data
        if(TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter title...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Enter Description..", Toast.LENGTH_SHORT).show();

        }
        else if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Pick category..", Toast.LENGTH_SHORT).show();
        }
        else if (pdfUri==null) {
            Toast.makeText(this, "Pick Pdf...", Toast.LENGTH_SHORT).show();

        }
        else {
            //all data is valid can upload now
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        //step 2: Upload pdf to firebase Storage
        Log.d(TAG,"UploadPdfToStorage:uploading to storage...");

        //show progress
        progressDialog.setMessage("Uploading Pdf...");
        progressDialog.show();


        //timestamp
        long timestamp = System.currentTimeMillis();
        //path of pdf in firebase storage
        String filePathAndName="Books/"+timestamp;
        //Storage reference
        StorageReference storageReference= FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG,"OnSuccess:PDF Uploaded to storage...");
                        Log.d(TAG,"OnSuccess:getting pdf url");

                        Task<Uri> uriTask= taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());
                        String uploadedPdfUrl= ""+uriTask.getResult();

                        //upload to firebase db
                        uploadPdfInfoToDb(uploadedPdfUrl,timestamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG,"OnFailure:PDF Upload Failed due to"+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "PDF Upload Failed due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {


        //step 3: Upload pdf info to firebase Storage
        Log.d(TAG,"UploadPdfToStorage:uploading to storage...");

        progressDialog.setMessage("Uploading pdf info...");

        String uid= firebaseAuth.getUid();

        //setup data to upload
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("uid",""+uid);
        hashMap.put("id",""+timestamp);
        hashMap.put("title",""+title);
        hashMap.put("description",""+description);
        hashMap.put("category",""+category);
        hashMap.put("Url",""+uploadedPdfUrl);
        hashMap.put("id", timestamp);


        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Log.d(TAG,"onSuccess:Successfully uploaded...");
                        Toast.makeText(PdfAddActivity.this, "Successfully uploaded...", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                         Log.d(TAG,"onFailure:Failed to upload to db due to"+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loadPdfCategories() {

        Log.d(TAG,"loadPdfCategories:Loading pdf Categories...");
        categoryArrayList =new ArrayList<>();

        //db reference to load categories...db > Categories
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryArrayList.clear();//clear data befor adding data
                for(DataSnapshot ds:snapshot.getChildren()){
                    //get data
                    ModelCategory model = ds.getValue(ModelCategory.class);
                    //add to arraylist
                    categoryArrayList.add(model);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void categoryPickDialog() {
      Log.d(TAG,"categoryPickDialog:showing category pick dialog");

      //get string array of categories from arraylist
        String[] categoriesArray = new String[categoryArrayList.size()];
        for (int i=0;i<categoryArrayList.size();i++){
            categoriesArray[i] = categoryArrayList.get(i).getCategory();
        }



        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                          //handle item click
                        //get clicked item from list
                        String category = categoriesArray[which];
                        //set to category textview
                        binding .categoryTv.setText(category);

                        Log.d(TAG,"onClick:Selected Category:"+category);
                    }
                })
                .show();
    }

    private void pdfPickIntent() {

        Log.d(TAG,"pdfPickIntent:Starting pdf pick intent");

        Intent intent=new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf"),PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode==RESULT_OK){
            if(requestCode==PDF_PICK_CODE){
                Log.d(TAG,"onActivityResult:PDF Picked");

                pdfUri = data.getData();

                Log.d(TAG,"onActivityResult:URL:"+pdfUri);
            }
        }
        else {
            Log.d(TAG,"onActivityResult:Cancel Picked PDF...");
            Toast.makeText(this, "Cancel Picked PDF...", Toast.LENGTH_SHORT).show();

        }
    }
}//add firebase storage to our project