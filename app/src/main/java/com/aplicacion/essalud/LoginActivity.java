package com.aplicacion.essalud;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.aplicacion.essalud.models.database.LocalDB;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.Objects;

import static com.aplicacion.essalud.methods.Methods.encrypt;
import static com.aplicacion.essalud.methods.Methods.showSnackBar;

public class LoginActivity extends AppCompatActivity {

    // Variables de selección
    private final String DNI = "DNI";
    private final String CE = "Carnet de Extranjería";
    // Controles
    private MaterialSpinner msDocumentType;
    private TextInputLayout tilDocumentNumber;
    private TextInputEditText tietDocumentNumber;
    private TextInputLayout tilPassword;
    private TextInputEditText tietPassword;
    private CheckBox chbRemember;
    private MaterialButton mbtnLogin;
    // Preferencias
    private SharedPreferences PREFERENCES;
    private SharedPreferences.Editor EDITOR;

    // Firebase
    FirebaseDatabase fdEsSaludBD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Validar si existe autenticación
        PREFERENCES = getSharedPreferences(LocalDB.PREFS_NAME, MODE_PRIVATE);
        String document_number_ce = PREFERENCES.getString(encrypt(LocalDB.PREF_DOCUMENT_NUMBER_CE), null);
        String document_number_dni = PREFERENCES.getString(encrypt(LocalDB.PREF_DOCUMENT_NUMBER_DNI), null);
        String password = PREFERENCES.getString(encrypt(LocalDB.PREF_PASSWORD), null);
        if (document_number_ce != null && document_number_dni != null && password != null)
            InitChatBotActivity();
        // Declaración de controles
        msDocumentType = (MaterialSpinner) findViewById(R.id.msDocumentType);
        tilDocumentNumber = (TextInputLayout) findViewById(R.id.tilDocumentNumber);
        tietDocumentNumber = (TextInputEditText) findViewById(R.id.tietDocumentNumber);
        tilPassword = (TextInputLayout) findViewById(R.id.tilPassword);
        tietPassword = (TextInputEditText) findViewById(R.id.tietPassword);
        chbRemember = (CheckBox) findViewById(R.id.chbRemember);
        mbtnLogin = (MaterialButton) findViewById(R.id.mbtnLogin);
        // Modificación de ToolBar
        ((Toolbar) findViewById(R.id.myToolbar)).setTitle("Autenticación EsSalud");
        // Llenado de variables de selección a msDocumentType
        msDocumentType.setItems(DNI, CE);
        msDocumentType.setSelectedIndex(0);
        tilDocumentNumber.setHelperText(getResources().getString(R.string.document_type_0));
        tilDocumentNumber.setCounterMaxLength(8);
        // Crear instancia de base de datos
        fdEsSaludBD = FirebaseDatabase.getInstance();
        // Funcionalidad de selección de item en mbsDocumentType
        msDocumentType.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                switch (position) {
                    case 0: // DNI
                        tilDocumentNumber.setHelperText(DNI);
                        tilDocumentNumber.setCounterMaxLength(8);
                        break;
                    case 1: // CE
                        tilDocumentNumber.setHelperText(CE);
                        tilDocumentNumber.setCounterMaxLength(9);
                        break;
                }
            }
        });
        // Inicio de sesión
        mbtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener credenciales ingresadas
                String documentType = msDocumentType.getText().toString();
                final String documentNumber = Objects.requireNonNull(tietDocumentNumber.getText()).toString();
                final String password = Objects.requireNonNull(tietPassword.getText()).toString();
                final boolean remember = chbRemember.isChecked();
                // Validar credenciales
                clearControls();
                if (TextUtils.isEmpty(documentNumber)) {
                    tilDocumentNumber.setError("Ingresa tu número de " + msDocumentType.getText());
                    return;
                } else if (documentNumber.length() != tilDocumentNumber.getCounterMaxLength()) {
                    tilDocumentNumber.setError("Cantidad de dígitos incorrectos");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    tilPassword.setError("Ingresa tu contraseña");
                    return;
                }
                // Filtrar en la BD
                fdEsSaludBD.getReference("pacientes").addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dsPacientes) {
                        for (DataSnapshot sPaciente : dsPacientes.getChildren()) {
                            final String bdPacienteId = Objects.requireNonNull(sPaciente.getKey()).toString();
                            final int bdPersonaId = Integer.parseInt(Objects.requireNonNull(sPaciente.child("PERSONA_ID").getValue()).toString());
                            final String bdDocumentNumberDNI = Objects.requireNonNull(sPaciente.child("dni").getValue()).toString();
                            final String bdDocumentNumberCE = Objects.requireNonNull(sPaciente.child("ce").getValue()).toString();
                            final String bdPassword = Objects.requireNonNull(sPaciente.child("password").getValue()).toString();
                            if ((bdDocumentNumberCE.equals(documentNumber)
                                    || bdDocumentNumberDNI.equals(documentNumber))
                                    && bdPassword.equals(password)) {
                                fdEsSaludBD.getReference("personas").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dsPersonas) {
                                        String bdFirstName = "";
                                        String bdLastName = "";
                                        String bdEmail = "";
                                        String bdAddress = "";
                                        for (DataSnapshot dsPersona : dsPersonas.getChildren()) {
                                            int dsPersonaId = Integer.parseInt(Objects.requireNonNull(dsPersona.getKey()));
                                            if (dsPersonaId == bdPersonaId) {
                                                bdFirstName = Objects.requireNonNull(dsPersona.child("NOMBRE").getValue()).toString();
                                                bdLastName = Objects.requireNonNull(dsPersona.child("APELLIDO").getValue()).toString();
                                                bdEmail = Objects.requireNonNull(dsPersona.child("CORREO_GOOGLE").getValue()).toString();
                                                bdAddress = Objects.requireNonNull(dsPersona.child("DIRECCION").getValue()).toString();
                                                EDITOR = PREFERENCES.edit();
                                                EDITOR.putString(encrypt(LocalDB.PREF_PACIENTE_NAME), encrypt(bdFirstName))
                                                        .putString(encrypt(LocalDB.PREF_PACIENTE_LASTNAME), encrypt(bdLastName))
                                                        .putString(encrypt(LocalDB.PREF_PACIENTE_EMAIL), encrypt(bdEmail))
                                                        .putString(encrypt(LocalDB.PREF_PACIENTE_ADDRESS), encrypt(bdAddress));
                                                if (remember) {
                                                    EDITOR.putString(encrypt(LocalDB.PREF_DOCUMENT_NUMBER_DNI), encrypt(bdDocumentNumberDNI))
                                                            .putString(encrypt(LocalDB.PREF_DOCUMENT_NUMBER_CE), encrypt(bdDocumentNumberCE))
                                                            .putString(encrypt(LocalDB.PREF_PASSWORD), encrypt(bdPassword));
                                                }
                                                EDITOR.commit();
                                                InitChatBotActivity();
                                                return;
                                            }
                                        }
                                        showSnackBar(Snackbar.make(findViewById(android.R.id.content), "Credenciales incorrectas", Snackbar.LENGTH_LONG));
                                        tietDocumentNumber.getText().clear();
                                        tietPassword.getText().clear();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    private void InitChatBotActivity() {
        startActivity(new Intent(this, TestChatBotActivity.class));
    }

    // Limpiar errores de controles
    private void clearControls() {
        tilDocumentNumber.setError("");
        tilPassword.setError("");
    }
}
