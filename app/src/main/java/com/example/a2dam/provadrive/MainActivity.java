package com.example.a2dam.provadrive;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.widget.DataBufferAdapter;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "drive-quickstart";
    private static final int REQUEST_CODE_SIGN_IN = 0;

    private GoogleSignInClient mGoogleSignInClient;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;

    private Button buscar, mostrar, crear;
    private EditText fichero;
    private ListView lista;
    private DataBufferAdapter<Metadata> listaAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGoogleSignInClient = buildGoogleSignInClient();

        Toast.makeText(this.getApplicationContext(),"Google Signin obtenido",Toast.LENGTH_SHORT).show();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);

        fichero = findViewById(R.id.fichero);
        buscar = findViewById(R.id.buscar);
        mostrar = findViewById(R.id.mostrar);
        crear = findViewById(R.id.crear);
        lista = findViewById(R.id.lista);

        listaAdapter = new ResultsAdapter(this);


        buscar.setOnClickListener(this);
        mostrar.setOnClickListener(this);
        crear.setOnClickListener(this);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));

                }
                break;
            default: break;
        }
    }

    public void crearfichero(String archivo){
        final String nombreFichero = archivo;
        final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
        final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        DriveFolder parent = rootFolderTask.getResult();
                        DriveContents contents = createContentsTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        try (Writer writer = new OutputStreamWriter(outputStream)) {
                            writer.write("Nuevo fichero");
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(nombreFichero)
                                .setMimeType("text/plain")
                                .setStarred(true)
                                .build();

                        return mDriveResourceClient.createFile(parent, changeSet, contents);
                    }
                })
                .addOnSuccessListener(this,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                Toast.makeText(MainActivity.this,"Fichero creado",Toast.LENGTH_SHORT).show();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to create file", e);
                        Toast.makeText(MainActivity.this,"Fichero no creado",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void buscarfichero(String fichero){
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, fichero))
                .build();
        final Task<MetadataBuffer> queryTask = mDriveResourceClient.query(query);

        queryTask
                .addOnSuccessListener(this,
                        new OnSuccessListener<MetadataBuffer>() {
                            @Override
                            public void onSuccess(MetadataBuffer metadataBuffer) {
                                if (queryTask.getResult().getCount()>0){
                                    String fichero = queryTask.getResult().get(0).getOriginalFilename();
                                    int i = queryTask.getResult().getCount();
                                    if (fichero != null && i != 0){
                                        Toast.makeText(MainActivity.this,i+" archivos "+fichero+ " encontrados", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this,"Fichero no encontrado",Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    public void listarFicheros(){
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain"))
                .build();
        // [START query_files]
        Task<MetadataBuffer> queryTask = mDriveResourceClient.query(query);
        // [END query_files]
        // [START query_results]
        queryTask
                .addOnSuccessListener(this,
                        new OnSuccessListener<MetadataBuffer>() {
                            @Override
                            public void onSuccess(MetadataBuffer metadataBuffer) {
                                // Handle results...
                                // [START_EXCLUDE]
                                listaAdapter.append(metadataBuffer);
                                // [END_EXCLUDE]
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure...
                        // [START_EXCLUDE]
                        Log.e(TAG, "Error retrieving files", e);
                        Toast.makeText(MainActivity.this,"Error obteniendo la lista",Toast.LENGTH_SHORT).show();
                        // [END_EXCLUDE]
                    }
                });


    }


    @Override
    public void onClick(View view) {
        String nombreFichero = fichero.getText().toString();
        switch (view.getId()){
            case R.id.buscar:
                if (nombreFichero.length()>0){
                    buscarfichero(nombreFichero);
                } else {
                    Toast.makeText(MainActivity.this,"Introduce un nombre valido",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.mostrar:
                Toast.makeText(this,"Mostrando ficheros",Toast.LENGTH_SHORT).show();
                listarFicheros();
                lista.setAdapter(listaAdapter);
                break;

            case R.id.crear:
                Toast.makeText(this,"Creando fichero",Toast.LENGTH_SHORT).show();
                if (nombreFichero.length()>0){
                    crearfichero(nombreFichero);
                } else {
                    Toast.makeText(MainActivity.this,"Introduce un nombre valido",Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;

        }
    }

    /**
     * Clears the result buffer to avoid memory leaks as soon
     * as the activity is no longer visible by the user.
     */
    @Override
    protected void onStop() {
        super.onStop();
        listaAdapter.clear();
    }

}

