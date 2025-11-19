package dngsoftware.acerfid;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setAction(getIntent().getAction());
        mainIntent.setData(getIntent().getData());
        mainIntent.putExtras(getIntent());
        startActivity(mainIntent);
        finish();
    }

}
