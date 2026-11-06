package com.example.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button btnCamera;
    private ImageView imgFoto;
    private TextView txtStatus;

    private Uri fotoUri;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> permissaoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCamera = findViewById(R.id.btnCamera);
        imgFoto = findViewById(R.id.imgFoto);
        txtStatus = findViewById(R.id.txtStatus);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                sucesso -> {

                    if (sucesso && fotoUri != null) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues valores = new ContentValues();
                            valores.put(
                                    MediaStore.Images.Media.IS_PENDING,
                                    0
                            );

                            getContentResolver().update(
                                    fotoUri, valores, null, null
                            );
                        }

                        imgFoto.setImageURI(fotoUri);
                        txtStatus.setText("Foto salva com sucesso!");

                        Toast.makeText(
                                this,
                                "Imagem salva na galeria!",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        if (fotoUri != null) {
                            getContentResolver().delete(
                                    fotoUri, null, null
                            );
                            fotoUri = null;
                        }

                        txtStatus.setText("Captura cancelada.");
                    }
                }
        );

        permissaoLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedida -> {
                    if (concedida) {
                        prepararFoto();
                    } else {
                        txtStatus.setText(
                                "Permissão de armazenamento negada."
                        );
                    }
                }
        );

        btnCamera.setOnClickListener(v -> abrirCamera());
    }

    private void abrirCamera() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                permissaoLauncher.launch(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                );
                return;
            }
        }

        prepararFoto();
    }

    private void prepararFoto() {

        ContentValues valores = new ContentValues();

        String nomeArquivo =
                "Foto_" + System.currentTimeMillis() + ".jpg";

        valores.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                nomeArquivo
        );

        valores.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            valores.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/MinhaCamera"
            );

            valores.put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
            );

        } else {

            File pasta = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    ),
                    "MinhaCamera"
            );

            if (!pasta.exists() && !pasta.mkdirs()) {
                txtStatus.setText("Não foi possível criar a pasta.");
                return;
            }

            File arquivo = new File(pasta, nomeArquivo);

            valores.put(
                    MediaStore.Images.Media.DATA,
                    arquivo.getAbsolutePath()
            );
        }

        fotoUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                valores
        );

        if (fotoUri != null) {
            cameraLauncher.launch(fotoUri);
        } else {
            txtStatus.setText("Erro ao preparar a foto.");
        }
    }
}