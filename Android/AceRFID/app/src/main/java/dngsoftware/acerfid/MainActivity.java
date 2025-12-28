package dngsoftware.acerfid;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import dngsoftware.acerfid.databinding.ActivityMainBinding;
import dngsoftware.acerfid.databinding.AddDialogBinding;
import dngsoftware.acerfid.databinding.ManualDialogBinding;
import dngsoftware.acerfid.databinding.PickerDialogBinding;
import static dngsoftware.acerfid.Utils.GetBrand;
import static dngsoftware.acerfid.Utils.GetDefaultTemps;
import static dngsoftware.acerfid.Utils.GetMaterialLength;
import static dngsoftware.acerfid.Utils.GetMaterialWeight;
import static dngsoftware.acerfid.Utils.GetSku;
import static dngsoftware.acerfid.Utils.GetTemps;
import static dngsoftware.acerfid.Utils.SetPermissions;
import static dngsoftware.acerfid.Utils.arrayContains;
import static dngsoftware.acerfid.Utils.bytesToHex;
import static dngsoftware.acerfid.Utils.combineArrays;
import static dngsoftware.acerfid.Utils.copyFile;
import static dngsoftware.acerfid.Utils.copyFileToUri;
import static dngsoftware.acerfid.Utils.copyUriToFile;
import static dngsoftware.acerfid.Utils.filamentTypes;
import static dngsoftware.acerfid.Utils.filamentVendors;
import static dngsoftware.acerfid.Utils.getAllMaterials;
import static dngsoftware.acerfid.Utils.hexToByte;
import static dngsoftware.acerfid.Utils.materialWeights;
import static dngsoftware.acerfid.Utils.numToBytes;
import static dngsoftware.acerfid.Utils.GetSetting;
import static dngsoftware.acerfid.Utils.SaveSetting;
import static dngsoftware.acerfid.Utils.openUrl;
import static dngsoftware.acerfid.Utils.parseColor;
import static dngsoftware.acerfid.Utils.parseNumber;
import static dngsoftware.acerfid.Utils.playBeep;
import static dngsoftware.acerfid.Utils.populateDatabase;
import static dngsoftware.acerfid.Utils.presetColors;
import static dngsoftware.acerfid.Utils.setNfcLaunchMode;
import static dngsoftware.acerfid.Utils.setTypeByItem;
import static dngsoftware.acerfid.Utils.setVendorByItem;
import static dngsoftware.acerfid.Utils.subArray;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.navigation.NavigationView;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback, NavigationView.OnNavigationItemSelectedListener {
    private MatDB matDb;
    private filamentDB rdb;
    private NfcAdapter nfcAdapter;
    Tag currentTag = null;
    int tagType;
    ArrayAdapter<String> madapter, sadapter;
    String MaterialName, MaterialWeight = "1 KG", MaterialColor = "FF0000FF";
    Dialog pickerDialog, addDialog, customDialog;
    AlertDialog inputDialog;
    int SelectedSize;
    boolean userSelect = false;
    private ActivityMainBinding main;
    private ManualDialogBinding manual;
    Bitmap gradientBitmap;
    private ExecutorService executorService;
    private ActivityResultLauncher<Intent> exportDirectoryChooser;
    private ActivityResultLauncher<Intent> importFileChooser;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private static final int ACTION_EXPORT = 1;
    private static final int ACTION_IMPORT = 2;
    private int pendingAction = -1;
    NavigationView navigationView;
    private DrawerLayout drawerLayout;


    private static final int PERMISSION_REQUEST_CODE = 2;
    private PickerDialogBinding colorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getApplicationContext().getResources();
        Locale locale = new Locale("en");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());

        main = ActivityMainBinding.inflate(getLayoutInflater());
        View rv = main.getRoot();
        setContentView(rv);

        SetPermissions(this);

        executorService = Executors.newSingleThreadExecutor();
        setupActivityResultLaunchers();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem launchItem = navigationView.getMenu().findItem(R.id.nav_launch);
        SwitchCompat launchSwitch = Objects.requireNonNull(launchItem.getActionView()).findViewById(R.id.drawer_switch);
        launchSwitch.setChecked(GetSetting(this, "autoLaunch", true));
        launchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setNfcLaunchMode(this, isChecked);
            SaveSetting(this, "autoLaunch", isChecked);
        });

        MenuItem readItem = navigationView.getMenu().findItem(R.id.nav_read);
        SwitchCompat readSwitch = Objects.requireNonNull(readItem.getActionView()).findViewById(R.id.drawer_switch);
        readSwitch.setChecked(GetSetting(this, "autoread", false));
        readSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SaveSetting(this, "autoread", isChecked);
        });



        main.editbutton.setVisibility(View.INVISIBLE);
        main.deletebutton.setVisibility(View.INVISIBLE);

        main.colorview.setOnClickListener(view -> openPicker());
        main.colorview.setBackgroundColor(Color.argb(255, 0, 0, 255));
        main.readbutton.setOnClickListener(view -> readTag(currentTag));
        main.writebutton.setOnClickListener(view -> writeTag(currentTag));

        main.menubutton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
        main.addbutton.setOnClickListener(view -> openAddDialog(false));
        main.editbutton.setOnClickListener(view -> openAddDialog(true));

        main.deletebutton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            SpannableString titleText = new SpannableString(getString(R.string.delete_filament));
            titleText.setSpan(new ForegroundColorSpan(Color.parseColor("#1976D2")), 0, titleText.length(), 0);
            SpannableString messageText = new SpannableString(MaterialName);
            messageText.setSpan(new ForegroundColorSpan(Color.BLACK), 0, messageText.length(), 0);
            builder.setTitle(titleText);
            builder.setMessage(messageText);
            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                if (matDb.getFilamentByName(MaterialName) != null) {
                    matDb.deleteItem(matDb.getFilamentByName(MaterialName));
                    loadMaterials(false);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog alert = builder.create();
            alert.show();
            if (alert.getWindow() != null) {
                alert.getWindow().setBackgroundDrawableResource(android.R.color.white);
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"));
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#1976D2"));
            }
        });


        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                Bundle options = new Bundle();
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
                nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, options);
            } else {
                showToast(R.string.please_activate_nfc, Toast.LENGTH_LONG);
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                finish();
            }
        } catch (Exception ignored) {
        }

        setMatDb();

        main.colorspin.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    openPicker();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    break;
            }
            return false;
        });

        ReadTagUID(getIntent());
    }

    void setMatDb() {
        try {
        if (rdb != null && rdb.isOpen()) {
            rdb.close();
        }

        rdb = filamentDB.getInstance(this);
        matDb = rdb.matDB();

        if (matDb.getItemCount() == 0) {
            populateDatabase(matDb);
        }

        runOnUiThread(() -> {
            sadapter = new ArrayAdapter<>(this, R.layout.spinner_item, materialWeights);
            main.spoolsize.setAdapter(sadapter);
            main.spoolsize.setSelection(SelectedSize);
            main.spoolsize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    SelectedSize = main.spoolsize.getSelectedItemPosition();
                    MaterialWeight = sadapter.getItem(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            loadMaterials(false);
        });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            try {
                nfcAdapter.disableReaderMode(this);
            } catch (Exception ignored) {
            }
        }
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
            openPicker();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            runOnUiThread(() -> {
                byte[] uid = tag.getId();
                if (uid.length >= 6) {
                    currentTag = tag;
                    showToast(getString(R.string.tag_found) + bytesToHex(uid, false), Toast.LENGTH_SHORT);
                    tagType = getTagType(NfcA.get(currentTag));
                    main.tagid.setText(bytesToHex(uid, true));
                    if (tagType == 100) {
                        main.tagtype.setText(R.string.ultralight_c);
                    }
                    else {
                        main.tagtype.setText(String.format(Locale.getDefault(), "   NTAG%d", tagType));
                    }
                    main.lbltagid.setVisibility(View.VISIBLE);
                    main.lbltagtype.setVisibility(View.VISIBLE);
                    if (GetSetting(this, "autoread", false)) {
                        readTag(currentTag);
                    }
                }
                else {
                    currentTag = null;
                    main.tagid.setText("");
                    main.tagtype.setText("");
                    main.lbltagid.setVisibility(View.INVISIBLE);
                    main.lbltagtype.setVisibility(View.INVISIBLE);
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                }
            });
        } catch (Exception ignored) {
        }
    }

    void loadMaterials(boolean select)
    {
        madapter = new ArrayAdapter<>(this, R.layout.spinner_item, getAllMaterials(matDb));
        main.material.setAdapter(madapter);
        main.material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                MaterialName = madapter.getItem(position);
                assert MaterialName != null;
                main.infotext.setText(String.format(Locale.getDefault(), getString(R.string.info_temps),
                        GetTemps(matDb, MaterialName)[0], GetTemps(matDb, MaterialName)[1], GetTemps(matDb, MaterialName)[2], GetTemps(matDb, MaterialName)[3]));

                if (position <= 11){
                    main.editbutton.setVisibility(View.INVISIBLE);
                    main.deletebutton.setVisibility(View.INVISIBLE);
                }else {
                    main.editbutton.setVisibility(View.VISIBLE);
                    main.deletebutton.setVisibility(View.VISIBLE);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        if (select) {
            main.material.setSelection(madapter.getPosition(MaterialName));
        }
        else {
            main.material.setSelection(3);
        }
    }

    void ReadTagUID(Intent intent) {
        if (intent != null) {
            try {
                if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                    currentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    assert currentTag != null;
                    byte[] uid = currentTag.getId();
                    if (uid.length >= 6) {
                        tagType = getTagType(NfcA.get(currentTag));
                        showToast(getString(R.string.tag_found) + bytesToHex(uid, false), Toast.LENGTH_SHORT);
                        main.tagid.setText(bytesToHex(uid, true));
                        if (tagType == 100) {
                            main.tagtype.setText(R.string.ultralight_c);
                        }
                        else {
                            main.tagtype.setText(String.format(Locale.getDefault(), "   NTAG%d", tagType));
                        }
                        main.lbltagid.setVisibility(View.VISIBLE);
                        main.lbltagtype.setVisibility(View.VISIBLE);
                        if (GetSetting(this, "autoread", false)) {
                            readTag(currentTag);
                        }
                    }
                    else {
                        currentTag = null;
                        main.tagid.setText("");
                        main.tagtype.setText("");
                        main.lbltagid.setVisibility(View.INVISIBLE);
                        main.lbltagtype.setVisibility(View.INVISIBLE);
                        showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void readTag(Tag tag) {
        if (tag == null) {
            showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
            return;
        }
        NfcA nfcA = NfcA.get(tag);
        if (nfcA != null) {
            try {
                byte[] data = new byte[144];
                ByteBuffer buff = ByteBuffer.wrap(data);
                for (int page = 4; page <= 36; page += 4) {
                    byte[] pageData = transceive(nfcA, new byte[] {(byte) 0x30, (byte)page});
                    if (pageData != null) {
                        buff.put(pageData);
                    }
                }
                if (buff.array()[0] != (byte) 0x00) {
                    userSelect = true;
                    MaterialName = new String(subArray(buff.array(), 44, 16), StandardCharsets.UTF_8).trim();
                    main.material.setSelection(madapter.getPosition(MaterialName));
                    String color = parseColor(subArray(buff.array(), 65, 3));
                    String alpha = bytesToHex(subArray(buff.array(), 64, 1),false);
                    if (color.equals("010101")) {color = "000000";} // basic fix for anycubic setting black to transparent)
                    MaterialColor =  alpha + color;
                    main.colorview.setBackgroundColor(Color.parseColor("#" + MaterialColor));

                    // String sku = new String(subArray(buff.array(), 4, 16), StandardCharsets.UTF_8 ).trim();
                    // String Brand = new String(subArray(buff.array(), 24, 16), StandardCharsets.UTF_8).trim();
                    int extMin = parseNumber(subArray(buff.array(), 80, 2));
                    int extMax = parseNumber(subArray(buff.array(), 82, 2));
                    int bedMin = parseNumber(subArray(buff.array(), 100, 2));
                    int bedMax = parseNumber(subArray(buff.array(), 102, 2));
                    main.infotext.setText(String.format(Locale.getDefault(), getString(R.string.info_temps), extMin, extMax, bedMin, bedMax));
                    // int diameter = parseNumber(subArray(buff.array(),104,2));
                    MaterialWeight = GetMaterialWeight(parseNumber(subArray(buff.array(), 106, 2)));
                    main.spoolsize.setSelection(sadapter.getPosition(MaterialWeight));
                    showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
                    userSelect = false;
                } else {
                    showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                }
            } catch (Exception ignored) {
                showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
            } finally {
                try {
                    if (nfcA.isConnected()) nfcA.close();
                } catch (Exception ignored) {
                }
            }
        } else {
            showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
        }
    }

    private void writeTagPage(NfcA nfcA, int page, byte[] data) throws Exception {
        byte[] cmd = new byte[6];
        cmd[0] = (byte) 0xA2;
        cmd[1] = (byte) page;
        System.arraycopy(data, 0, cmd, 2, data.length);
        transceive(nfcA, cmd);
    }

    private boolean authenticateTag(NfcA nfcA, byte[] password) throws Exception {
        byte[] cmd = new byte[5];
        cmd[0] = (byte) 0x1B;
        System.arraycopy(password, 0, cmd, 1, 4);
        nfcA.setTimeout(500);
        byte[] response = transceive(nfcA, cmd);
        return response != null && response.length == 2;
    }

    public void setTagPassword(NfcA nfcA, byte[] newPassword, byte[] newPack) throws Exception {
        int pagePwd;
        int pagePack;
        int pageCfg;
        if (tagType == 213) {
             pagePwd = 43;
             pagePack = 44;
             pageCfg = 41;
        } else if (tagType == 215) {
            pagePwd = 133;
            pagePack = 134;
            pageCfg = 131;
        } else if (tagType == 216) {
            pagePwd = 229;
            pagePack = 230;
            pageCfg = 227;
        } else if (tagType == 100) {
            showToast(getString(R.string.ultralight_not_currently_supported), Toast.LENGTH_SHORT);
            return;
        } else {
            return;
        }
        transceive(nfcA, new byte[]{(byte)0xA2, (byte)pagePwd, newPassword[0], newPassword[1], newPassword[2], newPassword[3]});
        transceive(nfcA, new byte[]{(byte)0xA2, (byte)pagePack, newPack[0], newPack[1], (byte)0x00, (byte)0x00});
        byte[] cfg = transceive(nfcA, new byte[]{(byte)0x30, (byte)pageCfg});
        transceive(nfcA, new byte[]{(byte)0xA2, (byte)pageCfg, cfg[0], cfg[1], cfg[2], (byte)0x04});
    }

    public void removeTagPassword(NfcA nfcA) throws Exception {
        if (checkTagAuth(nfcA)) {
            int pagePwd;
            int pagePack;
            int accessPage;
            int pageCfg;
            if (tagType == 213) {
                pagePwd = 43;
                pagePack = 44;
                accessPage = 42;
                pageCfg = 41;
            } else if (tagType == 215) {
                pagePwd = 133;
                pagePack = 134;
                accessPage = 132;
                pageCfg = 131;
            } else if (tagType == 216) {
                pagePwd = 229;
                pagePack = 230;
                accessPage = 228;
                pageCfg = 227;
            } else if (tagType == 100) {
                transceive(nfcA, new byte[]{(byte) 0xA2, (byte) 42, (byte) 0xFF, 0x00, 0x00, 0x00});
                return;
            } else {
                return;
            }
            byte[] cfg = transceive(nfcA, new byte[]{(byte) 0x30, (byte) pageCfg});
            transceive(nfcA, new byte[]{(byte) 0xA2, (byte) pageCfg, cfg[0], cfg[1], cfg[2], (byte) 0xFF});
            transceive(nfcA, new byte[]{(byte) 0xA2, (byte) pagePwd, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
            transceive(nfcA, new byte[]{(byte) 0xA2, (byte) pagePack, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
            transceive(nfcA, new byte[]{(byte) 0xA2, (byte) accessPage, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        }
    }

    private void writeTag(Tag tag) {
        new Thread(() -> {
            if (tag == null) {
                showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                return;
            }
            NfcA nfcA = NfcA.get(tag);
            if (nfcA != null) {
                try {
                    checkTagAuth(nfcA);
                    writeTagPage(nfcA, 4, new byte[]{123, 0, 101, 0});
                    for (int i = 0; i < 5; i++) { //sku
                        writeTagPage(nfcA, 5 + i, subArray(GetSku(matDb, MaterialName), i * 4, 4));
                    }
                    for (int i = 0; i < 5; i++) { //brand
                        writeTagPage(nfcA, 10 + i, subArray(GetBrand(matDb, MaterialName), i * 4, 4));
                    }
                    byte[] matData = new byte[20];
                    Arrays.fill(matData, (byte) 0);
                    System.arraycopy(MaterialName.getBytes(), 0, matData, 0, Math.min(20, MaterialName.length()));
                    writeTagPage(nfcA, 15, subArray(matData, 0, 4));   //type
                    writeTagPage(nfcA, 16, subArray(matData, 4, 4));   //type
                    writeTagPage(nfcA, 17, subArray(matData, 8, 4));   //type
                    writeTagPage(nfcA, 18, subArray(matData, 12, 4));  //type
                    String color = MaterialColor.substring(2);
                    String alpha = MaterialColor.substring(0, 2);
                    if (color.equals("000000")) {
                        color = "010101";
                    }
                    writeTagPage(nfcA, 20, combineArrays(hexToByte(alpha), parseColor(color))); //color
                    byte[] extTemp = new byte[4];
                    System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[0]), 0, extTemp, 0, 2); //min
                    System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[1]), 0, extTemp, 2, 2); //max
                    writeTagPage(nfcA, 24, extTemp);
                    byte[] bedTemp = new byte[4];
                    System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[2]), 0, bedTemp, 0, 2); //min
                    System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[3]), 0, bedTemp, 2, 2); //max
                    writeTagPage(nfcA, 29, bedTemp);
                    byte[] filData = new byte[4];
                    System.arraycopy(numToBytes(175), 0, filData, 0, 2); //diameter
                    System.arraycopy(numToBytes(GetMaterialLength(MaterialWeight)), 0, filData, 2, 2); //length
                    writeTagPage(nfcA, 30, filData);
                    writeTagPage(nfcA, 31, new byte[]{(byte) 232, 3, 0, 0}); // weight in grams
                    playBeep();
                    showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
                } finally {
                    try {
                       if (nfcA.isConnected()) nfcA.close();
                    } catch (Exception ignored) {}
                }
            } else {
                showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
            }
        }).start();
    }

    private boolean checkTagAuth(NfcA nfcA) {
        try {
            int configStartPage;
            if (tagType == 213) {
                configStartPage = 41;
            } else if (tagType == 215) {
                configStartPage = 131;
            } else if (tagType == 216) {
                configStartPage = 227;
            } else if (tagType == 100) {
                configStartPage = 42;
            } else {
                return false;
            }
            byte[] configData = transceive(nfcA, new byte[] {(byte)0x30, (byte)configStartPage});
            if (configStartPage == 42)
            {
                if ((configData[0] & 0xFF) < 48) {
                    showToast(R.string.tag_is_password_protected_unable_to_write_to_this_tag, Toast.LENGTH_SHORT);
                    return true;
                }
                return false;
            }
            else {
                if ((configData[3] & 0xFF) < 255) {
                    showToast(R.string.tag_is_password_protected_trying_default_password, Toast.LENGTH_SHORT);
                    if (!authenticateTag(nfcA, new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF})) {
                        if (!authenticateTag(nfcA, new byte[]{0, 0, 0, 0})) {
                            showToast(R.string.password_failed_unable_to_write_to_this_tag, Toast.LENGTH_SHORT);
                        }
                    }
                    return true;
                }
                return false;
            }
        } catch (Exception ignored) {return false;}
    }

    private int getTagType(NfcA nfcA) {
        if (probePage(nfcA, (byte) 220)) return 216;
        if (probePage(nfcA, (byte) 125)) return 215;
        if (probePage(nfcA, (byte) 47)) return 100;
        return 213;
    }

    private boolean probePage(NfcA nfcA, byte pageNumber) {
        try (nfcA) {
            try {
                byte[] result = transceive(nfcA, new byte[]{(byte) 0x30, pageNumber});
                if (result != null && result.length == 16) {
                    return true;
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return false;
    }

    private byte[] transceive(NfcA nfcA, byte[] data) throws Exception {
        if (!nfcA.isConnected()) nfcA.connect();
        return nfcA.transceive(data);
    }

    private void formatTag(Tag tag) {
        new Thread(() -> {
            if (tag == null) {
                showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                return;
            }
            try (NfcA nfcA = NfcA.get(tag)) {
                try {
                    byte[] ccBytes;
                    if (tagType == 216) {
                        ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x6D, (byte) 0x00};
                    } else if (tagType == 215) {
                        ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x3E, (byte) 0x00};
                    } else if (tagType == 100) {
                        ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x06, (byte) 0x00};
                    } else {
                        ccBytes = new byte[]{(byte) 0xE1, (byte) 0x10, (byte) 0x12, (byte) 0x00};
                    }
                    showToast(R.string.formatting_tag, Toast.LENGTH_SHORT);
                    removeTagPassword(nfcA);
                    writeTagPage(nfcA, 2, new byte[]{0x00, 0x00, 0x00, 0x00});
                    writeTagPage(nfcA, 3, ccBytes);
                    for (int i = 4; i < 32; i++) {
                        writeTagPage(nfcA, i, new byte[]{0x00, 0x00, 0x00, 0x00});
                    }
                    showToast(R.string.tag_formatted, Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    showToast(R.string.failed_to_format_tag_for_writing, Toast.LENGTH_SHORT);
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    void openPicker() {
        try {
            pickerDialog = new Dialog(this, R.style.Theme_AceRFID);
            pickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            pickerDialog.setCanceledOnTouchOutside(false);
            pickerDialog.setTitle(R.string.pick_color);
            PickerDialogBinding dl = PickerDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            colorDialog = dl;
            pickerDialog.setContentView(rv);
            gradientBitmap = null;

            dl.btncls.setOnClickListener(v -> {
                MaterialColor = dl.txtcolor.getText().toString();
                if (customDialog != null && customDialog.isShowing()) {
                    manual.txtcolor.setText(MaterialColor);
                }else {
                    if (dl.txtcolor.getText().toString().length() == 8) {
                        try {
                            int color = Color.argb(dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                            main.colorview.setBackgroundColor(color);
                        } catch (Exception ignored) {
                        }
                    }
                }
                pickerDialog.dismiss();
            });

            dl.redSlider.setProgress(Color.red(Color.parseColor("#" + MaterialColor)));
            dl.greenSlider.setProgress(Color.green(Color.parseColor("#" + MaterialColor)));
            dl.blueSlider.setProgress(Color.blue(Color.parseColor("#" + MaterialColor)));
            dl.alphaSlider.setProgress(Color.alpha(Color.parseColor("#" + MaterialColor)));

            setupPresetColors(dl);
            updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());

            setupGradientPicker(dl);

            dl.gradientPickerView.setOnTouchListener((v, event) -> {
                v.performClick();
                if (gradientBitmap == null) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float touchX = event.getX();
                    float touchY = event.getY();
                    int pixelX = Math.max(0, Math.min(gradientBitmap.getWidth() - 1, (int) touchX));
                    int pixelY = Math.max(0, Math.min(gradientBitmap.getHeight() - 1, (int) touchY));
                    int pickedColor = gradientBitmap.getPixel(pixelX, pixelY);
                    setSlidersFromColor(dl, Color.argb(255, Color.red(pickedColor), Color.green(pickedColor), Color.blue(pickedColor)));
                    return true;
                }
                return false;
            });

            setupCollapsibleSection(dl,
                    dl.rgbSlidersHeader,
                    dl.rgbSlidersContent,
                    dl.rgbSlidersToggleIcon,
                    GetSetting(this,"RGB_VIEW",false)
            );
            setupCollapsibleSection(dl,
                    dl.gradientPickerHeader,
                    dl.gradientPickerContent,
                    dl.gradientPickerToggleIcon,
                    GetSetting(this,"PICKER_VIEW",true)
            );
            setupCollapsibleSection(dl,
                    dl.presetColorsHeader,
                    dl.presetColorsContent,
                    dl.presetColorsToggleIcon,
                    GetSetting(this,"PRESET_VIEW",true)
            );
            setupCollapsibleSection(dl,
                    dl.photoColorHeader,
                    dl.photoColorContent,
                    dl.photoColorToggleIcon,
                    GetSetting(this, "PHOTO_VIEW", false)
            );

            SeekBar.OnSeekBarChangeListener rgbChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

            dl.redSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.greenSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.blueSlider.setOnSeekBarChangeListener(rgbChangeListener);

            dl.alphaSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            dl.txtcolor.setOnClickListener(v -> showHexInputDialog(dl));

            dl.photoImage.setOnClickListener(v -> {
                Drawable drawable = ContextCompat.getDrawable(dl.photoImage.getContext(), R.drawable.camera);
                if (dl.photoImage.getDrawable() != null && drawable != null) {
                    if (Objects.equals(dl.photoImage.getDrawable().getConstantState(), drawable.getConstantState())) {
                        checkPermissionsAndCapture();
                    }
                } else {
                    checkPermissionsAndCapture();
                }
            });

            dl.clearImage.setOnClickListener(v -> {

                dl.photoImage.setImageResource( R.drawable.camera);
                dl.photoImage.setDrawingCacheEnabled(false);
                dl.photoImage.buildDrawingCache(false);
                dl.photoImage.setOnTouchListener(null);
                dl.clearImage.setVisibility(View.GONE);

            });

            pickerDialog.show();
        } catch (Exception ignored) {}
    }

    void openAddDialog(boolean edit) {
        try {
            if (!Utils.GetSetting(this,"CFN",false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                SpannableString titleText = new SpannableString(getString(R.string.notice));
                titleText.setSpan(new ForegroundColorSpan(Color.parseColor("#1976D2")), 0, titleText.length(), 0);
                SpannableString messageText = new SpannableString(getString(R.string.cf_notice));
                messageText.setSpan(new ForegroundColorSpan(Color.BLACK), 0, messageText.length(), 0);
                builder.setTitle(titleText);
                builder.setMessage(messageText);
                builder.setPositiveButton(R.string.accept, (dialog, which) -> {
                    Utils.SaveSetting(this, "CFN", true);
                    dialog.dismiss();
                    openAddDialog(edit);
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog alert = builder.create();
                alert.show();
                if (alert.getWindow() != null) {
                    alert.getWindow().setBackgroundDrawableResource(android.R.color.white);
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"));
                    alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#1976D2"));
                }
                return;
            }

            addDialog = new Dialog(this, R.style.Theme_AceRFID);
            addDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            addDialog.setCanceledOnTouchOutside(false);
            addDialog.setTitle(R.string.add_filament);
            AddDialogBinding dl = AddDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            addDialog.setContentView(rv);
            dl.btncls.setOnClickListener(v -> addDialog.dismiss());
            dl.btnhlp.setOnClickListener(v -> openUrl(this,getString(R.string.helpurl)));

            dl.chkvendor.setOnClickListener(v -> {
                if (dl.chkvendor.isChecked()) {
                    dl.vendor.setVisibility(View.INVISIBLE);
                    dl.txtvendor.setVisibility(View.VISIBLE);

                } else {
                    dl.vendor.setVisibility(View.VISIBLE);
                    dl.txtvendor.setVisibility(View.INVISIBLE);

                }
            });

            if (edit) {
                dl.btnsave.setText(R.string.save);
                dl.lbltitle.setText(R.string.edit_filament);
            }
            else {
                dl.btnsave.setText(R.string.add);
                dl.lbltitle.setText(R.string.add_filament);
            }

           dl.btnsave.setOnClickListener(v -> {
               if (dl.txtserial.getText().toString().isEmpty() || dl.txtextmin.getText().toString().isEmpty() || dl.txtextmax.getText().toString().isEmpty() || dl.txtbedmin.getText().toString().isEmpty() || dl.txtbedmax.getText().toString().isEmpty()) {
                   showToast(R.string.fill_all_fields, Toast.LENGTH_SHORT);
                   return;
               }
               if (dl.chkvendor.isChecked() && dl.txtvendor.getText().toString().isEmpty()) {
                   showToast(R.string.fill_all_fields, Toast.LENGTH_SHORT);
                   return;
               }

               String vendor = dl.vendor.getSelectedItem().toString();
               if (dl.chkvendor.isChecked())
               {
                   vendor = dl.txtvendor.getText().toString().trim();
               }
               if (edit) {
                   updateFilament(vendor, dl.type.getSelectedItem().toString(), dl.txtserial.getText().toString(), dl.txtextmin.getText().toString(), dl.txtextmax.getText().toString(), dl.txtbedmin.getText().toString(), dl.txtbedmax.getText().toString());
               } else {
                   addFilament(vendor, dl.type.getSelectedItem().toString(), dl.txtserial.getText().toString(), dl.txtextmin.getText().toString(), dl.txtextmax.getText().toString(), dl.txtbedmin.getText().toString(), dl.txtbedmax.getText().toString());
               }

               addDialog.dismiss();
           });

            ArrayAdapter<String> vadapter = new ArrayAdapter<>(this, R.layout.spinner_item, filamentVendors);
            dl.vendor.setAdapter(vadapter);

            ArrayAdapter<String> tadapter = new ArrayAdapter<>(this, R.layout.spinner_item, filamentTypes);
            dl.type.setAdapter(tadapter);

            dl.type.setOnTouchListener((v, event) -> {
                userSelect = true;
                v.performClick();
                return false;
            });

            dl.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (userSelect) {
                        String tmpType = Objects.requireNonNull(tadapter.getItem(position));
                        int[] temps = GetDefaultTemps(tmpType);
                        dl.txtextmin.setText(String.valueOf(temps[0]));
                        dl.txtextmax.setText(String.valueOf(temps[1]));
                        dl.txtbedmin.setText(String.valueOf(temps[2]));
                        dl.txtbedmax.setText(String.valueOf(temps[3]));
                        userSelect = false;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    userSelect = false;
                }
            });

            if (edit) {
                setTypeByItem(dl.type, tadapter, MaterialName);
                try {
                    if (!arrayContains(filamentVendors, MaterialName.split(dl.type.getSelectedItem().toString() + " ")[0].trim())) {
                        dl.chkvendor.setChecked(true);
                        dl.txtvendor.setVisibility(View.VISIBLE);
                        dl.vendor.setVisibility(View.INVISIBLE);
                        dl.txtvendor.setText(MaterialName.split(dl.type.getSelectedItem().toString() + " ")[0].trim());
                    } else {
                        dl.chkvendor.setChecked(false);
                        dl.txtvendor.setVisibility(View.INVISIBLE);
                        dl.vendor.setVisibility(View.VISIBLE);
                        setVendorByItem(dl.vendor, vadapter, MaterialName);
                    }
                } catch (Exception ignored) {
                    dl.chkvendor.setChecked(false);
                    dl.txtvendor.setVisibility(View.INVISIBLE);
                    dl.vendor.setVisibility(View.VISIBLE);
                    dl.vendor.setSelection(0);
                }
                try {
                    dl.txtserial.setText(MaterialName.split(dl.type.getSelectedItem().toString() + " ")[1]);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    dl.txtserial.setText("");
                }
                int[] temps = GetTemps(matDb, MaterialName);
                dl.txtextmin.setText(String.valueOf(temps[0]));
                dl.txtextmax.setText(String.valueOf(temps[1]));
                dl.txtbedmin.setText(String.valueOf(temps[2]));
                dl.txtbedmax.setText(String.valueOf(temps[3]));

            }else {
                dl.vendor.setSelection(0);
                dl.type.setSelection(7);
                int[] temps = GetDefaultTemps("PLA");
                dl.txtextmin.setText(String.valueOf(temps[0]));
                dl.txtextmax.setText(String.valueOf(temps[1]));
                dl.txtbedmin.setText(String.valueOf(temps[2]));
                dl.txtbedmax.setText(String.valueOf(temps[3]));
            }

            addDialog.show();
        } catch (Exception ignored) {}
    }

    void addFilament(String tmpVendor, String tmpType, String tmpSerial, String tmpExtMin, String tmpExtMax, String tmpBedMin, String tmpBedMax) {
        Filament filament = new Filament();
        filament.position =  matDb.getItemCount();
        filament.filamentID = "";
        filament.filamentName = String.format("%s %s %s", tmpVendor.trim(), tmpType, tmpSerial.trim());
        filament.filamentVendor = "";
        filament.filamentParam = String.format("%s|%s|%s|%s", tmpExtMin, tmpExtMax, tmpBedMin, tmpBedMax);
        matDb.addItem(filament);
        loadMaterials(false);
    }

    void updateFilament(String tmpVendor, String tmpType, String tmpSerial, String tmpExtMin, String tmpExtMax, String tmpBedMin, String tmpBedMax) {
        Filament currentFilament = matDb.getFilamentByName(MaterialName);
        int tmpPosition = currentFilament.position;
        matDb.deleteItem(currentFilament);
        MaterialName = String.format("%s %s %s", tmpVendor.trim() , tmpType, tmpSerial.trim());
        Filament filament = new Filament();
        filament.position =  tmpPosition;
        filament.filamentID = "";
        filament.filamentName = MaterialName;
        filament.filamentVendor = "";
        filament.filamentParam = String.format("%s|%s|%s|%s", tmpExtMin, tmpExtMax, tmpBedMin, tmpBedMax);
        matDb.addItem(filament);
        loadMaterials(true);
    }

    private void updateColorDisplay(PickerDialogBinding dl, int currentAlpha,int currentRed,int currentGreen,int currentBlue) {
        int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
        dl.colorDisplay.setBackgroundColor(color);
        String hexCode = rgbToHexA(currentRed, currentGreen, currentBlue, currentAlpha);
        dl.txtcolor.setText(hexCode);
        double alphaNormalized = currentAlpha / 255.0;
        int blendedRed = (int) (currentRed * alphaNormalized + 244 * (1 - alphaNormalized));
        int blendedGreen = (int) (currentGreen * alphaNormalized + 244 * (1 - alphaNormalized));
        int blendedBlue = (int) (currentBlue * alphaNormalized + 244 * (1 - alphaNormalized));
        double brightness = (0.299 * blendedRed + 0.587 * blendedGreen + 0.114 * blendedBlue) / 255;
        if (brightness > 0.5) {
            dl.txtcolor.setTextColor(Color.BLACK);
        } else {
            dl.txtcolor.setTextColor(Color.WHITE);
        }

    }

    private String rgbToHexA(int r, int g, int b, int a) {
        return String.format("%02X%02X%02X%02X", a, r, g, b);
    }

    private void setupPresetColors(PickerDialogBinding dl) {
        dl.presetColorGrid.removeAllViews();
        for (int color : presetColors()) {
            Button colorButton = new Button(this);
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.preset_circle_size),
                    (int) getResources().getDimension(R.dimen.preset_circle_size)
            );
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin)
            );
            colorButton.setLayoutParams(params);
            GradientDrawable circleDrawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.circle_shape, null);
            assert circleDrawable != null;
            circleDrawable.setColor(color);
            colorButton.setBackground(circleDrawable);
            colorButton.setTag(color);
            colorButton.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                setSlidersFromColor(dl, selectedColor);
            });
            dl.presetColorGrid.addView(colorButton);
        }
    }

    private void setSlidersFromColor(PickerDialogBinding dl, int argbColor) {
        dl.redSlider.setProgress(Color.red(argbColor));
        dl.greenSlider.setProgress(Color.green(argbColor));
        dl.blueSlider.setProgress(Color.blue(argbColor));
        dl.alphaSlider.setProgress(Color.alpha(argbColor));
        updateColorDisplay(dl, Color.alpha(argbColor), Color.red(argbColor), Color.green(argbColor), Color.blue(argbColor));
    }

    private void showHexInputDialog(PickerDialogBinding dl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.enter_hex_color_aarrggbb);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint(R.string.aarrggbb);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        input.setText(rgbToHexA(dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress(), dl.alphaSlider.getProgress()));
        InputFilter[] filters = new InputFilter[3];
        filters[0] = new HexInputFilter();
        filters[1] = new InputFilter.LengthFilter(8);
        filters[2] = new InputFilter.AllCaps();
        input.setFilters(filters);
        builder.setView(input);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.submit, (dialog, which) -> {
            String hexInput = input.getText().toString().trim();
            if (isValidHexCode(hexInput)) {
                setSlidersFromColor(dl, Color.parseColor("#" + hexInput));
            } else {
                showToast(R.string.invalid_hex_code_please_use_aarrggbb_format, Toast.LENGTH_LONG);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        inputDialog = builder.create();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidthPx = displayMetrics.widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int maxWidthDp = 100;
        int maxWidthPx = (int) (maxWidthDp * density);
        int dialogWidthPx = (int) (screenWidthPx * 0.80);
        if (dialogWidthPx > maxWidthPx) {
            dialogWidthPx = maxWidthPx;
        }
        Objects.requireNonNull(inputDialog.getWindow()).setLayout(dialogWidthPx, WindowManager.LayoutParams.WRAP_CONTENT);
        inputDialog.getWindow().setGravity(Gravity.CENTER); // Center the dialog on the screen
        inputDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = inputDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = inputDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#82B1FF"));
            negativeButton.setTextColor(Color.parseColor("#82B1FF"));
        });
        inputDialog.show();
    }

    private static class HexInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (Character.isDigit(character) || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F')) {
                    filtered.append(character);
                }
            }
            return filtered.toString();
        }
    }

    private boolean isValidHexCode(String hexCode) {
        Pattern pattern = Pattern.compile("^[0-9a-fA-F]{8}$");
        Matcher matcher = pattern.matcher(hexCode);
        return matcher.matches();
    }


    void setupGradientPicker(PickerDialogBinding dl) {
        dl.gradientPickerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dl.gradientPickerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = dl.gradientPickerView.getWidth();
                int height = dl.gradientPickerView.getHeight();
                if (width > 0 && height > 0) {
                    gradientBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(gradientBitmap);
                    Paint paint = new Paint();
                    float[] hsv = new float[3];
                    hsv[1] = 1.0f;
                    for (int y = 0; y < height; y++) {
                        hsv[2] = 1.0f - (float) y / height;
                        for (int x = 0; x < width; x++) {
                            hsv[0] = (float) x / width * 360f;
                            paint.setColor(Color.HSVToColor(255, hsv));
                            canvas.drawPoint(x, y, paint);
                        }
                    }
                    dl.gradientPickerView.setBackground(new BitmapDrawable(getResources(), gradientBitmap));
                }
            }
        });
    }

    private void setupCollapsibleSection(PickerDialogBinding dl, LinearLayout header, final ViewGroup content, final ImageView toggleIcon, boolean isExpandedInitially) {
        content.setVisibility(isExpandedInitially ? View.VISIBLE : View.GONE);
        toggleIcon.setImageResource(isExpandedInitially ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                toggleIcon.setImageResource(R.drawable.ic_arrow_down);
                if (header.getId() == dl.rgbSlidersHeader.getId()) {
                    SaveSetting(this,"RGB_VIEW",false);
                }
                else if (header.getId() == dl.gradientPickerHeader.getId()) {
                    SaveSetting(this,"PICKER_VIEW",false);
                }
                else if (header.getId() == dl.presetColorsHeader.getId()) {
                    SaveSetting(this,"PRESET_VIEW",false);
                }
                else if (header.getId() == dl.photoColorHeader.getId()) {
                    SaveSetting(this,"PHOTO_VIEW",false);
                }
            } else {
                content.setVisibility(View.VISIBLE);
                toggleIcon.setImageResource(R.drawable.ic_arrow_up);
                if (header.getId() == dl.rgbSlidersHeader.getId()) {
                    SaveSetting(this,"RGB_VIEW",true);
                }
                else if (header.getId() == dl.gradientPickerHeader.getId()) {
                    SaveSetting(this,"PICKER_VIEW",true);
                    if (gradientBitmap == null) {
                        setupGradientPicker(dl);
                    }
                }
                else if (header.getId() == dl.presetColorsHeader.getId()) {
                    SaveSetting(this,"PRESET_VIEW",true);
                }
                else if (header.getId() == dl.photoColorHeader.getId()) {
                    SaveSetting(this,"PHOTO_VIEW",true);
                }
            }
        });
    }

    private void setupActivityResultLaunchers() {
        exportDirectoryChooser = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null) {
                            getContentResolver().takePersistableUriPermission(
                                    treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );
                            performSAFExport(treeUri);
                        } else {
                            showToast(R.string.failed_to_get_export_directory, Toast.LENGTH_SHORT);
                        }
                    } else {
                        showToast(R.string.export_cancelled, Toast.LENGTH_SHORT);
                    }
                }
        );

        importFileChooser = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            performSAFImport(fileUri);
                        } else {
                            showToast(R.string.failed_to_select_import_file, Toast.LENGTH_SHORT);
                        }
                    } else {
                        showToast(R.string.import_cancelled, Toast.LENGTH_SHORT);
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (pendingAction == ACTION_EXPORT) {
                            performLegacyExport();
                        } else if (pendingAction == ACTION_IMPORT) {
                            performLegacyImport();
                        }
                    } else {
                        showToast(R.string.storage_permission_denied_cannot_perform_action, Toast.LENGTH_LONG);
                    }
                    pendingAction = -1;
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        colorDialog.photoImage.setImageBitmap(bitmap);
                        setupPhotoPicker(colorDialog.photoImage);
                    } else {
                        // Handle failure or cancellation
                        showToast(R.string.photo_capture_cancelled_or_failed, Toast.LENGTH_SHORT);
                    }
                }
        );
    }

    private void checkPermissionAndStartAction(int actionType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (actionType == ACTION_EXPORT) {
                    performLegacyExport();
                } else {
                    performLegacyImport();
                }
            } else {
                pendingAction = actionType;
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            if (actionType == ACTION_EXPORT) {
                startSAFExportProcess();
            } else {
                startSAFImportProcess();
            }
        }
    }

    private void startSAFExportProcess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.select_backup_folder));
        exportDirectoryChooser.launch(intent);
    }

    private void performSAFExport(Uri treeUri) {
        executorService.execute(() -> {
            try {
                File dbFile = filamentDB.getDatabaseFile(this);
                filamentDB.closeInstance();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
                    showToast(R.string.cannot_write_to_selected_directory, Toast.LENGTH_LONG);
                    return;
                }
                String dbBaseName = dbFile.getName().replace(".db", "");
                DocumentFile dbDestFile = pickedDir.createFile("application/octet-stream", dbBaseName + ".db");
                if (dbDestFile != null) {
                    copyFileToUri(this, dbFile, dbDestFile.getUri());
                } else {
                    showToast(R.string.failed_to_create_db_backup_file, Toast.LENGTH_LONG);
                    return;
                }
                showToast(R.string.database_exported_successfully, Toast.LENGTH_LONG);
            } catch (Exception e) {
                showToast(getString(R.string.database_saf_export_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                filamentDB.getInstance(this);
            }
        });
    }

    private void performLegacyExport() {
        executorService.execute(() -> {
            try {
                File dbFile = filamentDB.getDatabaseFile(this);
                filamentDB.closeInstance();
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                if (!downloadsDir.exists()) {
                    boolean val = downloadsDir.mkdirs();
                }
                String dbBaseName = dbFile.getName().replace(".db", "");
                File dbDestFile = new File(downloadsDir, dbBaseName + ".db");
                copyFile(dbFile, dbDestFile);
                showToast(R.string.database_exported_successfully_to_downloads_folder, Toast.LENGTH_LONG);
            } catch (Exception e) {
                showToast(getString(R.string.database_legacy_export_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                filamentDB.getInstance(this);
            }
        });
    }

    private void startSAFImportProcess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        String[] mimeTypes = {"application/x-sqlite3", "application/vnd.sqlite3", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importFileChooser.launch(intent);
    }

    private void performSAFImport(Uri sourceUri) {
        if (!sourceUri.toString().toLowerCase().contains("filament_database")) {
            showToast(R.string.incorrect_database_file_selected, Toast.LENGTH_LONG);
            return;
        }
        executorService.execute(() -> {
            try {
                filamentDB.closeInstance();
                File dbFile = filamentDB.getDatabaseFile(this);
                File dbDir = dbFile.getParentFile();
                if (dbDir != null && !dbDir.exists()) {
                    boolean val = dbDir.mkdirs();
                }
                copyUriToFile(this, sourceUri, dbFile);
                filamentDB.getInstance(this);
                setMatDb();

                showToast(R.string.database_imported_successfully, Toast.LENGTH_LONG);
            } catch (Exception e) {
                showToast(getString(R.string.database_saf_import_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                if (filamentDB.INSTANCE == null) {
                    filamentDB.getInstance(this);
                    setMatDb();
                }
            }
        });
    }

    private void performLegacyImport() {
        executorService.execute(() -> {
            try {
                filamentDB.closeInstance();

                File dbFile = filamentDB.getDatabaseFile(this);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File sourceDbFile = new File(downloadsDir, dbFile.getName());
                if (!dbFile.getName().toLowerCase().contains("filament_database")) {
                    showToast(R.string.incorrect_database_file_selected, Toast.LENGTH_LONG);
                    return;
                }
                if (!sourceDbFile.exists()) {
                    showToast(getString(R.string.backup_file_not_found_in_downloads) + sourceDbFile.getName(), Toast.LENGTH_LONG);
                    return;
                }
                File dbDir = dbFile.getParentFile();
                if (dbDir != null && !dbDir.exists()) {
                    boolean val = dbDir.mkdirs();
                }
                copyFile(sourceDbFile, dbFile);
                filamentDB.getInstance(this);
                setMatDb();

                showToast(R.string.database_imported_successfully, Toast.LENGTH_LONG);

            } catch (Exception e) {
                showToast(getString(R.string.database_legacy_import_failed) + e.getMessage(), Toast.LENGTH_LONG);
            } finally {
                if (filamentDB.INSTANCE == null) {
                    filamentDB.getInstance(this);
                    setMatDb();

                }
            }
        });
    }

    private void showImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString("Import Database");
        titleText.setSpan(new ForegroundColorSpan(Color.parseColor("#1976D2")), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString("Restore database from file\n\nfilament_database.db");
        messageText.setSpan(new ForegroundColorSpan(Color.BLACK), 0, messageText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.import_txt, (dialog, which) -> checkPermissionAndStartAction(ACTION_IMPORT));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#1976D2"));
        }
    }

    private void showExportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString titleText = new SpannableString("Export Database");
        titleText.setSpan(new ForegroundColorSpan(Color.parseColor("#1976D2")), 0, titleText.length(), 0);
        SpannableString messageText = new SpannableString("Backup database to file\n\nfilament_database.db");
        messageText.setSpan(new ForegroundColorSpan(Color.BLACK), 0, messageText.length(), 0);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.export, (dialog, which) -> new Thread(() -> {
            if (matDb.getItemCount() > 0) {
                runOnUiThread(() -> checkPermissionAndStartAction(ACTION_EXPORT));
            } else {
                runOnUiThread(() -> showToast(R.string.no_data_to_export, Toast.LENGTH_SHORT));
            }
        }).start());
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#1976D2"));
        }
    }

    private void checkPermissionsAndCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
        else {
            takePicture();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                showToast(R.string.camera_permission_is_required_to_take_photos, Toast.LENGTH_SHORT);
            }
        }
    }

    private void takePicture() {
        if (cameraLauncher != null) {
            cameraLauncher.launch(null);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPhotoPicker(ImageView imageView) {
        colorDialog.clearImage.setVisibility(View.VISIBLE);
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache(true);
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                Bitmap bitmap = imageView.getDrawingCache();
                float touchX = event.getX();
                float touchY = event.getY();
                if (touchX >= 0 && touchX < bitmap.getWidth() && touchY >= 0 && touchY < bitmap.getHeight()) {
                    try {
                        int pixel = bitmap.getPixel((int) touchX, (int) touchY);
                        int r = Color.red(pixel);
                        int g = Color.green(pixel);
                        int b = Color.blue(pixel);
                        colorDialog.colorDisplay.setBackgroundColor(Color.rgb(r, g, b));
                        colorDialog.txtcolor.setText(String.format("FF%06X", (0xFFFFFF & pixel)));
                        setSlidersFromColor(colorDialog, Color.argb(255, Color.red(pixel), Color.green(pixel), Color.blue(pixel)));
                    } catch (Exception ignored) {}
                }
            }
            return true;
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_export) {
            showExportDialog();
        }else if (id == R.id.nav_import) {
            showImportDialog();
        }else if (id == R.id.nav_manual) {
            openCustom();
        }else if (id == R.id.nav_format) {
            formatTag(currentTag);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint("SetTextI18n")
    void openCustom() {
        try {
            customDialog = new Dialog(this, R.style.Theme_AceRFID);
            customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            customDialog.setCanceledOnTouchOutside(false);
            customDialog.setTitle(R.string.custom_tag_data);
            manual = ManualDialogBinding.inflate(getLayoutInflater());
            View rv = manual.getRoot();
            customDialog.setContentView(rv);

            manual.txtbedmin.setText("50");
            manual.txtbedmax.setText("60");
            manual.txtextmax.setText("220");
            manual.txtextmin.setText("190");
            manual.txtlength.setText("330");
            manual.txtdiam.setText("1.75");
            manual.txtbrand.setText("");
            manual.txttype.setText("PLA");
            manual.txtsku.setText("");
            manual.txtcolor.setText("FF0000FF");

            manual.txttype.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txttype;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtcolor.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtcolor;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty));
                        editText.setTextColor(Color.RED);
                    } else if (input.length() < 8) {
                        editText.setError(getString(R.string.minimum_8_characters_required));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtlength.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtlength;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty));
                        editText.setTextColor(Color.RED);
                    } else if (input.length() < 2) {
                        editText.setError(getString(R.string.the_length_is_too_short));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtdiam.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtdiam;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty));
                        editText.setTextColor(Color.RED);
                    } else if (input.length() < 3) {
                        editText.setError(getString(R.string.this_value_might_be_invalid));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtextmin.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtextmin;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty));
                        editText.setTextColor(Color.RED);
                    } else if (input.length() < 3) {
                        editText.setError(getString(R.string.this_temperature_is_too_low));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtextmax.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtextmax;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty));
                        editText.setTextColor(Color.RED);
                    } else if (input.length() < 3) {
                        editText.setError(getString(R.string.this_temperature_is_too_low));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtbedmin.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtbedmin;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty_use_0));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.txtbedmax.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String input = s.toString();
                    EditText editText = manual.txtbedmax;
                    if (input.isEmpty()) {
                        editText.setError(getString(R.string.field_cannot_be_empty_use_0));
                        editText.setTextColor(Color.RED);
                    } else {
                        editText.setError(null);
                        editText.setTextColor(Color.BLACK);
                    }
                }
            });

            manual.btncls.setOnClickListener(v -> customDialog.dismiss());
            manual.btncol.setOnClickListener(view -> openPicker());

            manual.btnread.setOnClickListener(v -> {
                if (currentTag == null) {
                    showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                    return;
                }
                NfcA nfcA = NfcA.get(currentTag);
                if (nfcA != null) {
                    try {
                        byte[] data = new byte[144];
                        ByteBuffer buff = ByteBuffer.wrap(data);
                        for (int page = 4; page <= 36; page += 4) {
                            byte[] pageData = transceive(nfcA, new byte[] {(byte) 0x30, (byte)page});
                            if (pageData != null) {
                                buff.put(pageData);
                            }
                        }
                        if (buff.array()[0] != (byte) 0x00) {
                            manual.txttype.setText(new String(subArray(buff.array(), 44, 16), StandardCharsets.UTF_8).trim());
                            manual.txtsku.setText(new String(subArray(buff.array(), 4, 16), StandardCharsets.UTF_8 ).trim());
                            manual.txtbrand.setText(new String(subArray(buff.array(), 24, 16), StandardCharsets.UTF_8).trim());
                            String color = parseColor(subArray(buff.array(), 65, 3));
                            String alpha = bytesToHex(subArray(buff.array(), 64, 1),false);
                            if (color.equals("010101")) {color = "000000";}
                            manual.txtcolor.setText(alpha.toUpperCase() + color.toUpperCase());
                            manual.txtextmin.setText(String.valueOf(parseNumber(subArray(buff.array(), 80, 2))));
                            manual.txtextmax.setText(String.valueOf(parseNumber(subArray(buff.array(), 82, 2))));
                            manual.txtbedmin.setText(String.valueOf(parseNumber(subArray(buff.array(), 100, 2))));
                            manual.txtbedmax.setText(String.valueOf(parseNumber(subArray(buff.array(), 102, 2))));
                            manual.txtdiam.setText(String.format(Locale.getDefault(), "%.2f",parseNumber(subArray(buff.array(),104,2)) / 100.0));
                            manual.txtlength.setText(String.valueOf(parseNumber(Utils.subArray(buff.array(), 106, 2))));
                            showToast(R.string.data_read_from_tag, Toast.LENGTH_SHORT);
                        } else {
                            showToast(R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT);
                        }
                    } catch (Exception ignored) {
                        showToast(R.string.error_reading_tag, Toast.LENGTH_SHORT);
                    } finally {
                        try {
                            nfcA.close();
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                }
            });

            manual.btnwrite.setOnClickListener(v -> {
                if (manual.txtcolor.getText().length() == 8 && manual.txttype.getText().length() > 0
                        && manual.txtextmin.getText().length() > 0 && manual.txtextmax.getText().length() > 0
                        && manual.txtbedmin.getText().length() > 0 && manual.txtbedmax.getText().length() > 0
                        && manual.txtdiam.getText().length() > 0 && manual.txtlength.getText().length() > 0) {

                    if (currentTag == null) {
                        showToast(R.string.no_nfc_tag_found, Toast.LENGTH_SHORT);
                        return;
                    }
                    NfcA nfcA = NfcA.get(currentTag);
                    if (nfcA != null) {
                        try {
                            checkTagAuth(nfcA);
                            writeTagPage(nfcA, 4, new byte[]{123, 0, 101, 0});
                            byte[] skuData = new byte[20];
                            Arrays.fill(skuData, (byte) 0);
                            System.arraycopy(manual.txtsku.getText().toString().trim().getBytes(), 0, skuData, 0, Math.min(20, manual.txtsku.getText().toString().length()));
                            writeTagPage(nfcA, 5, subArray(skuData, 0, 4));
                            writeTagPage(nfcA, 6, subArray(skuData, 4, 4));
                            writeTagPage(nfcA, 7, subArray(skuData, 8, 4));
                            writeTagPage(nfcA, 8, subArray(skuData, 12, 4));
                            byte[] bndData = new byte[20];
                            Arrays.fill(bndData, (byte) 0);
                            System.arraycopy(manual.txtbrand.getText().toString().trim().getBytes(), 0, bndData, 0, Math.min(20, manual.txtbrand.getText().toString().length()));
                            writeTagPage(nfcA, 10, subArray(bndData, 0, 4));
                            writeTagPage(nfcA, 11, subArray(bndData, 4, 4));
                            writeTagPage(nfcA, 12, subArray(bndData, 8, 4));
                            writeTagPage(nfcA, 13, subArray(bndData, 12, 4));
                            byte[] matData = new byte[20];
                            Arrays.fill(matData, (byte) 0);
                            System.arraycopy(manual.txttype.getText().toString().trim().getBytes(), 0, matData, 0, Math.min(20, manual.txttype.getText().toString().length()));
                            writeTagPage(nfcA, 15, subArray(matData, 0, 4));
                            writeTagPage(nfcA, 16, subArray(matData, 4, 4));
                            writeTagPage(nfcA, 17, subArray(matData, 8, 4));
                            writeTagPage(nfcA, 18, subArray(matData, 12, 4));
                            String color = manual.txtcolor.getText().toString().trim().substring(2);
                            String alpha = manual.txtcolor.getText().toString().trim().substring(0, 2);
                            if (color.equals("000000")) {color = "010101";}
                            writeTagPage(nfcA, 20, combineArrays(hexToByte(alpha), parseColor(color)));
                            byte[] extTemp = new byte[4];
                            System.arraycopy(numToBytes(Integer.parseInt(manual.txtextmin.getText().toString().trim())), 0, extTemp, 0, 2);
                            System.arraycopy(numToBytes(Integer.parseInt(manual.txtextmax.getText().toString().trim())), 0, extTemp, 2, 2);
                            writeTagPage(nfcA, 24, extTemp);
                            byte[] bedTemp = new byte[4];
                            System.arraycopy(numToBytes(Integer.parseInt(manual.txtbedmin.getText().toString().trim())), 0, bedTemp, 0, 2);
                            System.arraycopy(numToBytes(Integer.parseInt(manual.txtbedmax.getText().toString().trim())), 0, bedTemp, 2, 2);
                            writeTagPage(nfcA, 29, bedTemp);
                            byte[] filData = new byte[4];
                            System.arraycopy(numToBytes(Integer.parseInt(manual.txtdiam.getText().toString().trim().replace(".", ""))), 0, filData, 0, 2);
                            System.arraycopy(numToBytes(Integer.parseInt(manual.txtlength.getText().toString().trim())), 0, filData, 2, 2);
                            writeTagPage(nfcA, 30, filData);
                            writeTagPage(nfcA, 31, new byte[]{(byte) 232, 3, 0, 0});
                            playBeep();
                            showToast(R.string.data_written_to_tag, Toast.LENGTH_SHORT);
                        } catch (Exception e) {
                            showToast(R.string.error_writing_to_tag, Toast.LENGTH_SHORT);
                        } finally {
                            try {
                                nfcA.close();
                            } catch (Exception ignored) {
                            }
                        }
                    } else {
                        showToast(R.string.invalid_tag_type, Toast.LENGTH_SHORT);
                    }
                }
                else {
                    showToast(R.string.invalid_input, Toast.LENGTH_SHORT);
                }

            });
            manual.btnfmt.setOnClickListener(v -> formatTag(currentTag));

            manual.btnrst.setOnClickListener(v -> {
                manual.txtbedmin.setText("50");
                manual.txtbedmax.setText("60");
                manual.txtextmax.setText("220");
                manual.txtextmin.setText("190");
                manual.txtlength.setText("330");
                manual.txtdiam.setText("1.75");
                manual.txtbrand.setText("");
                manual.txttype.setText("PLA");
                manual.txtsku.setText("");
                manual.txtcolor.setText("FF0000FF");
                showToast(R.string.values_reset, Toast.LENGTH_SHORT);
            });
            customDialog.show();
        } catch (Exception ignored) {
        }
    }


    private void showToast(final int message, final int duration) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), message, duration).show();
        });
    }

    private void showToast(final String message, final int duration) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), message, duration).show();
        });
    }
    









}